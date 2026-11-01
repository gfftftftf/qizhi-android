package com.qizhi.rest.guard

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.qizhi.rest.QizhiApp
import com.qizhi.rest.data.ThemeMode
import com.qizhi.rest.R
import com.qizhi.rest.ui.theme.QizhiTheme

class OverlayController(
    private val context: Context,
    private val onHome: () -> Unit,
    private val onEmergency: () -> Unit,
    private val onScreenOff: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var root: FrameLayout? = null
    private var owner: OverlayLifecycleOwner? = null
    private var params: WindowManager.LayoutParams? = null

    val isShowing: Boolean get() = root != null

    fun show(state: OverlayState) {
        GuardBus.setOverlay(state.copy(visible = true))
        if (root != null) {
            updateFlags(state)
            return
        }
        val wm = windowManager ?: return
        if (!android.provider.Settings.canDrawOverlays(context)) return
        val dark = resolveDark()
        val lifecycleOwner = OverlayLifecycleOwner()
        val themedContext = ContextThemeWrapper(context, R.style.Theme_Qizhi)
        val compose = ComposeView(themedContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            setContent {
                val overlay by GuardBus.overlay.collectAsState()
                QizhiTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    GuardOverlayScreen(
                        state = overlay,
                        onHome = onHome,
                        onEmergency = onEmergency,
                        onScreenOff = onScreenOff,
                    )
                }
            }
        }
        lifecycleOwner.onCreate()
        val container = FrameLayout(context).apply {
            addView(
                compose,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        val layoutParams = buildParams(state)
        val result = runCatching {
            wm.addView(container, layoutParams)
            true
        }.getOrDefault(false)
        if (!result) {
            lifecycleOwner.onDestroy()
            GuardBus.hideOverlay()
            return
        }
        owner = lifecycleOwner
        root = container
        params = layoutParams
        lifecycleOwner.onResume()
    }

    fun update(state: OverlayState) {
        GuardBus.setOverlay(state.copy(visible = true))
        updateFlags(state)
    }

    fun hide() {
        GuardBus.hideOverlay()
        val container = root ?: return
        runCatching { windowManager?.removeView(container) }
        root = null
        params = null
        owner?.onDestroy()
        owner = null
    }

    private fun updateFlags(state: OverlayState) {
        val wm = windowManager ?: return
        val container = root ?: return
        val layoutParams = params ?: return
        val keepScreenOn = state.visible && state.kind == OverlayKind.REST
        layoutParams.flags = if (keepScreenOn) {
            layoutParams.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        } else {
            layoutParams.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        }
        runCatching { wm.updateViewLayout(container, layoutParams) }
    }

    private fun buildParams(state: OverlayState): WindowManager.LayoutParams {
        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        val type = if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (state.kind == OverlayKind.REST) {
                this.flags = this.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            }
            title = "QizhiGuardOverlay"
        }
    }

    private fun resolveDark(): Boolean {
        val app = appContext as? QizhiApp
        return when (app?.container?.settings?.current?.themeMode ?: ThemeMode.SYSTEM) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> {
                val mode = appContext.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
                mode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}

private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
