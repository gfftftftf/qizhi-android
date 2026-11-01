package com.qizhi.rest.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qizhi.rest.ui.components.QizhiBackground
import com.qizhi.rest.ui.screens.AppsScreen
import com.qizhi.rest.ui.screens.OnboardingScreen
import com.qizhi.rest.ui.screens.RhythmScreen
import com.qizhi.rest.ui.screens.SettingsScreen
import com.qizhi.rest.ui.screens.TodayScreen
import com.qizhi.rest.ui.theme.LocalQizhiPalette
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class AppTab(val label: String, val icon: ImageVector) {
    TODAY("今天", Icons.Rounded.WbSunny),
    RHYTHM("节律", Icons.Rounded.NightsStay),
    APPS("管控", Icons.Rounded.Apps),
    SETTINGS("设置", Icons.Rounded.Tune),
}

@Composable
fun AppRoot(
    viewModel: MainViewModel,
    onRequestNotifications: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(AppTab.TODAY) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    QizhiBackground {
        if (!settings.onboardingDone) {
            OnboardingScreen(
                viewModel = viewModel,
                onRequestNotifications = onRequestNotifications,
                onDone = { viewModel.completeOnboarding() },
            )
        } else {
            Box {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        fadeIn(tween(260)) togetherWith fadeOut(tween(120))
                    },
                    label = "tabContent",
                ) { target ->
                    when (target) {
                        AppTab.TODAY -> TodayScreen(
                            viewModel = viewModel,
                            onOpenSettings = { tab = AppTab.SETTINGS },
                            onOpenRhythm = { tab = AppTab.RHYTHM },
                        )

                        AppTab.RHYTHM -> RhythmScreen(viewModel)
                        AppTab.APPS -> AppsScreen(viewModel)
                        AppTab.SETTINGS -> SettingsScreen(viewModel)
                    }
                }
                QizhiBottomBar(
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 92.dp, start = 20.dp, end = 20.dp),
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = LocalQizhiPalette.current.ink,
                        contentColor = LocalQizhiPalette.current.background,
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }
        }
    }

    val grantResults by viewModel.grantResults.collectAsStateWithLifecycle()
    if (grantResults != null) {
        GrantResultsDialog(
            results = grantResults!!,
            onDismiss = viewModel::dismissGrantResults,
        )
    }
}

@Composable
private fun QizhiBottomBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalQizhiPalette.current
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = palette.surface.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            palette.line.copy(alpha = 0.8f),
        ),
        shadowElevation = 14.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppTab.entries.forEach { entry ->
                val active = entry == selected
                val alpha by animateFloatAsState(
                    targetValue = if (active) 1f else 0.55f,
                    animationSpec = tween(220),
                    label = "tabAlpha",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelect(entry) }
                        .background(
                            if (active) palette.accentSoft.copy(alpha = 0.7f) else Color.Transparent,
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = entry.label,
                        tint = if (active) palette.accentDeep else palette.muted,
                        modifier = Modifier.size(21.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) palette.ink else palette.muted.copy(alpha = alpha),
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun GrantResultsDialog(
    results: List<com.qizhi.rest.privilege.GrantResult>,
    onDismiss: () -> Unit,
) {
    val palette = LocalQizhiPalette.current
    val successCount = results.count { it.success }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text(
                    text = if (successCount == results.size) "权限已就绪" else "权限配置结果",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$successCount / ${results.size} 项成功",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                results.forEach { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (result.success) {
                                androidx.compose.material.icons.Icons.Rounded.Check
                            } else {
                                Icons.Rounded.Close
                            },
                            contentDescription = null,
                            tint = if (result.success) palette.accentDeep else palette.error,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text(
                                text = result.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.ink,
                            )
                            if (!result.success && result.detail != null) {
                                Text(
                                    text = result.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.muted,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = palette.accentDeep)
            }
        },
    )
}
