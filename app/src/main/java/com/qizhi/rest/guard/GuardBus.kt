package com.qizhi.rest.guard

import com.qizhi.rest.core.BlockReason
import com.qizhi.rest.data.Phase
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OverlayKind { REST, SLEEP, BLOCKED, LIMIT }

data class OverlayState(
    val visible: Boolean = false,
    val kind: OverlayKind = OverlayKind.REST,
    val phase: Phase = Phase.REST,
    val startedAt: Long = 0L,
    val endsAt: Long = 0L,
    val packageName: String? = null,
    val packageLabel: String? = null,
    val reason: BlockReason? = null,
    val allowEmergency: Boolean = true,
    val lockOnSleep: Boolean = true,
)

/**
 * 进程内事件总线。AccessibilityService 与 GuardService 位于同一进程，
 * 用 SharedFlow 传递前台应用事件，避免频繁启动 Service。
 */
object GuardBus {
    private val _foreground = MutableSharedFlow<String>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val foreground: SharedFlow<String> = _foreground

    private val _overlay = MutableStateFlow(OverlayState())
    val overlay: StateFlow<OverlayState> = _overlay.asStateFlow()

    fun reportForeground(packageName: String) {
        _foreground.tryEmit(packageName)
    }

    fun setOverlay(state: OverlayState) {
        _overlay.value = state
    }

    fun hideOverlay() {
        _overlay.value = _overlay.value.copy(visible = false)
    }
}
