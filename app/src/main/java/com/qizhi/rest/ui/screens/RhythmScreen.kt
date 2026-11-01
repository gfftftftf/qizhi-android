package com.qizhi.rest.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qizhi.rest.core.TimeUtils
import com.qizhi.rest.data.Settings
import com.qizhi.rest.ui.MainViewModel
import com.qizhi.rest.ui.components.Eyebrow
import com.qizhi.rest.ui.components.Hairline
import com.qizhi.rest.ui.components.Pill
import com.qizhi.rest.ui.components.SectionHeading
import com.qizhi.rest.ui.components.SettingToggle
import com.qizhi.rest.ui.components.SoftCard
import com.qizhi.rest.ui.theme.LocalQizhiPalette

private enum class TimeTarget { BEDTIME, WAKE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RhythmScreen(viewModel: MainViewModel) {
    val palette = LocalQizhiPalette.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val privilege by viewModel.privilege.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf<TimeTarget?>(null) }
    var windDownLocal by remember(settings.windDownMinutes) {
        mutableFloatStateOf(settings.windDownMinutes.toFloat())
    }

    LazyColumn(
        modifier = Modifier.statusBarsPadding(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 124.dp),
    ) {
        item {
            Eyebrow("Rhythm")
            Spacer(Modifier.height(6.dp))
            Text(
                text = "节律",
                style = MaterialTheme.typography.displayMedium,
                color = palette.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "不是把每一天塞满，而是知道什么时候开始、什么时候停下。",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(24.dp))
        }

        item {
            SoftCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Eyebrow("Sleep")
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "睡眠窗口",
                                style = MaterialTheme.typography.headlineSmall,
                                color = palette.ink,
                            )
                        }
                        Pill(
                            text = if (settings.sleepGuardEnabled) "守护中" else "已暂停",
                            selected = settings.sleepGuardEnabled,
                            onClick = {
                                viewModel.updateSettings {
                                    it.copy(sleepGuardEnabled = !it.sleepGuardEnabled)
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TimeField(
                            label = "入睡",
                            time = TimeUtils.formatClock(settings.bedtimeMinutes),
                            icon = Icons.Rounded.Bedtime,
                            modifier = Modifier.weight(1f),
                            onClick = { picker = TimeTarget.BEDTIME },
                        )
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(1.dp)
                                    .background(palette.line),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "至",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.muted,
                            )
                        }
                        TimeField(
                            label = "起床",
                            time = TimeUtils.formatClock(settings.wakeMinutes),
                            icon = Icons.Rounded.LightMode,
                            modifier = Modifier.weight(1f),
                            onClick = { picker = TimeTarget.WAKE },
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Hairline()
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WbTwilight,
                                contentDescription = null,
                                tint = palette.accentDeep,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "睡前缓冲",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.ink,
                            )
                        }
                        Text(
                            text = if (settings.windDownMinutes == 0) "关闭" else "提前 ${settings.windDownMinutes} 分钟",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accentDeep,
                        )
                    }
                    Slider(
                        value = windDownLocal,
                        onValueChange = { windDownLocal = it },
                        onValueChangeFinished = {
                            viewModel.updateSettings {
                                it.copy(windDownMinutes = (windDownLocal / 5).toInt() * 5)
                            }
                        },
                        valueRange = 0f..60f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.accent,
                            activeTrackColor = palette.accent,
                            inactiveTrackColor = palette.surfaceHigh,
                        ),
                    )
                    Text(
                        text = "到点前先收到一条提醒，让身体有时间慢下来。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    Hairline()
                    SettingToggle(
                        title = "到点尝试熄屏",
                        subtitle = "睡眠开始 30 秒后，若屏幕仍亮着则尝试熄屏。",
                        checked = settings.sleepLockScreen,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(sleepLockScreen = checked) }
                        },
                    )
                    SettingToggle(
                        title = "睡眠勿扰",
                        subtitle = "进入优先模式，只允许重要来电。需要 Shizuku 或勿扰访问。",
                        checked = settings.sleepDnd,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(sleepDnd = checked) }
                        },
                    )
                    SettingToggle(
                        title = "夜间灰度",
                        subtitle = "把屏幕调成黑白，让夜晚少一点刺激。需要 Shizuku。",
                        checked = settings.sleepGrayscale,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(sleepGrayscale = checked) }
                        },
                    )
                    SettingToggle(
                        title = "阻断所有非系统应用",
                        subtitle = "比黑名单更彻底。电话、设置、桌面与紧急功能仍然可用。",
                        checked = settings.sleepBlockAll,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(sleepBlockAll = checked) }
                        },
                    )
                    SettingToggle(
                        title = "被阻断时强制停止",
                        subtitle = "通过 Shizuku 执行，减少反复切换应用的空隙。",
                        checked = settings.sleepForceStop,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(sleepForceStop = checked) }
                        },
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        item {
            SoftCard {
                Column {
                    Eyebrow("Focus & Rest")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "专注与休息",
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.ink,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "专注时长",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(25, 45, 60, 90).forEach { minutes ->
                            Pill(
                                text = "$minutes 分",
                                selected = settings.focusMinutes == minutes,
                                onClick = {
                                    viewModel.updateSettings { it.copy(focusMinutes = minutes) }
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "休息时长",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20).forEach { minutes ->
                            Pill(
                                text = "$minutes 分",
                                selected = settings.restMinutes == minutes,
                                onClick = {
                                    viewModel.updateSettings { it.copy(restMinutes = minutes) }
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Hairline()
                    SettingToggle(
                        title = "自动循环",
                        subtitle = "专注结束后自动进入休息；休息结束后，如果屏幕亮着则继续下一轮专注。",
                        checked = settings.autoCycle,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(autoCycle = checked) }
                        },
                    )
                    SettingToggle(
                        title = "严格休息",
                        subtitle = "提前结束休息需要验证设备锁屏密码；关闭后长按即可结束。",
                        checked = settings.restStrict,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(restStrict = checked) }
                        },
                    )
                    SettingToggle(
                        title = "休息勿扰",
                        subtitle = "在短暂的休息里，挡住通知的打扰。",
                        checked = settings.restDnd,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(restDnd = checked) }
                        },
                    )
                    SettingToggle(
                        title = "休息灰度",
                        subtitle = "让屏幕短暂失去颜色，帮助注意力离开内容。",
                        checked = settings.restGrayscale,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(restGrayscale = checked) }
                        },
                    )
                    SettingToggle(
                        title = "允许紧急离开",
                        subtitle = "长按守护界面 6 秒，再通过锁屏验证提前结束。关闭后只能从系统设置强制停止。",
                        checked = settings.allowEmergencyExit,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(allowEmergencyExit = checked) }
                        },
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        item {
            SoftCard(padding = PaddingValues(18.dp)) {
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "系统能力",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.ink,
                        )
                        Text(
                            text = if (privilege.shizukuReady) "Shizuku 已连接" else "部分功能受限",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (privilege.shizukuReady) palette.accentDeep else palette.glow,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when {
                            !permissions.overlay -> "悬浮窗未开启：休息与睡眠时无法显示全屏守护界面。"
                            !privilege.shizukuReady -> "Shizuku 未就绪：勿扰、灰度、精确熄屏与强制停止可能不可用。"
                            !permissions.accessibility -> "无障碍未开启：应用切换时的管控会慢一些。"
                            else -> "核心能力已就绪。所有设置会自动保存，并在下一次守护时生效。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                        lineHeight = 19.sp,
                    )
                }
            }
        }
    }

    if (picker != null) {
        val isBedtime = picker == TimeTarget.BEDTIME
        val initial = if (isBedtime) settings.bedtimeMinutes else settings.wakeMinutes
        val state = rememberTimePickerState(
            initialHour = initial / 60,
            initialMinute = initial % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { picker = null },
            containerColor = palette.surface,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = if (isBedtime) "入睡时间" else "起床时间",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.ink,
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = state)
                    Text(
                        text = "睡眠窗口可以跨越午夜。",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = state.hour * 60 + state.minute
                    viewModel.updateSettings { current ->
                        if (isBedtime) {
                            current.copy(
                                bedtimeMinutes = minutes,
                                wakeMinutes = if (current.wakeMinutes == minutes) {
                                    (minutes + 30) % 1440
                                } else {
                                    current.wakeMinutes
                                },
                            )
                        } else {
                            current.copy(
                                wakeMinutes = minutes,
                                bedtimeMinutes = if (current.bedtimeMinutes == minutes) {
                                    (minutes + 1430) % 1440
                                } else {
                                    current.bedtimeMinutes
                                },
                            )
                        }
                    }
                    picker = null
                }) {
                    Text("确定", color = palette.accentDeep)
                }
            },
            dismissButton = {
                TextButton(onClick = { picker = null }) {
                    Text("取消", color = palette.muted)
                }
            },
        )
    }
}

@Composable
private fun TimeField(
    label: String,
    time: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalQizhiPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surfaceHigh.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.accentDeep,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = time,
            style = MaterialTheme.typography.displaySmall,
            color = palette.ink,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
        )
    }
}
