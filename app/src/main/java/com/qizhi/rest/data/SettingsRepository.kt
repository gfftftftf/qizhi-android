package com.qizhi.rest.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("qizhi_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    val current: Settings get() = _settings.value

    @Synchronized
    fun update(transform: (Settings) -> Settings): Settings {
        val next = sanitize(transform(_settings.value))
        write(next)
        _settings.value = next
        return next
    }

    private fun read(): Settings = Settings(
        themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM),
        sleepGuardEnabled = prefs.getBoolean(KEY_SLEEP_GUARD, true),
        bedtimeMinutes = prefs.getInt(KEY_BEDTIME, 23 * 60 + 30),
        wakeMinutes = prefs.getInt(KEY_WAKE, 7 * 60),
        windDownMinutes = prefs.getInt(KEY_WIND_DOWN, 30),
        sleepBlockAll = prefs.getBoolean(KEY_SLEEP_BLOCK_ALL, false),
        sleepLockScreen = prefs.getBoolean(KEY_SLEEP_LOCK, true),
        sleepDnd = prefs.getBoolean(KEY_SLEEP_DND, true),
        sleepGrayscale = prefs.getBoolean(KEY_SLEEP_GRAY, true),
        sleepForceStop = prefs.getBoolean(KEY_SLEEP_FORCE_STOP, true),
        focusMinutes = prefs.getInt(KEY_FOCUS, 45),
        restMinutes = prefs.getInt(KEY_REST, 10),
        autoCycle = prefs.getBoolean(KEY_AUTO_CYCLE, false),
        restStrict = prefs.getBoolean(KEY_REST_STRICT, true),
        restGrayscale = prefs.getBoolean(KEY_REST_GRAY, false),
        restDnd = prefs.getBoolean(KEY_REST_DND, false),
        allowEmergencyExit = prefs.getBoolean(KEY_EMERGENCY_EXIT, true),
        enforcementEnabled = prefs.getBoolean(KEY_ENFORCEMENT, true),
        forceStopBlocked = prefs.getBoolean(KEY_FORCE_STOP, true),
        dailyLimitEnabled = prefs.getBoolean(KEY_DAILY_LIMIT, true),
        alwaysGuardLimits = prefs.getBoolean(KEY_ALWAYS_GUARD, true),
        onboardingDone = prefs.getBoolean(KEY_ONBOARDED, false),
        hasSeenSafety = prefs.getBoolean(KEY_SAFETY, false),
        lastGrantAt = prefs.getLong(KEY_LAST_GRANT, 0L),
    ).let(::sanitize)

    private fun sanitize(s: Settings): Settings {
        var wake = s.wakeMinutes.coerceIn(0, 1439)
        if (wake == s.bedtimeMinutes) wake = (wake + 30) % 1440
        return s.copy(
            bedtimeMinutes = s.bedtimeMinutes.coerceIn(0, 1439),
            wakeMinutes = wake,
            windDownMinutes = s.windDownMinutes.coerceIn(0, 120),
            focusMinutes = s.focusMinutes.coerceIn(5, 180),
            restMinutes = s.restMinutes.coerceIn(1, 60),
        )
    }

    private fun write(s: Settings) {
        prefs.edit()
            .putString(KEY_THEME, s.themeMode.name)
            .putBoolean(KEY_SLEEP_GUARD, s.sleepGuardEnabled)
            .putInt(KEY_BEDTIME, s.bedtimeMinutes)
            .putInt(KEY_WAKE, s.wakeMinutes)
            .putInt(KEY_WIND_DOWN, s.windDownMinutes)
            .putBoolean(KEY_SLEEP_BLOCK_ALL, s.sleepBlockAll)
            .putBoolean(KEY_SLEEP_LOCK, s.sleepLockScreen)
            .putBoolean(KEY_SLEEP_DND, s.sleepDnd)
            .putBoolean(KEY_SLEEP_GRAY, s.sleepGrayscale)
            .putBoolean(KEY_SLEEP_FORCE_STOP, s.sleepForceStop)
            .putInt(KEY_FOCUS, s.focusMinutes)
            .putInt(KEY_REST, s.restMinutes)
            .putBoolean(KEY_AUTO_CYCLE, s.autoCycle)
            .putBoolean(KEY_REST_STRICT, s.restStrict)
            .putBoolean(KEY_REST_GRAY, s.restGrayscale)
            .putBoolean(KEY_REST_DND, s.restDnd)
            .putBoolean(KEY_EMERGENCY_EXIT, s.allowEmergencyExit)
            .putBoolean(KEY_ENFORCEMENT, s.enforcementEnabled)
            .putBoolean(KEY_FORCE_STOP, s.forceStopBlocked)
            .putBoolean(KEY_DAILY_LIMIT, s.dailyLimitEnabled)
            .putBoolean(KEY_ALWAYS_GUARD, s.alwaysGuardLimits)
            .putBoolean(KEY_ONBOARDED, s.onboardingDone)
            .putBoolean(KEY_SAFETY, s.hasSeenSafety)
            .putLong(KEY_LAST_GRANT, s.lastGrantAt)
            .apply()
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_SLEEP_GUARD = "sleep_guard"
        const val KEY_BEDTIME = "bedtime"
        const val KEY_WAKE = "wake"
        const val KEY_WIND_DOWN = "wind_down"
        const val KEY_SLEEP_BLOCK_ALL = "sleep_block_all"
        const val KEY_SLEEP_LOCK = "sleep_lock"
        const val KEY_SLEEP_DND = "sleep_dnd"
        const val KEY_SLEEP_GRAY = "sleep_gray"
        const val KEY_SLEEP_FORCE_STOP = "sleep_force_stop"
        const val KEY_FOCUS = "focus"
        const val KEY_REST = "rest"
        const val KEY_AUTO_CYCLE = "auto_cycle"
        const val KEY_REST_STRICT = "rest_strict"
        const val KEY_REST_GRAY = "rest_gray"
        const val KEY_REST_DND = "rest_dnd"
        const val KEY_EMERGENCY_EXIT = "emergency_exit"
        const val KEY_ENFORCEMENT = "enforcement"
        const val KEY_FORCE_STOP = "force_stop"
        const val KEY_DAILY_LIMIT = "daily_limit"
        const val KEY_ALWAYS_GUARD = "always_guard"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_SAFETY = "safety"
        const val KEY_LAST_GRANT = "last_grant"
    }
}
