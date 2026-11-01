package com.qizhi.rest.ui

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qizhi.rest.QizhiApp
import com.qizhi.rest.core.TimeUtils
import com.qizhi.rest.data.AppRule
import com.qizhi.rest.data.GuardSession
import com.qizhi.rest.data.InstalledAppInfo
import com.qizhi.rest.data.Phase
import com.qizhi.rest.data.Settings
import com.qizhi.rest.data.ThemeMode
import com.qizhi.rest.guard.GuardController
import com.qizhi.rest.privilege.GrantResult
import com.qizhi.rest.privilege.PermissionState
import com.qizhi.rest.privilege.PermissionTools
import com.qizhi.rest.privilege.PrivilegeStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AppUi(
    val info: InstalledAppInfo,
    val icon: ImageBitmap?,
    val rule: AppRule?,
    val usageMinutes: Long,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as QizhiApp
    private val container = app.container

    val settings: StateFlow<Settings> = container.settings.settings
    val rules: StateFlow<Map<String, AppRule>> = container.rules.rules
    val history = container.history.history
    val session: StateFlow<GuardSession?> = container.session.current
    val privilege: StateFlow<PrivilegeStatus> = container.privileged.status

    private val _permissions = MutableStateFlow(PermissionTools.snapshot(application))
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    private val _apps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val apps: StateFlow<List<InstalledAppInfo>> = _apps.asStateFlow()
    private val icons = mutableMapOf<String, ImageBitmap?>()
    private val _usage = MutableStateFlow<Map<String, Long>>(emptyMap())
    val usage: StateFlow<Map<String, Long>> = _usage.asStateFlow()
    private val _loadingApps = MutableStateFlow(true)
    val loadingApps: StateFlow<Boolean> = _loadingApps.asStateFlow()

    private val _granting = MutableStateFlow(false)
    val granting: StateFlow<Boolean> = _granting.asStateFlow()
    private val _grantResults = MutableStateFlow<List<GrantResult>?>(null)
    val grantResults: StateFlow<List<GrantResult>?> = _grantResults.asStateFlow()

    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _now.value = System.currentTimeMillis()
                delay(1_000)
            }
        }
        refreshApps()
        refreshUsage()
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                refreshUsage(force = false)
                refreshPermissions()
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingApps.value = true
            val loaded = runCatching { container.usage.loadLaunchableApps() }.getOrDefault(emptyList())
            loaded.forEach { info ->
                if (!icons.containsKey(info.packageName)) {
                    icons[info.packageName] = info.icon?.toImageBitmapSafe()
                }
            }
            _apps.value = loaded
            _loadingApps.value = false
        }
    }

    fun refreshUsage(force: Boolean = true) {
        viewModelScope.launch {
            _usage.value = runCatching {
                container.usage.usageToday(force)
            }.getOrDefault(emptyMap())
        }
    }

    fun refreshPermissions() {
        _permissions.value = PermissionTools.snapshot(getApplication())
        container.privileged.refresh()
    }

    fun refreshAll() {
        refreshPermissions()
        refreshUsage(force = true)
        if (_apps.value.isEmpty()) refreshApps()
    }

    fun appList(): List<AppUi> {
        val ruleMap = rules.value
        val usageMap = _usage.value
        return _apps.value.map { info ->
            AppUi(
                info = info,
                icon = icons[info.packageName],
                rule = ruleMap[info.packageName],
                usageMinutes = (usageMap[info.packageName] ?: 0L) / 60_000L,
            )
        }
    }

    fun iconFor(packageName: String): ImageBitmap? = icons[packageName]

    fun setRule(rule: AppRule) {
        container.rules.setRule(rule)
        if (session.value != null) {
            GuardController.refresh(getApplication())
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val next = container.settings.update(transform)
        container.scheduler.scheduleAll(next)
        refreshPermissions()
        if (session.value != null) GuardController.refresh(getApplication())
    }

    fun setTheme(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }

    fun completeOnboarding() {
        updateSettings { it.copy(onboardingDone = true, hasSeenSafety = true) }
    }

    fun startFocus(minutes: Int = settings.value.focusMinutes) {
        GuardController.startFocus(getApplication(), minutes)
    }

    fun startRest(minutes: Int = settings.value.restMinutes) {
        GuardController.startRest(getApplication(), minutes)
    }

    fun startSleepNow() {
        val s = settings.value
        val now = java.time.ZonedDateTime.now()
        val end = TimeUtils.sleepEndFor(now, s.wakeMinutes)
        GuardController.startSleep(getApplication(), end.toInstant().toEpochMilli())
    }

    fun stopSession() {
        GuardController.stop(getApplication())
    }

    fun requestShizukuPermission() {
        val requested = container.privileged.requestPermission()
        if (!requested) {
            _messages.tryEmit("请先打开 Shizuku 并确认服务正在运行。")
        }
    }

    fun grantAll() {
        if (_granting.value) return
        viewModelScope.launch {
            _granting.value = true
            val results = container.privileged.grantAll()
            _grantResults.value = results
            _granting.value = false
            container.settings.update { it.copy(lastGrantAt = System.currentTimeMillis()) }
            refreshPermissions()
        }
    }

    fun dismissGrantResults() {
        _grantResults.value = null
    }

    fun testRoot() {
        viewModelScope.launch {
            val ok = container.privileged.verifyRoot()
            _messages.tryEmit(if (ok) "Root 已连接，可作为 Shizuku 的备用通道。" else "未检测到可用的 Root。")
        }
    }

    fun restoreSystemTweaks() {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching { container.tweaks.restore() }.getOrDefault(false)
            _messages.tryEmit(if (ok) "已恢复勿扰与屏幕颜色设置。" else "没有可恢复的设置，或 Shizuku 当前不可用。")
        }
    }

    fun clearHistory() {
        container.history.clear()
        _messages.tryEmit("历史记录已清空。")
    }

    fun clearRules() {
        container.rules.clear()
        if (session.value != null) GuardController.refresh(getApplication())
        _messages.tryEmit("应用管控规则已清空。")
    }

    fun message(text: String) {
        _messages.tryEmit(text)
    }

    fun streak(): Int {
        val map = history.value.associateBy { it.date }
        var day = LocalDate.now()
        if (map[day.toString()]?.hasActivity != true) day = day.minusDays(1)
        var count = 0
        while (true) {
            val record = map[day.toString()] ?: break
            if (!record.hasActivity) break
            count++
            day = day.minusDays(1)
            if (count > 400) break
        }
        return count
    }

    fun todayFocusMinutes(): Int = history.value.firstOrNull {
        it.date == TimeUtils.todayKey()
    }?.focusMinutes ?: 0

    fun todayRestCount(): Int = history.value.firstOrNull {
        it.date == TimeUtils.todayKey()
    }?.restSessions ?: 0

    fun lastSleepMinutes(): Int = history.value.firstOrNull {
        it.date == TimeUtils.todayKey()
    }?.sleepMinutes ?: 0

    fun isOverlayReady(): Boolean = PermissionTools.hasOverlay(getApplication())

    fun isSessionActive(): Boolean = session.value?.let {
        it.phase != Phase.IDLE && it.endsAt > System.currentTimeMillis()
    } == true

    private fun Drawable.toImageBitmapSafe(): ImageBitmap? = runCatching {
        val bitmap = toBitmap(width = 112, height = 112)
        bitmap.asImageBitmap()
    }.getOrNull()
}
