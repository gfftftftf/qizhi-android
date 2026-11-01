package com.qizhi.rest.guard

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.qizhi.rest.QizhiApp
import com.qizhi.rest.ui.theme.LocalQizhiPalette
import com.qizhi.rest.ui.theme.QizhiTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExitConfirmActivity : ComponentActivity() {

    private var stopped = false
    private val keyguard by lazy { getSystemService(KeyguardManager::class.java) }

    private val credentialLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            stopAndFinish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        val app = application as QizhiApp
        setContent {
            QizhiTheme(themeMode = app.container.settings.current.themeMode) {
                ExitConfirmScreen(
                    deviceSecure = keyguard?.isDeviceSecure == true,
                    onVerify = ::verifyCredential,
                    onEmergency = ::stopAndFinish,
                    onCancel = { finish() },
                )
            }
        }
    }

    private fun verifyCredential() {
        val km = keyguard ?: return
        val intent = km.createConfirmDeviceCredentialIntent(
            "结束守护",
            "验证设备锁屏密码后，才能提前离开。",
        )
        if (intent == null) {
            stopAndFinish()
            return
        }
        runCatching { credentialLauncher.launch(intent) }
            .onFailure { stopAndFinish() }
    }

    private fun stopAndFinish() {
        stopped = true
        GuardController.stop(this)
        finish()
    }

    override fun onDestroy() {
        if (!stopped && isFinishing) GuardController.cancelEmergency(this)
        super.onDestroy()
    }
}

@Composable
private fun ExitConfirmScreen(
    deviceSecure: Boolean,
    onVerify: () -> Unit,
    onEmergency: () -> Unit,
    onCancel: () -> Unit,
) {
    val palette = LocalQizhiPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.overlayTop.copy(alpha = 0.96f),
                        palette.overlayBottom,
                    ),
                ),
            )
            .padding(26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(palette.surface.copy(alpha = 0.96f))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(palette.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = palette.accentDeep,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "提前离开守护",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (deviceSecure) {
                    "休息与睡眠是栖止为你守住的边界。\n如果确有需要，请验证设备锁屏密码。"
                } else {
                    "设备尚未设置锁屏密码。\n你可以长按下方按钮 8 秒，紧急结束本次守护。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(26.dp))
            if (deviceSecure) {
                Button(
                    onClick = onVerify,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.ink,
                        contentColor = palette.background,
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 15.dp),
                ) {
                    Text("验证并结束", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                EmergencyHold(onConfirmed = onEmergency)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCancel) {
                Text(
                    "继续守护",
                    color = palette.muted,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun EmergencyHold(onConfirmed: () -> Unit) {
    val palette = LocalQizhiPalette.current
    val haptic = LocalHapticFeedback.current
    var progress by remember { mutableFloatStateOf(0f) }
    var fired by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(palette.errorContainer.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        coroutineScope {
                            val start = System.currentTimeMillis()
                            val job = launch {
                                while (isActive) {
                                    val p = ((System.currentTimeMillis() - start) / 8000f)
                                        .coerceIn(0f, 1f)
                                    progress = p
                                    if (p >= 1f && !fired) {
                                        fired = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onConfirmed()
                                        break
                                    }
                                    delay(16)
                                }
                            }
                            tryAwaitRelease()
                            job.cancel()
                            progress = 0f
                        }
                    },
                )
            }
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = palette.error,
                trackColor = palette.error.copy(alpha = 0.15f),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = if (progress > 0f) "继续按住…" else "长按 8 秒紧急结束",
                style = MaterialTheme.typography.labelLarge,
                color = palette.error,
            )
        }
    }
}
