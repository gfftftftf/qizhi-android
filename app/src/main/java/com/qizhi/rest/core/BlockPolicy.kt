package com.qizhi.rest.core

import com.qizhi.rest.data.AppRule
import com.qizhi.rest.data.Phase

enum class BlockReason { FOCUS, REST, SLEEP, DAILY_LIMIT, SLEEP_ALL }

object BlockPolicy {

    /**
     * 永不禁用的包：系统界面、电话、紧急呼叫、权限控制器、桌面等。
     * 这些包是设备可恢复性的最后防线，任何模式下都不应被遮挡或强制停止。
     */
    val alwaysAllowed: Set<String> = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.incallui",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.emergency",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "moe.shizuku.privileged.api",
    )

    fun reasonFor(
        packageName: String,
        ownPackage: String,
        phase: Phase,
        rule: AppRule?,
        usageMinutes: Long,
        dailyLimitsEnabled: Boolean,
        sleepBlockAll: Boolean,
        isSystemApp: Boolean,
        allowedPackages: Set<String> = emptySet(),
    ): BlockReason? {
        if (packageName == ownPackage) return null
        if (packageName in alwaysAllowed || packageName in allowedPackages) return null
        // 系统应用不参与管控，避免把设备锁死在无法恢复的状态。
        if (isSystemApp) return null

        val limit = rule?.dailyLimitMinutes ?: 0
        if (dailyLimitsEnabled && limit > 0 && usageMinutes >= limit) {
            return BlockReason.DAILY_LIMIT
        }

        return when (phase) {
            Phase.FOCUS -> if (rule?.blockFocus == true) BlockReason.FOCUS else null
            Phase.REST -> if (rule?.blockRest == true) BlockReason.REST else null
            Phase.SLEEP -> when {
                rule?.blockSleep == true -> BlockReason.SLEEP
                sleepBlockAll -> BlockReason.SLEEP_ALL
                else -> null
            }
            Phase.IDLE -> null
        }
    }

    fun shouldBlock(
        packageName: String,
        ownPackage: String,
        phase: Phase,
        rule: AppRule?,
        usageMinutes: Long,
        dailyLimitsEnabled: Boolean,
        sleepBlockAll: Boolean,
        isSystemApp: Boolean,
        allowedPackages: Set<String> = emptySet(),
    ): Boolean = reasonFor(
        packageName = packageName,
        ownPackage = ownPackage,
        phase = phase,
        rule = rule,
        usageMinutes = usageMinutes,
        dailyLimitsEnabled = dailyLimitsEnabled,
        sleepBlockAll = sleepBlockAll,
        isSystemApp = isSystemApp,
        allowedPackages = allowedPackages,
    ) != null
}
