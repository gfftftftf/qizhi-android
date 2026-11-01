package com.qizhi.rest.guard

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import com.qizhi.rest.QizhiApp
import com.qizhi.rest.core.BlockPolicy
import com.qizhi.rest.core.BlockReason
import com.qizhi.rest.core.TimeUtils
import com.qizhi.rest.data.GuardSession
import com.qizhi.rest.data.Phase
import com.qizhi.rest.data.RestReason
import com.qizhi.rest.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.abs

class GuardService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var app: QizhiApp
    private lateinit var container: com.qizhi.rest.AppContainer
    private lateinit var overlay: OverlayController

    private var session: GuardSession? = null
    private var tickerJob: Job? = null
    private var pollJob: Job? = null
    private var autoLockJob: Job? = null
    private var autoNextJob: Job? = null
    private var foregroundJob: Job? = null

    private val throttle = mutableMapOf<String, Long>()
    private val blockMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        app = application as QizhiApp
        container = app.container
        GuardNotifications.createChannels(this)
        overlay = OverlayController(
            context = this,
            onHome = ::onOverlayHome,
            onEmergency = ::onOverlayEmergency,
            onScreenOff = ::onOverlayScreenOff,
        )
        startForegroundCompat(GuardNotifications.starting(this))

        val restored = container.session.session
        session = restored
        if (restored != null && restored.endsAt <= System.currentTimeMillis()) {
            container.session.clear()
            session = null
            app.appScope.launch { runCatching { container.tweaks.restore() } }
        }

        foregroundJob = scope.launch {
            GuardBus.foreground.collect { pkg ->
                if (session != null) handleForeground(pkg)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            GuardController.ACTION_START_FOCUS -> {
                val minutes = intent.getIntExtra(
                    GuardController.EXTRA_MINUTES,
                    container.settings.current.focusMinutes,
                )
                startFocus(minutes)
            }

            GuardController.ACTION_START_REST -> {
                val minutes = intent.getIntExtra(
                    GuardController.EXTRA_MINUTES,
                    container.settings.current.restMinutes,
                )
                val reason = runCatching {
                    RestReason.valueOf(
                        intent.getStringExtra(GuardController.EXTRA_REASON)
                            ?: RestReason.MANUAL.name,
                    )
                }.getOrDefault(RestReason.MANUAL)
                startRest(minutes, reason)
            }

            GuardController.ACTION_START_SLEEP -> {
                val endsAt = intent.getLongExtra(GuardController.EXTRA_ENDS_AT, 0L)
                if (endsAt > System.currentTimeMillis()) startSleep(endsAt)
            }

            GuardController.ACTION_STOP -> {
                autoNextJob?.cancel()
                finishCurrent(completed = false, record = true)
                stopIfIdle()
            }

            GuardController.ACTION_STOP_FOCUS -> {
                if (session?.phase == Phase.FOCUS) {
                    finishCurrent(completed = false, record = true)
                }
                stopIfIdle()
            }

            GuardController.ACTION_WAKE -> wakeUp()
            GuardController.ACTION_PHASE_END -> handlePhaseEnd(intent)
            GuardController.ACTION_SCREEN_OFF -> lockScreenInternal()
            GuardController.ACTION_RESUME -> resumeCurrent()
            GuardController.ACTION_REFRESH -> updateNotification()
            GuardController.ACTION_CANCEL_EMERGENCY -> {
                if (session != null) showSessionOverlay() else stopIfIdle()
            }

            null -> resumeCurrent()
            else -> resumeCurrent()
        }
        return START_STICKY
    }

    private fun resumeCurrent() {
        val current = session ?: container.session.session
        if (current == null) {
            stopIfIdle()
            return
        }
        if (current.endsAt <= System.currentTimeMillis()) {
            session = current
            finishCurrent(completed = true, record = true)
            return
        }
        session = current
        startForegroundCompat(GuardNotifications.guardNotification(this, current))
        showSessionOverlay()
        container.scheduler.schedulePhaseEnd(current.phase, current.endsAt)
        startLoops()
        maybeAutoLock(current)
        app.appScope.launch {
            runCatching {
                container.tweaks.restore()
                container.tweaks.applyFor(current, container.settings.current)
            }
        }
    }

    private fun startFocus(minutes: Int) {
        val duration = minutes.coerceIn(5, 180)
        if (session != null) {
            finishCurrent(
                completed = false,
                record = true,
                stopService = false,
                restoreEnv = false,
                notifyCompletion = false,
            )
        }
        val now = System.currentTimeMillis()
        begin(
            GuardSession(
                phase = Phase.FOCUS,
                startedAt = now,
                endsAt = now + duration * 60_000L,
            ),
        )
    }

    private fun startRest(minutes: Int, reason: RestReason) {
        val duration = minutes.coerceIn(1, 60)
        if (session != null) {
            finishCurrent(
                completed = false,
                record = true,
                stopService = false,
                restoreEnv = false,
                notifyCompletion = false,
            )
        }
        val now = System.currentTimeMillis()
        begin(
            GuardSession(
                phase = Phase.REST,
                startedAt = now,
                endsAt = now + duration * 60_000L,
                restReason = reason,
            ),
        )
    }

    private fun startSleep(endsAt: Long) {
        if (session != null) {
            finishCurrent(
                completed = false,
                record = true,
                stopService = false,
                restoreEnv = false,
                notifyCompletion = false,
            )
        }
        val now = System.currentTimeMillis()
        begin(
            GuardSession(
                phase = Phase.SLEEP,
                startedAt = now,
                endsAt = endsAt,
            ),
        )
    }

    private fun begin(newSession: GuardSession) {
        session = newSession
        container.session.set(newSession)
        startForegroundCompat(GuardNotifications.guardNotification(this, newSession))
        showSessionOverlay()
        container.scheduler.schedulePhaseEnd(newSession.phase, newSession.endsAt)
        startLoops()
        maybeAutoLock(newSession)
        app.appScope.launch {
            runCatching {
                container.tweaks.restore()
                container.tweaks.applyFor(newSession, container.settings.current)
            }
        }
    }

    private fun startLoops() {
        tickerJob?.cancel()
        pollJob?.cancel()
        val current = session ?: return
        tickerJob = scope.launch {
            while (isActive) {
                val active = session ?: break
                if (System.currentTimeMillis() >= active.endsAt) {
                    finishCurrent(completed = true, record = true)
                    break
                }
                delay(1_000)
            }
        }
        pollJob = scope.launch {
            while (isActive) {
                val active = session ?: break
                if (active.phase == Phase.FOCUS && container.settings.current.enforcementEnabled) {
                    val pkg = container.usage.foregroundPackage()
                        ?: if (container.privileged.canExecute()) {
                            container.privileged.foregroundPackage()
                        } else {
                            null
                        }
                    if (pkg != null) handleForeground(pkg)
                }
                delay(2_500)
            }
        }
    }

    private suspend fun handleForeground(packageName: String) = blockMutex.withLock {
        val active = session ?: return@withLock
        if (!container.settings.current.enforcementEnabled) return@withLock
        if (packageName == this.packageName) return@withLock
        if (packageName in BlockPolicy.alwaysAllowed) return@withLock

        val settings = container.settings.current
        val usage = container.usage.usageToday()
        val usageMinutes = (usage[packageName] ?: 0L) / 60_000L
        val rule = container.rules.ruleFor(packageName)
        val allowedPackages = container.usage.launcherPackages() +
            container.usage.imePackages() +
            this.packageName
        val reason = BlockPolicy.reasonFor(
            packageName = packageName,
            ownPackage = this.packageName,
            phase = active.phase,
            rule = rule,
            usageMinutes = usageMinutes,
            dailyLimitsEnabled = settings.dailyLimitEnabled,
            sleepBlockAll = settings.sleepBlockAll,
            isSystemApp = container.usage.isSystemApp(packageName),
            allowedPackages = allowedPackages,
        )

        val overlayVisible = GuardBus.overlay.value
        if (reason == null) {
            if (overlayVisible.visible &&
                (overlayVisible.kind == OverlayKind.BLOCKED || overlayVisible.kind == OverlayKind.LIMIT)
            ) {
                val isRealForeground = container.usage.isLaunchable(packageName) ||
                    packageName in container.usage.launcherPackages()
                if (isRealForeground) overlay.hide()
            }
            return@withLock
        }

        val now = SystemClock.elapsedRealtime()
        val last = throttle[packageName] ?: 0L
        if (now - last < 3_500L) return@withLock
        throttle[packageName] = now

        val label = container.usage.labelFor(packageName)
        if (!overlayVisible.visible ||
            (overlayVisible.kind != OverlayKind.REST && overlayVisible.kind != OverlayKind.SLEEP)
        ) {
            overlay.show(
                OverlayState(
                    kind = if (reason == BlockReason.DAILY_LIMIT) OverlayKind.LIMIT else OverlayKind.BLOCKED,
                    phase = active.phase,
                    startedAt = active.startedAt,
                    endsAt = active.endsAt,
                    packageName = packageName,
                    packageLabel = label,
                    reason = reason,
                    allowEmergency = false,
                ),
            )
        }

        withContext(Dispatchers.Main) {
            val wentHome = QizhiAccessibilityService.goHome()
            if (!wentHome) {
                app.appScope.launch { runCatching { container.privileged.goHome() } }
            }
        }
        if (!android.provider.Settings.canDrawOverlays(this@GuardService)) {
            val message = when (reason) {
                BlockReason.FOCUS -> "此刻是专注时间，结束后它会回到你手里。"
                BlockReason.REST -> "此刻是休息时间，离开屏幕一会儿。"
                BlockReason.SLEEP -> "睡眠守护进行中，明早再见。"
                BlockReason.DAILY_LIMIT -> "今日使用额度已经用完。"
                BlockReason.SLEEP_ALL -> "睡眠模式正在守护你的夜晚。"
            }
            GuardNotifications.blocked(this@GuardService, label, message)
        }

        val forceStop = when (active.phase) {
            Phase.SLEEP -> settings.sleepForceStop
            else -> settings.forceStopBlocked
        }
        if (forceStop) {
            app.appScope.launch { runCatching { container.privileged.forceStop(packageName) } }
        }
    }

    private fun finishCurrent(
        completed: Boolean,
        record: Boolean,
        stopService: Boolean = true,
        restoreEnv: Boolean = true,
        notifyCompletion: Boolean = true,
    ) {
        val current = session ?: return
        session = null
        tickerJob?.cancel()
        pollJob?.cancel()
        autoLockJob?.cancel()
        container.scheduler.cancelPhaseEnd()
        overlay.hide()
        container.session.clear()

        if (record) {
            val endAt = if (completed) current.endsAt else minOf(System.currentTimeMillis(), current.endsAt)
            val elapsed = TimeUtils.safeMinutesBetween(current.startedAt, endAt)
            val minutes = if (completed) current.durationMinutes else elapsed
            when (current.phase) {
                Phase.FOCUS -> if (minutes > 0) {
                    container.history.addFocus(minutes, countSession = minutes >= 5)
                }

                Phase.REST -> if (completed || elapsed > 0) {
                    container.history.addRest(minutes.coerceAtLeast(0), completed)
                }

                Phase.SLEEP -> if (minutes > 0) container.history.addSleep(minutes)
                Phase.IDLE -> Unit
            }
            if (notifyCompletion) postCompletion(current, completed, minutes)
        }

        if (completed) {
            val settings = container.settings.current
            val next = when {
                settings.autoCycle && current.phase == Phase.FOCUS ->
                    GuardSession(
                        phase = Phase.REST,
                        startedAt = System.currentTimeMillis(),
                        endsAt = System.currentTimeMillis() + settings.restMinutes * 60_000L,
                        restReason = RestReason.CYCLE,
                    )

                settings.autoCycle &&
                    current.phase == Phase.REST &&
                    isInteractive() ->
                    GuardSession(
                        phase = Phase.FOCUS,
                        startedAt = System.currentTimeMillis(),
                        endsAt = System.currentTimeMillis() + settings.focusMinutes * 60_000L,
                    )

                else -> null
            }
            if (next != null) {
                startForegroundCompat(GuardNotifications.starting(this))
                autoNextJob?.cancel()
                autoNextJob = app.appScope.launch {
                    delay(800)
                    withContext(Dispatchers.Main) {
                        if (session == null) begin(next)
                    }
                }
                return
            }
        }

        if (restoreEnv) {
            app.appScope.launch { runCatching { container.tweaks.restore() } }
        }
        if (stopService) {
            stopGuardService()
        }
    }

    private fun postCompletion(current: GuardSession, completed: Boolean, minutes: Int) {
        when (current.phase) {
            Phase.FOCUS -> {
                if (completed) {
                    GuardNotifications.completion(
                        this,
                        "专注完成",
                        "你刚刚投入了 ${TimeUtils.formatCompactDuration(minutes)}。站起来，看看远处。",
                    )
                }
            }

            Phase.REST -> {
                if (completed) {
                    GuardNotifications.completion(
                        this,
                        "休息结束",
                        "带着一点清醒，回到今天。",
                    )
                } else {
                    GuardNotifications.completion(
                        this,
                        "休息被提前结束",
                        "没关系，下一次再慢一点。",
                    )
                }
            }

            Phase.SLEEP -> {
                if (completed) GuardNotifications.morning(this, minutes)
            }

            Phase.IDLE -> Unit
        }
    }

    private fun wakeUp() {
        if (session?.phase == Phase.SLEEP) {
            finishCurrent(completed = true, record = true)
        } else if (session == null) {
            GuardNotifications.morning(this, 0)
            stopIfIdle()
        }
    }

    private fun handlePhaseEnd(intent: Intent) {
        val current = session ?: return stopIfIdle()
        val phase = runCatching {
            Phase.valueOf(intent.getStringExtra(GuardController.EXTRA_PHASE) ?: "")
        }.getOrNull() ?: return
        val endsAt = intent.getLongExtra(GuardController.EXTRA_ENDS_AT, 0L)
        if (current.phase == phase && abs(current.endsAt - endsAt) < 8_000L) {
            finishCurrent(completed = true, record = true)
        } else if (System.currentTimeMillis() >= current.endsAt) {
            finishCurrent(completed = true, record = true)
        }
    }

    private fun maybeAutoLock(current: GuardSession) {
        autoLockJob?.cancel()
        if (current.phase != Phase.SLEEP) return
        if (!container.settings.current.sleepLockScreen) return
        autoLockJob = scope.launch {
            delay(30_000)
            if (session?.phase == Phase.SLEEP && isInteractive()) {
                lockScreenInternal()
            }
        }
    }

    private fun lockScreenInternal() {
        if (!isInteractive()) return
        autoLockJob?.cancel()
        val locked = QizhiAccessibilityService.lockScreen()
        if (!locked) {
            app.appScope.launch { runCatching { container.privileged.sleepScreen() } }
        }
    }

    private fun isInteractive(): Boolean {
        val pm = getSystemService(PowerManager::class.java) ?: return true
        return pm.isInteractive
    }

    private fun showSessionOverlay() {
        val current = session ?: return
        val settings = container.settings.current
        when (current.phase) {
            Phase.REST -> overlay.show(
                OverlayState(
                    kind = OverlayKind.REST,
                    phase = Phase.REST,
                    startedAt = current.startedAt,
                    endsAt = current.endsAt,
                    allowEmergency = settings.allowEmergencyExit,
                ),
            )

            Phase.SLEEP -> overlay.show(
                OverlayState(
                    kind = OverlayKind.SLEEP,
                    phase = Phase.SLEEP,
                    startedAt = current.startedAt,
                    endsAt = current.endsAt,
                    allowEmergency = settings.allowEmergencyExit,
                    lockOnSleep = settings.sleepLockScreen,
                ),
            )

            else -> overlay.hide()
        }
    }

    private fun onOverlayHome() {
        overlay.hide()
        val wentHome = QizhiAccessibilityService.goHome()
        if (!wentHome) {
            app.appScope.launch { runCatching { container.privileged.goHome() } }
        }
    }

    private fun onOverlayEmergency() {
        if (session == null) return
        if (!container.settings.current.allowEmergencyExit) return
        overlay.hide()
        startActivity(
            Intent(this, ExitConfirmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    private fun onOverlayScreenOff() {
        autoLockJob?.cancel()
        lockScreenInternal()
    }

    private fun updateNotification() {
        val current = session ?: return
        startForegroundCompat(GuardNotifications.guardNotification(this, current))
    }

    private fun startForegroundCompat(notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    GuardNotifications.ID_GUARD,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(GuardNotifications.ID_GUARD, notification)
            }
        }.onFailure {
            GuardNotifications.cancel(this, GuardNotifications.ID_GUARD)
        }
    }

    private fun stopGuardService() {
        autoNextJob?.cancel()
        GuardNotifications.cancel(this, GuardNotifications.ID_GUARD)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopIfIdle() {
        if (session == null) stopGuardService()
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        pollJob?.cancel()
        autoLockJob?.cancel()
        foregroundJob?.cancel()
        if (::overlay.isInitialized) overlay.hide()
        scope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 常驻守护不随最近任务清除而结束。
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
