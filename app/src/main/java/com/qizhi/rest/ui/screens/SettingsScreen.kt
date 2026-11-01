package com.qizhi.rest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qizhi.rest.BuildConfig
import com.qizhi.rest.data.ThemeMode
import com.qizhi.rest.privilege.PermissionTools
import com.qizhi.rest.ui.MainViewModel
import com.qizhi.rest.ui.components.Eyebrow
import com.qizhi.rest.ui.components.Hairline
import com.qizhi.rest.ui.components.PermissionRow
import com.qizhi.rest.ui.components.PrimaryButton
import com.qizhi.rest.ui.components.QuietButton
import com.qizhi.rest.ui.components.SettingToggle
import com.qizhi.rest.ui.components.SoftCard
import com.qizhi.rest.ui.theme.LocalQizhiPalette

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val palette = LocalQizhiPalette.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val privilege by viewModel.privilege.collectAsStateWithLifecycle()
    val granting by viewModel.granting.collectAsStateWithLifecycle()
    var showPermissionInfo by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var confirmClearRules by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.statusBarsPadding(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 124.dp),
    ) {
        item {
            Eyebrow("Preferences")
            Spacer(Modifier.height(6.dp))
            Text(
                text = "设置",
                style = MaterialTheme.typography.displayMedium,
                color = palette.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "把权限交给一个克制、透明、离线运行的工具。",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
            )
            Spacer(Modifier.height(24.dp))
        }

        item {
            Text(
                text = "外观",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.ink,
            )
            Spacer(Modifier.height(10.dp))
            SoftCard {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ThemeOption(
                        title = "跟随系统",
                        icon = Icons.Rounded.BrightnessAuto,
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setTheme(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeOption(
                        title = "浅色",
                        icon = Icons.Rounded.LightMode,
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setTheme(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeOption(
                        title = "深色",
                        icon = Icons.Rounded.DarkMode,
                        selected = settings.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setTheme(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "深色模式不是简单的反色：背景更深、对比更低，适合夜晚查看。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
            Spacer(Modifier.height(26.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "高级权限",
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.ink,
                )
                Text(
                    text = "${permissions.readyCount} / ${permissions.totalCount} 就绪",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                )
            }
            Spacer(Modifier.height(10.dp))
            ShizukuCard(
                viewModel = viewModel,
                installed = privilege.installed,
                binderAlive = privilege.binderAlive,
                permissionGranted = privilege.permissionGranted,
                ready = privilege.shizukuReady,
                uid = privilege.uid,
                granting = granting,
                onOpenInfo = { showPermissionInfo = true },
            )
            Spacer(Modifier.height(12.dp))
            SoftCard(padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                Column {
                    PermissionRow(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "通知",
                        description = "守护状态与节律提醒。",
                        granted = permissions.notifications,
                        actionLabel = "去开启",
                        onAction = {
                            PermissionTools.openNotificationSettings(context)
                        },
                    )
                    Hairline()
                    PermissionRow(
                        icon = Icons.Rounded.Insights,
                        title = "使用情况访问",
                        description = "统计今日应用使用时长与前台应用。",
                        granted = permissions.usageAccess,
                        actionLabel = "去开启",
                        onAction = { PermissionTools.openUsageAccess(context) },
                    )
                    Hairline()
                    PermissionRow(
                        icon = Icons.Rounded.Layers,
                        title = "悬浮窗",
                        description = "显示全屏休息与睡眠守护界面。",
                        granted = permissions.overlay,
                        actionLabel = "去开启",
                        onAction = { PermissionTools.openOverlay(context) },
                    )
                    Hairline()
                    PermissionRow(
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = "电池优化白名单",
                        description = "让夜间守护不被系统中断。",
                        granted = permissions.batteryUnrestricted,
                        actionLabel = "去设置",
                        onAction = { PermissionTools.openBatterySettings(context) },
                    )
                    Hairline()
                    PermissionRow(
                        icon = Icons.Rounded.Alarm,
                        title = "精确闹钟",
                        description = "准点触发睡眠与起床。",
                        granted = permissions.exactAlarm,
                        actionLabel = "去开启",
                        onAction = { PermissionTools.openExactAlarm(context) },
                    )
                    Hairline()
                    PermissionRow(
                        icon = Icons.Rounded.AccessibilityNew,
                        title = "无障碍服务",
                        description = "即时识别前台应用并执行管控。",
                        granted = permissions.accessibility,
                        actionLabel = "去开启",
                        onAction = { PermissionTools.openAccessibility(context) },
                    )
                    Hairline()
                    PermissionRow(
                        icon = Icons.Rounded.Shield,
                        title = "勿扰访问",
                        description = "可选。用于应用内更精细地控制勿扰。",
                        granted = permissions.dndAccess,
                        actionLabel = "去开启",
                        onAction = { PermissionTools.openDndAccess(context) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = viewModel::testRoot) {
                    Text("测试 Root 备用通道", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                }
                TextButton(onClick = { showPermissionInfo = true }) {
                    Text("权限说明", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                }
            }
            Spacer(Modifier.height(26.dp))
        }

        item {
            Text(
                text = "守护规则",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.ink,
            )
            Spacer(Modifier.height(10.dp))
            SoftCard {
                Column {
                    SettingToggle(
                        title = "启用应用管控",
                        subtitle = "关闭后，应用不会被阻断，但休息与睡眠计时仍会运行。",
                        checked = settings.enforcementEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(enforcementEnabled = checked) }
                        },
                    )
                    SettingToggle(
                        title = "每日限额",
                        subtitle = "按应用设置额度，达到后收起应用。",
                        checked = settings.dailyLimitEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(dailyLimitEnabled = checked) }
                        },
                    )
                    SettingToggle(
                        title = "允许紧急离开",
                        subtitle = "长按 6 秒后，仍需设备锁屏验证。这是为了防止意外，而不是困住你。",
                        checked = settings.allowEmergencyExit,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(allowEmergencyExit = checked) }
                        },
                    )
                    SettingToggle(
                        title = "严格休息",
                        subtitle = "关闭后，长按 6 秒即可直接结束休息，不再验证锁屏。",
                        checked = settings.restStrict,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(restStrict = checked) }
                        },
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
        }

        item {
            Text(
                text = "系统与数据",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.ink,
            )
            Spacer(Modifier.height(10.dp))
            SoftCard(padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                Column {
                    ActionRow(
                        icon = Icons.Rounded.RestartAlt,
                        title = "恢复勿扰与屏幕颜色",
                        subtitle = "把栖止改过的系统设置恢复到守护前的状态。",
                        onClick = viewModel::restoreSystemTweaks,
                    )
                    Hairline()
                    ActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        title = "清空历史记录",
                        subtitle = "删除本机的专注、休息与睡眠统计。",
                        destructive = true,
                        onClick = { confirmClearHistory = true },
                    )
                    Hairline()
                    ActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        title = "清空应用规则",
                        subtitle = "移除所有应用的阻断与限额设置。",
                        destructive = true,
                        onClick = { confirmClearRules = true },
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
        }

        item {
            Text(
                text = "关于",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.ink,
            )
            Spacer(Modifier.height(10.dp))
            SoftCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(palette.ink),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "栖",
                            fontFamily = FontFamily.Serif,
                            fontSize = 27.sp,
                            color = palette.background,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "栖止",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.ink,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        text = "版本 ${BuildConfig.VERSION_NAME} · 离线运行 · 无广告",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted,
                    )
                    Spacer(Modifier.height(18.dp))
                    Hairline()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "栖止没有网络权限。它不收集、不上传任何数据；所有作息记录、应用规则与统计都只留在这台设备上。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.accentSoft.copy(alpha = 0.65f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = palette.accentDeep,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "本地优先 · 可随时卸载",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentDeep,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "栖止不是医疗器械，不提供睡眠诊断或治疗建议。如果长期失眠或白天过度嗜睡，请咨询专业医生。",
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
            )
        }
    }

    if (showPermissionInfo) {
        AlertDialog(
            onDismissRequest = { showPermissionInfo = false },
            containerColor = palette.surface,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text("栖止会做什么", style = MaterialTheme.typography.headlineMedium, color = palette.ink)
            },
            text = {
                Column {
                    InfoLine("通过 Shizuku 强制停止被管控的应用，不删除应用数据。")
                    InfoLine("通过 Shizuku 切换勿扰与灰度，并在结束后恢复原值。")
                    InfoLine("通过 Shizuku 或无障碍服务熄屏、返回桌面。")
                    InfoLine("通过无障碍服务识别前台应用，实现即时阻断。")
                    InfoLine("不会修改锁屏密码、不会禁用系统应用、不会拦截电话与紧急呼叫。")
                    InfoLine("没有 INTERNET 权限，所有数据仅在本机。")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPermissionInfo = false }) {
                    Text("明白", color = palette.accentDeep)
                }
            },
        )
    }
    if (confirmClearHistory) {
        ConfirmDialog(
            title = "清空历史记录？",
            message = "专注、休息与睡眠统计将被永久删除，无法恢复。应用规则不会受影响。",
            confirm = "清空",
            onConfirm = {
                confirmClearHistory = false
                viewModel.clearHistory()
            },
            onDismiss = { confirmClearHistory = false },
        )
    }
    if (confirmClearRules) {
        ConfirmDialog(
            title = "清空应用规则？",
            message = "所有应用的专注、休息、睡眠阻断与每日限额都会被移除。",
            confirm = "清空规则",
            onConfirm = {
                confirmClearRules = false
                viewModel.clearRules()
            },
            onDismiss = { confirmClearRules = false },
        )
    }
}

@Composable
private fun ShizukuCard(
    viewModel: MainViewModel,
    installed: Boolean,
    binderAlive: Boolean,
    permissionGranted: Boolean,
    ready: Boolean,
    uid: Int,
    granting: Boolean,
    onOpenInfo: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalQizhiPalette.current
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
                        text = "Shizuku",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.ink,
                    )
                    Text(
                        text = when {
                            !installed -> "未安装"
                            !binderAlive -> "已安装 · 服务未运行"
                            !permissionGranted -> "已连接 · 等待授权"
                            else -> "已连接 · UID $uid"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ready) palette.accentDeep else palette.muted,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (ready) palette.accent else palette.line),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Shizuku 让栖止以 shell 身份完成系统操作，无需 Root。" +
                    "如果你使用 Root，也可以在下方测试备用通道。",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(14.dp))
            when {
                !installed -> Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    PrimaryButton(
                        text = "下载 Shizuku",
                        onClick = { PermissionTools.openShizukuDownload(context) },
                        modifier = Modifier.weight(1f),
                    )
                    QuietButton(text = "说明", onClick = onOpenInfo)
                }

                !binderAlive -> Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    PrimaryButton(
                        text = "打开 Shizuku",
                        onClick = { PermissionTools.openShizuku(context) },
                        modifier = Modifier.weight(1f),
                    )
                    QuietButton(text = "说明", onClick = onOpenInfo)
                }

                !permissionGranted -> PrimaryButton(
                    text = "请求 Shizuku 授权",
                    onClick = viewModel::requestShizukuPermission,
                    modifier = Modifier.fillMaxWidth(),
                )

                else -> Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    PrimaryButton(
                        text = if (granting) "配置中…" else "一键配置所有权限",
                        onClick = viewModel::grantAll,
                        modifier = Modifier.weight(1f),
                        enabled = !granting,
                    )
                    if (granting) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = palette.accent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalQizhiPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) palette.accentSoft.copy(alpha = 0.8f) else palette.surfaceHigh.copy(alpha = 0.4f))
            .border(
                width = if (selected) 1.2.dp else 1.dp,
                color = if (selected) palette.accent.copy(alpha = 0.65f) else palette.line.copy(alpha = 0.6f),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) palette.accentDeep else palette.muted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) palette.accentDeep else palette.muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val palette = LocalQizhiPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) palette.error else palette.accentDeep,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) palette.error else palette.ink,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalQizhiPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = palette.error,
            )
        },
        title = {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = palette.ink)
        },
        text = {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = palette.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = palette.muted)
            }
        },
    )
}

@Composable
private fun InfoLine(text: String) {
    val palette = LocalQizhiPalette.current
    Row(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(
            text = "·",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.accentDeep,
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
            lineHeight = 19.sp,
        )
    }
}
