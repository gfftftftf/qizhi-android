package com.qizhi.rest.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qizhi.rest.privilege.PermissionTools
import com.qizhi.rest.ui.MainViewModel
import com.qizhi.rest.ui.components.Eyebrow
import com.qizhi.rest.ui.components.Hairline
import com.qizhi.rest.ui.components.PermissionRow
import com.qizhi.rest.ui.components.PrimaryButton
import com.qizhi.rest.ui.components.SoftCard
import com.qizhi.rest.ui.theme.LocalQizhiPalette

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onRequestNotifications: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalQizhiPalette.current
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val privilege by viewModel.privilege.collectAsStateWithLifecycle()
    val granting by viewModel.granting.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 22.dp,
            end = 22.dp,
            top = 34.dp,
            bottom = 40.dp,
        ),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(palette.ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "栖",
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        color = palette.background,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "栖 止",
                        style = MaterialTheme.typography.headlineMedium,
                        letterSpacing = 4.sp,
                        color = palette.ink,
                    )
                    Text(
                        text = "让休息成为秩序",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.muted,
                        letterSpacing = 1.sp,
                    )
                }
            }
            Spacer(Modifier.height(38.dp))
            Eyebrow("A quiet guard for your attention")
            Spacer(Modifier.height(12.dp))
            Text(
                text = "在该停的时候，\n替你按下暂停。",
                style = MaterialTheme.typography.displayLarge,
                color = palette.ink,
                lineHeight = 64.sp,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "栖止不是又一个效率工具。它守住睡眠、休息与专注的边界，" +
                    "在必要时收起让你分心的应用。",
                style = MaterialTheme.typography.bodyLarge,
                color = palette.muted,
                lineHeight = 25.sp,
            )
            Spacer(Modifier.height(26.dp))
        }

        item {
            SoftCard(
                padding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            ) {
                Column {
                    OnboardPoint(
                        icon = Icons.Rounded.Shield,
                        title = "强权限，克制使用",
                        text = "通过 Shizuku 获得 shell 级能力；系统应用、电话与紧急功能永不禁用。",
                    )
                    Spacer(Modifier.height(14.dp))
                    OnboardPoint(
                        icon = Icons.Rounded.Insights,
                        title = "数据只留在本机",
                        text = "没有网络权限，没有账号，没有云端。你的作息记录只属于你。",
                    )
                    Spacer(Modifier.height(14.dp))
                    OnboardPoint(
                        icon = Icons.Rounded.CheckCircle,
                        title = "随时可以退出",
                        text = "休息与睡眠守护需要设备锁屏验证才能提前结束；不会锁死你的设备。",
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
        }

        item {
            Eyebrow("Step 01 · 基础权限")
            Spacer(Modifier.height(10.dp))
            SoftCard {
                Column {
                    PermissionRow(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "通知",
                        description = "显示守护状态、睡前提醒与完成通知。",
                        granted = permissions.notifications,
                        actionLabel = "去开启",
                        onAction = onRequestNotifications,
                    )
                    Hairline()
                    PermissionRow(
                        icon = Icons.Rounded.Insights,
                        title = "使用情况访问",
                        description = "读取前台应用与今日使用时长，用于应用管控。",
                        granted = permissions.usageAccess,
                        actionLabel = "去开启",
                        onAction = { PermissionTools.openUsageAccess(context) },
                    )
                    Hairline()
                    PermissionRow(
                        icon = Icons.Rounded.Layers,
                        title = "悬浮窗",
                        description = "在休息与睡眠时显示全屏守护界面；也可由 Shizuku 自动授予。",
                        granted = permissions.overlay,
                        actionLabel = "去开启",
                        onAction = { PermissionTools.openOverlay(context) },
                    )
                    Hairline()
                    PermissionRow(
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = "电池优化白名单",
                        description = "避免系统在夜间中断睡眠守护。",
                        granted = permissions.batteryUnrestricted,
                        actionLabel = "去设置",
                        onAction = { PermissionTools.openBatterySettings(context) },
                    )
                    Hairline()
                    PermissionRow(
                        icon = Icons.Rounded.Alarm,
                        title = "精确闹钟",
                        description = "准点进入睡眠、起床与休息阶段。",
                        granted = permissions.exactAlarm,
                        actionLabel = "去开启",
                        onAction = { PermissionTools.openExactAlarm(context) },
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
        }

        item {
            Eyebrow("Step 02 · 高权限通道")
            Spacer(Modifier.height(10.dp))
            SoftCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(palette.accentSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = palette.accentDeep,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Shizuku",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.ink,
                            )
                            Text(
                                text = when {
                                    !privilege.installed -> "未安装 · 需要先安装 Shizuku"
                                    !privilege.binderAlive -> "服务未运行 · 请打开 Shizuku"
                                    !privilege.permissionGranted -> "等待授权"
                                    else -> "已连接 · UID ${privilege.uid}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (privilege.shizukuReady) palette.accentDeep else palette.muted,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    if (privilege.shizukuReady) palette.accent else palette.line,
                                ),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Shizuku 以 shell 身份运行，可执行应用限额、强制停止、勿扰与灰度等系统操作。" +
                            "栖止也可使用 Root 作为备用通道。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    when {
                        !privilege.installed -> PrimaryButton(
                            text = "了解并安装 Shizuku",
                            onClick = { PermissionTools.openShizukuDownload(context) },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        !privilege.binderAlive -> PrimaryButton(
                            text = "打开 Shizuku 启动服务",
                            onClick = { PermissionTools.openShizuku(context) },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        !privilege.permissionGranted -> PrimaryButton(
                            text = "请求 Shizuku 授权",
                            onClick = { viewModel.requestShizukuPermission() },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        else -> PrimaryButton(
                            text = if (granting) "正在配置…" else "一键配置高级权限",
                            onClick = { viewModel.grantAll() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !granting,
                            icon = if (granting) null else Icons.Rounded.Shield,
                        )
                    }
                    if (granting) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = palette.accent,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "正在通过 Shizuku 逐项配置…",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.muted,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
        }

        item {
            Eyebrow("Step 03 · 即时管控")
            Spacer(Modifier.height(10.dp))
            SoftCard {
                Column {
                    PermissionRow(
                        icon = Icons.Rounded.AccessibilityNew,
                        title = "无障碍服务",
                        description = "识别当前应用，切换的瞬间执行管控；关闭时仍可用轮询兜底。",
                        granted = permissions.accessibility,
                        actionLabel = "去开启",
                        onAction = { PermissionTools.openAccessibility(context) },
                    )
                    Hairline()
                    PermissionRow(
                        icon = Icons.Rounded.Shield,
                        title = "勿扰访问（可选）",
                        description = "更精细地控制系统勿扰模式；Shizuku 模式下通常无需开启。",
                        granted = permissions.dndAccess,
                        actionLabel = "去开启",
                        onAction = { PermissionTools.openDndAccess(context) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "你可以在不授予任何高级权限的情况下进入应用；" +
                    "但强制休息、应用管控与自动熄屏将无法完整工作。",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(26.dp))
        }

        item {
            PrimaryButton(
                text = "进入栖止",
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "先进入，稍后在设置中补齐",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.muted,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "继续即表示你理解：栖止不是医疗器械，不提供睡眠诊断或治疗建议。",
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun OnboardPoint(
    icon: ImageVector,
    title: String,
    text: String,
) {
    val palette = LocalQizhiPalette.current
    Row {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.accentDeep,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(19.dp),
        )
        Spacer(Modifier.width(11.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.ink,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                lineHeight = 19.sp,
            )
        }
    }
}
