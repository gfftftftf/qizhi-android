package com.qizhi.rest.guard

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.qizhi.rest.QizhiApp
import com.qizhi.rest.core.BlockPolicy
import com.qizhi.rest.core.BlockReason
import com.qizhi.rest.data.Phase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class QizhiAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lastCheck = mutableMapOf<String, Long>()
    private val lastNotice = mutableMapOf<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        GuardBus.reportForeground(pkg)

        val app = application as? QizhiApp ?: return
        val container = app.container
        val session = container.session.session

        // 有守护会话时由 GuardService 统一处理遮罩与强制停止，避免双份操作。
        if (session != null) return

        val settings = container.settings.current
        if (!settings.enforcementEnabled || !settings.dailyLimitEnabled || !settings.alwaysGuardLimits) {
            return
        }
        val rule = container.rules.ruleFor(pkg) ?: return
        if (rule.dailyLimitMinutes <= 0) return
        if (container.usage.isSystemApp(pkg)) return
        if (pkg in BlockPolicy.alwaysAllowed || pkg in container.usage.launcherPackages()) return

        val now = SystemClock.elapsedRealtime()
        val previous = lastCheck[pkg] ?: 0L
        if (now - previous < 2_000L) return
        lastCheck[pkg] = now

        scope.launch {
            val usage = container.usage.usageToday()[pkg]?.div(60_000L) ?: 0L
            val reason = BlockPolicy.reasonFor(
                packageName = pkg,
                ownPackage = packageName,
                phase = Phase.IDLE,
                rule = rule,
                usageMinutes = usage,
                dailyLimitsEnabled = true,
                sleepBlockAll = false,
                isSystemApp = container.usage.isSystemApp(pkg),
                allowedPackages = container.usage.launcherPackages(),
            )
            if (reason != BlockReason.DAILY_LIMIT) return@launch
            withContext(Dispatchers.Main) {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            if (settings.forceStopBlocked) {
                container.privileged.forceStop(pkg)
            }
            val noticeAt = lastNotice[pkg] ?: 0L
            if (SystemClock.elapsedRealtime() - noticeAt > 10 * 60_000L) {
                lastNotice[pkg] = SystemClock.elapsedRealtime()
                GuardNotifications.blocked(
                    this@QizhiAccessibilityService,
                    container.usage.labelFor(pkg),
                    "今日使用已达 ${rule.dailyLimitMinutes} 分钟限额，明天再见。",
                )
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance?.get() === this) instance = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: WeakReference<QizhiAccessibilityService>? = null

        fun lockScreen(): Boolean {
            val service = instance?.get() ?: return false
            if (Build.VERSION.SDK_INT < 28) return false
            return runCatching {
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }.getOrDefault(false)
        }

        fun goHome(): Boolean {
            val service = instance?.get() ?: return false
            return runCatching {
                service.performGlobalAction(GLOBAL_ACTION_HOME)
            }.getOrDefault(false)
        }

        fun isConnected(): Boolean = instance?.get() != null
    }
}
