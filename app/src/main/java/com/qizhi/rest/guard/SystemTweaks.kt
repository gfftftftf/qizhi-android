package com.qizhi.rest.guard

import com.qizhi.rest.data.EnvRepository
import com.qizhi.rest.data.GuardSession
import com.qizhi.rest.data.Phase
import com.qizhi.rest.data.Settings
import com.qizhi.rest.privilege.PrivilegedEngine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 睡眠 / 休息期间的系统级氛围调整。
 *
 * 所有改动都会在开始前记录原来的值，并在结束后恢复。若特权连接在恢复时断开，
 * 快照会保留，下一次打开栖止或 Shizuku 恢复后可以继续恢复。
 */
class SystemTweaks(
    private val engine: PrivilegedEngine,
    private val envRepository: EnvRepository,
) {

    private val mutex = Mutex()

    suspend fun applyFor(session: GuardSession, settings: Settings) = mutex.withLock {
        if (!engine.canExecute()) return@withLock
        when (session.phase) {
            Phase.SLEEP -> {
                if (settings.sleepDnd) enableDnd()
                if (settings.sleepGrayscale) enableGrayscale()
            }
            Phase.REST -> {
                if (settings.restDnd) enableDnd()
                if (settings.restGrayscale) enableGrayscale()
            }
            else -> Unit
        }
    }

    private suspend fun enableDnd() {
        val snapshot = envRepository.snapshot()
        if (!snapshot.dndSaved) {
            val previous = engine.getGlobal("zen_mode")
            if (previous.isNotBlank() && previous != "null") {
                envRepository.saveDnd(previous)
            } else {
                envRepository.saveDnd("0")
            }
        }
        engine.putGlobal("zen_mode", "1")
    }

    private suspend fun enableGrayscale() {
        val snapshot = envRepository.snapshot()
        if (!snapshot.graySaved) {
            val enabled = engine.getSecure("accessibility_display_daltonizer_enabled")
            val mode = engine.getSecure("accessibility_display_daltonizer")
            envRepository.saveGray(
                enabled = enabled.ifBlank { "0" },
                mode = mode.ifBlank { "0" },
            )
        }
        engine.putSecure("accessibility_display_daltonizer_enabled", "1")
        engine.putSecure("accessibility_display_daltonizer", "0")
    }

    /**
     * @return true 表示已恢复并清空快照。
     */
    suspend fun restore(): Boolean = mutex.withLock {
        if (!engine.canExecute()) return@withLock false
        val snapshot = envRepository.snapshot()
        if (!snapshot.dndSaved && !snapshot.graySaved) return@withLock true
        var ok = true
        if (snapshot.dndSaved) {
            val result = engine.putGlobal("zen_mode", snapshot.dndPrevious)
            ok = ok && result.ok
        }
        if (snapshot.graySaved) {
            val enabled = engine.putSecure(
                "accessibility_display_daltonizer_enabled",
                snapshot.grayEnabledPrevious,
            )
            val mode = engine.putSecure(
                "accessibility_display_daltonizer",
                snapshot.grayModePrevious,
            )
            ok = ok && enabled.ok && mode.ok
        }
        if (ok) envRepository.clear()
        ok
    }
}
