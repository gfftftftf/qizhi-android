package com.qizhi.rest.data

import java.time.LocalDate
import java.time.LocalTime

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class Phase { IDLE, FOCUS, REST, SLEEP }

enum class RestReason { MANUAL, CYCLE }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    // 睡眠节律
    val sleepGuardEnabled: Boolean = true,
    val bedtimeMinutes: Int = 23 * 60 + 30,
    val wakeMinutes: Int = 7 * 60,
    val windDownMinutes: Int = 30,
    val sleepBlockAll: Boolean = false,
    val sleepLockScreen: Boolean = true,
    val sleepDnd: Boolean = true,
    val sleepGrayscale: Boolean = true,
    val sleepForceStop: Boolean = true,

    // 专注与休息
    val focusMinutes: Int = 45,
    val restMinutes: Int = 10,
    val autoCycle: Boolean = false,
    val restStrict: Boolean = true,
    val restGrayscale: Boolean = false,
    val restDnd: Boolean = false,
    val allowEmergencyExit: Boolean = true,

    // 应用管控
    val enforcementEnabled: Boolean = true,
    val forceStopBlocked: Boolean = true,
    val dailyLimitEnabled: Boolean = true,
    val alwaysGuardLimits: Boolean = true,

    // 状态
    val onboardingDone: Boolean = false,
    val hasSeenSafety: Boolean = false,
    val lastGrantAt: Long = 0L,
)

data class AppRule(
    val packageName: String,
    val blockFocus: Boolean = false,
    val blockRest: Boolean = true,
    val blockSleep: Boolean = true,
    val dailyLimitMinutes: Int = 0,
) {
    val isRestricted: Boolean
        get() = blockFocus || blockRest || blockSleep || dailyLimitMinutes > 0
}

data class GuardSession(
    val phase: Phase,
    val startedAt: Long,
    val endsAt: Long,
    val restReason: RestReason? = null,
    val cycle: Int = 0,
) {
    val durationMinutes: Int
        get() = ((endsAt - startedAt).coerceAtLeast(0L) / 60_000L).toInt()

    fun remainingMillis(now: Long = System.currentTimeMillis()): Long =
        (endsAt - now).coerceAtLeast(0L)
}

data class DayRecord(
    val date: String = LocalDate.now().toString(),
    val focusMinutes: Int = 0,
    val focusSessions: Int = 0,
    val restMinutes: Int = 0,
    val restSessions: Int = 0,
    val sleepMinutes: Int = 0,
    val skippedRests: Int = 0,
) {
    val hasActivity: Boolean
        get() = focusMinutes > 0 || restMinutes > 0 || sleepMinutes > 0
}

data class EnvSnapshot(
    val dndSaved: Boolean = false,
    val dndPrevious: String = "0",
    val graySaved: Boolean = false,
    val grayEnabledPrevious: String = "0",
    val grayModePrevious: String = "0",
)

fun Settings.bedtime(): LocalTime = LocalTime.of(bedtimeMinutes / 60, bedtimeMinutes % 60)

fun Settings.wake(): LocalTime = LocalTime.of(wakeMinutes / 60, wakeMinutes % 60)
