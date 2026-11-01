package com.qizhi.rest.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qizhi.rest.core.TimeUtils
import com.qizhi.rest.data.DayRecord
import com.qizhi.rest.data.GuardSession
import com.qizhi.rest.data.Phase
import com.qizhi.rest.data.Settings
import com.qizhi.rest.guard.ExitConfirmActivity
import com.qizhi.rest.guard.GuardController
import com.qizhi.rest.ui.MainViewModel
import com.qizhi.rest.ui.components.Eyebrow
import com.qizhi.rest.ui.components.Hairline
import com.qizhi.rest.ui.components.Pill
import com.qizhi.rest.ui.components.PrimaryButton
import com.qizhi.rest.ui.components.QuietButton
import com.qizhi.rest.ui.components.SoftCard
import com.qizhi.rest.ui.components.StatCell
import com.qizhi.rest.ui.components.WeekBars
import com.qizhi.rest.ui.theme.LocalQizhiPalette
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TodayScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenRhythm: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalQizhiPalette.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val privilege by viewModel.privilege.collectAsStateWithLifecycle()

    val active = session?.takeIf { it.endsAt > now }
    val zonedNow = remember(now) { ZonedDateTime.now() }
    val sleepWindow = TimeUtils.currentOrNextSleepWindow(
        zonedNow,
        settings.bedtimeMinutes,
        settings.wakeMinutes,
    )
    val todayRecord = history.firstOrNull { it.date == TimeUtils.todayKey() } ?: DayRecord()
    val streak = viewModel.streak()
    val lastSleep = viewModel.lastSleepMinutes()
    var confirmStopFocus by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 124.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Eyebrow(
                        "${zonedNow.monthValue} 月 ${zonedNow.dayOfMonth} 日 · " +
                            TimeUtils.formatWeekday(zonedNow.toLocalDate()),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = TimeUtils.greeting(zonedNow),
                        style = MaterialTheme.typography.headlineLarge,
                        color = palette.ink,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(palette.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "栖",
                        fontFamily = FontFamily.Serif,
                        fontSize = 19.sp,
                        color = palette.ink,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        item {
            HeroCard(
                session = active,
                now = now,
                settings = settings,
                sleepStart = sleepWindow.first,
                onStartFocus = { viewModel.startFocus() },
                onStartRest = { viewModel.startRest() },
                onStopFocus = { confirmStopFocus = true },
                onResumeGuard = { GuardController.resume(context) },
                onEmergency = {
                    context.startActivity(
                        Intent(context, ExitConfirmActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
            Spacer(Modifier.height(18.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickAction(
                    icon = Icons.Rounded.PlayArrow,
                    title = "专注",
                    detail = "${settings.focusMinutes} 分钟",
                    onClick = { viewModel.startFocus() },
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    icon = Icons.Rounded.Coffee,
                    title = "休息",
                    detail = "${settings.restMinutes} 分钟",
                    onClick = { viewModel.startRest() },
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    icon = Icons.Rounded.Bedtime,
                    title = "早睡",
                    detail = "现在入睡",
                    onClick = {
                        viewModel.startSleepNow()
                        viewModel.message("睡眠守护已开始。")
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        if (!permissions.overlay || !permissions.usageAccess || !privilege.shizukuReady) {
            item {
                PermissionBanner(
                    missing = listOfNotNull(
                        "悬浮窗".takeIf { !permissions.overlay },
                        "使用情况".takeIf { !permissions.usageAccess },
                        "Shizuku".takeIf { !privilege.shizukuReady },
                    ),
                    onOpenSettings = onOpenSettings,
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        item {
            SleepCard(
                settings = settings,
                sleepStart = sleepWindow.first,
                sleepEnd = sleepWindow.second,
                now = zonedNow,
                lastSleep = lastSleep,
                inSleepWindow = TimeUtils.isInSleepWindow(
                    zonedNow,
                    settings.bedtimeMinutes,
                    settings.wakeMinutes,
                ),
                onOpenRhythm = onOpenRhythm,
                onSleepNow = {
                    viewModel.startSleepNow()
                    viewModel.message("睡眠守护已开始。")
                },
            )
            Spacer(Modifier.height(24.dp))
        }

        item {
            SectionStats(
                todayRecord = todayRecord,
                history = history,
                streak = streak,
            )
            Spacer(Modifier.height(24.dp))
        }

        item {
            SoftCard(padding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = palette.accentDeep,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text(
                            text = "守护边界",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.ink,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "系统应用、电话、紧急呼叫与权限控制器永不被阻断。" +
                                "休息与睡眠守护可以通过设备锁屏验证提前结束。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted,
                            lineHeight = 19.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "栖止 · 所有数据仅保存在本机",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted.copy(alpha = 0.7f),
                letterSpacing = 0.8.sp,
            )
        }
    }

    if (confirmStopFocus) {
        AlertDialog(
            onDismissRequest = { confirmStopFocus = false },
            containerColor = palette.surface,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text("结束这次专注？", style = MaterialTheme.typography.headlineMedium, color = palette.ink)
            },
            text = {
                Text(
                    "已投入的时间仍会记入今天。休息一下，或者直接回到手头的事。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmStopFocus = false
                    viewModel.stopSession()
                }) {
                    Text("结束专注", color = palette.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStopFocus = false }) {
                    Text("继续", color = palette.accentDeep)
                }
            },
        )
    }
}

@Composable
private fun HeroCard(
    session: GuardSession?,
    now: Long,
    settings: Settings,
    sleepStart: ZonedDateTime,
    onStartFocus: () -> Unit,
    onStartRest: () -> Unit,
    onStopFocus: () -> Unit,
    onResumeGuard: () -> Unit,
    onEmergency: () -> Unit,
) {
    val palette = LocalQizhiPalette.current
    val active = session != null
    val phase = session?.phase
    val progress = if (session != null) {
        ((now - session.startedAt).toFloat() /
            (session.endsAt - session.startedAt).coerceAtLeast(1L)).coerceIn(0f, 1f)
    } else {
        0f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(palette.heroTop, palette.heroBottom),
                ),
            ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.glow.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.1f),
                    radius = size.width * 0.55f,
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.85f, size.height * 0.1f),
            )
            val center = Offset(size.width * 0.86f, size.height * 0.32f)
            drawCircle(
                color = palette.ink.copy(alpha = 0.06f),
                radius = size.width * 0.28f,
                center = center,
                style = Stroke(width = 1f),
            )
            drawCircle(
                color = palette.ink.copy(alpha = 0.035f),
                radius = size.width * 0.42f,
                center = center,
                style = Stroke(width = 1f),
            )
            repeat(9) { i ->
                val angle = (i * 40f + 12f) * (Math.PI / 180f)
                val x = center.x + cos(angle).toFloat() * size.width * 0.34f
                val y = center.y + sin(angle).toFloat() * size.width * 0.34f
                drawCircle(
                    color = palette.star.copy(alpha = 0.28f),
                    radius = 1.5f,
                    center = Offset(x, y),
                )
            }
        }
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (active) palette.accent else palette.muted.copy(alpha = 0.5f)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (phase) {
                            Phase.FOCUS -> "专注进行中"
                            Phase.REST -> "休息进行中"
                            Phase.SLEEP -> "睡眠守护中"
                            else -> "此刻 · 清醒"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.ink.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                    )
                }
                Text(
                    text = nowClock(now),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.muted,
                )
            }
            Spacer(Modifier.height(26.dp))
            if (session == null) {
                Text(
                    text = "留一点余地，\n给今天。",
                    style = MaterialTheme.typography.displayMedium,
                    color = palette.ink,
                    lineHeight = 52.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "下一次入睡 · ${TimeUtils.formatRelativeMinutes(TimeUtils.minutesUntil(ZonedDateTime.now(), sleepStart))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                )
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        text = "开始专注",
                        onClick = onStartFocus,
                        icon = Icons.Rounded.PlayArrow,
                    )
                    QuietButton(
                        text = "立即休息",
                        onClick = onStartRest,
                    )
                }
            } else {
                Text(
                    text = when (phase) {
                        Phase.FOCUS -> "把注意力\n放回一件事。"
                        Phase.REST -> "把屏幕放下，\n慢慢呼吸。"
                        Phase.SLEEP -> "夜深了，\n把今天合上。"
                        else -> ""
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.ink,
                    lineHeight = 38.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = TimeUtils.formatCountdown(session.endsAt - now),
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.ink,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "预计 ${clockOf(session.endsAt)} 结束",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = palette.accent,
                    trackColor = palette.ink.copy(alpha = 0.08f),
                )
                Spacer(Modifier.height(20.dp))
                when (phase) {
                    Phase.FOCUS -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(text = "提前休息", onClick = onStartRest)
                        QuietButton(text = "结束专注", onClick = onStopFocus)
                    }

                    Phase.REST, Phase.SLEEP -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(text = "回到守护", onClick = onResumeGuard)
                        QuietButton(text = "紧急结束", onClick = onEmergency)
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    title: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalQizhiPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surface.copy(alpha = 0.86f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.accentDeep,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.ink,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
        )
    }
}

@Composable
private fun PermissionBanner(missing: List<String>, onOpenSettings: () -> Unit) {
    val palette = LocalQizhiPalette.current
    SoftCard(
        padding = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(palette.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = palette.accentDeep,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "守护还差几步",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.ink,
                )
                Text(
                    text = "尚未就绪：${missing.joinToString("、")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
            TextButton(onClick = onOpenSettings) {
                Text("去配置", color = palette.accentDeep)
            }
        }
    }
}

@Composable
private fun SleepCard(
    settings: Settings,
    sleepStart: ZonedDateTime,
    sleepEnd: ZonedDateTime,
    now: ZonedDateTime,
    lastSleep: Int,
    inSleepWindow: Boolean,
    onOpenRhythm: () -> Unit,
    onSleepNow: () -> Unit,
) {
    val palette = LocalQizhiPalette.current
    SoftCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Eyebrow("Tonight")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (inSleepWindow) "正在睡眠守护" else "今夜节律",
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.ink,
                    )
                }
                Pill(
                    text = if (settings.sleepGuardEnabled) "已开启" else "已关闭",
                    selected = settings.sleepGuardEnabled,
                    onClick = onOpenRhythm,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = TimeUtils.formatClock(settings.bedtimeMinutes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.ink,
                    )
                    Text("入睡", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                }
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DarkMode,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = TimeUtils.formatClock(settings.wakeMinutes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.ink,
                    )
                    Text("起床", style = MaterialTheme.typography.labelSmall, color = palette.muted)
                }
            }
            Spacer(Modifier.height(14.dp))
            SleepTimeline(
                bedtime = settings.bedtimeMinutes,
                wake = settings.wakeMinutes,
                nowMinute = now.hour * 60 + now.minute,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        inSleepWindow -> "距起床 ${TimeUtils.formatCompactDuration(TimeUtils.minutesUntil(now, sleepEnd).toInt())}"
                        lastSleep > 0 -> "昨夜守护 ${TimeUtils.formatCompactDuration(lastSleep)}"
                        else -> "距入睡 ${TimeUtils.formatCompactDuration(TimeUtils.minutesUntil(now, sleepStart).toInt())}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
                if (!inSleepWindow) {
                    TextButton(onClick = onSleepNow) {
                        Text("今晚早睡", color = palette.accentDeep, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Text(
                        text = "安睡",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentDeep,
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepTimeline(bedtime: Int, wake: Int, nowMinute: Int) {
    val palette = LocalQizhiPalette.current
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
    ) {
        val y = size.height / 2f
        val radius = size.height / 2f
        drawLine(
            color = palette.line.copy(alpha = 0.7f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )
        fun xOf(minute: Int) = size.width * (minute / 1440f)
        if (bedtime < wake) {
            drawLine(
                color = palette.accent,
                start = Offset(xOf(bedtime), y),
                end = Offset(xOf(wake), y),
                strokeWidth = 5f,
                cap = StrokeCap.Round,
            )
        } else {
            drawLine(
                color = palette.accent,
                start = Offset(xOf(bedtime), y),
                end = Offset(size.width, y),
                strokeWidth = 5f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = palette.accent,
                start = Offset(0f, y),
                end = Offset(xOf(wake), y),
                strokeWidth = 5f,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = palette.glow,
            radius = radius + 1.5f,
            center = Offset(xOf(nowMinute), y),
        )
        drawCircle(
            color = palette.background,
            radius = radius - 2.5f,
            center = Offset(xOf(nowMinute), y),
        )
    }
}

@Composable
private fun SectionStats(
    todayRecord: DayRecord,
    history: List<DayRecord>,
    streak: Int,
) {
    val palette = LocalQizhiPalette.current
    val week = remember(history) {
        val map = history.associateBy { it.date }
        TimeUtils.lastNDates(7).map { date ->
            val label = when (date.dayOfWeek.value) {
                1 -> "一"
                2 -> "二"
                3 -> "三"
                4 -> "四"
                5 -> "五"
                6 -> "六"
                else -> "日"
            }
            label to (map[date.toString()]?.focusMinutes ?: 0)
        }
    }
    SoftCard {
        Column {
            Eyebrow("Today")
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatCell(
                    value = "${todayRecord.focusMinutes}",
                    label = "专注分钟",
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    value = "${todayRecord.restSessions}",
                    label = "完成休息",
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    value = if (streak > 0) "$streak 天" else "—",
                    label = "连续守护",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))
            Hairline()
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "近七日专注",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.muted,
                )
                Text(
                    text = if (todayRecord.focusMinutes > 0) "今天已投入 ${todayRecord.focusMinutes} 分钟" else "今天还没有专注记录",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.height(12.dp))
            WeekBars(values = week)
        }
    }
}

private fun nowClock(now: Long): String =
    java.time.Instant.ofEpochMilli(now)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

private fun clockOf(epoch: Long): String =
    java.time.Instant.ofEpochMilli(epoch)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
