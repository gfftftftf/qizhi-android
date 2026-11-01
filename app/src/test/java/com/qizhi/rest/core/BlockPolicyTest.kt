package com.qizhi.rest.core

import com.qizhi.rest.data.AppRule
import com.qizhi.rest.data.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlockPolicyTest {

    private val own = "com.qizhi.rest"

    @Test
    fun systemAndOwnAppsAreNeverBlocked() {
        assertNull(reason(Phase.SLEEP, packageName = "android", isSystem = true, sleepAll = true))
        assertNull(reason(Phase.SLEEP, packageName = own, isSystem = false, sleepAll = true))
        assertNull(
            reason(
                Phase.SLEEP,
                packageName = "com.android.settings",
                isSystem = false,
                sleepAll = true,
            ),
        )
    }

    @Test
    fun focusRuleOnlyBlocksDuringFocus() {
        val rule = AppRule("com.example.app", blockFocus = true, blockRest = false, blockSleep = false)
        assertEquals(BlockReason.FOCUS, reason(Phase.FOCUS, rule = rule))
        assertNull(reason(Phase.REST, rule = rule))
        assertNull(reason(Phase.IDLE, rule = rule))
    }

    @Test
    fun restRuleBlocksRest() {
        val rule = AppRule("com.example.app", blockRest = true)
        assertEquals(BlockReason.REST, reason(Phase.REST, rule = rule))
    }

    @Test
    fun sleepAllBlocksEveryUserApp() {
        assertEquals(
            BlockReason.SLEEP_ALL,
            reason(Phase.SLEEP, packageName = "com.example.random", isSystem = false, sleepAll = true),
        )
    }

    @Test
    fun dailyLimitIndependentOfPhase() {
        val rule = AppRule("com.example.app", dailyLimitMinutes = 30)
        assertEquals(BlockReason.DAILY_LIMIT, reason(Phase.IDLE, rule = rule, usage = 30))
        assertEquals(BlockReason.DAILY_LIMIT, reason(Phase.FOCUS, rule = rule, usage = 31))
        assertNull(reason(Phase.IDLE, rule = rule, usage = 29))
    }

    @Test
    fun disabledDailyLimitsAreIgnored() {
        val rule = AppRule("com.example.app", dailyLimitMinutes = 30)
        assertNull(
            reason(
                Phase.IDLE,
                rule = rule,
                usage = 60,
                dailyLimits = false,
            ),
        )
    }

    @Test
    fun allowlistedPackageIsAllowed() {
        assertNull(
            reason(
                Phase.SLEEP,
                packageName = "com.example.launcher",
                isSystem = false,
                sleepAll = true,
                allowed = setOf("com.example.launcher"),
            ),
        )
    }

    @Test
    fun systemAppIsNotBlockedEvenWithRule() {
        val rule = AppRule("com.android.systemui", blockSleep = true, dailyLimitMinutes = 1)
        assertNull(reason(Phase.SLEEP, packageName = "com.android.systemui", rule = rule, isSystem = true, usage = 100))
    }

    private fun reason(
        phase: Phase,
        packageName: String = "com.example.app",
        rule: AppRule? = null,
        usage: Long = 0,
        dailyLimits: Boolean = true,
        sleepAll: Boolean = false,
        isSystem: Boolean = false,
        allowed: Set<String> = emptySet(),
    ): BlockReason? = BlockPolicy.reasonFor(
        packageName = packageName,
        ownPackage = own,
        phase = phase,
        rule = rule,
        usageMinutes = usage,
        dailyLimitsEnabled = dailyLimits,
        sleepBlockAll = sleepAll,
        isSystemApp = isSystem,
        allowedPackages = allowed,
    )
}
