package com.qizhi.rest

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qizhi.rest.guard.GuardController
import com.qizhi.rest.privilege.PermissionTools
import com.qizhi.rest.ui.AppRoot
import com.qizhi.rest.ui.MainViewModel
import com.qizhi.rest.ui.theme.QizhiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            QizhiTheme(themeMode = settings.themeMode) {
                AppRoot(
                    viewModel = viewModel,
                    onRequestNotifications = ::requestNotifications,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAll()
        if (viewModel.isSessionActive()) {
            GuardController.resumeIfNeeded(this)
        }
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT < 33) {
            PermissionTools.openNotificationSettings(this)
            return
        }
        if (!PermissionTools.hasNotifications(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PermissionTools.openNotificationSettings(this)
        }
    }
}
