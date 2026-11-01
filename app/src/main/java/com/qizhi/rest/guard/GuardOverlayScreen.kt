package com.qizhi.rest.guard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qizhi.rest.core.BlockReason
import com.qizhi.rest.core.TimeUtils
import com.qizhi.rest.data.Phase
import com.qizhi.rest.ui.theme.LocalQizhiPalette
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val quotes = listOf(
    "休息不是浪费时间，是让时间重新变得清晰。",
    "你不必一直有用。",
    "把注意力从屏幕，交还给呼吸。",
    "慢一点，才能看见。",
    "今晚的睡眠，是明天的清醒。",
    "世界不会因为你闭眼十分钟而停转。",
)

@Composable
fun GuardOverlayScreen(
    state: OverlayState,
    onHome: () -> Unit,
    onEmergency: () -> Unit,
    onScreenOff: () -> Unit,
) {
    val palette = LocalQizhiPalette.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.visible, state.endsAt) {
        while (state.visible) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val remaining = (state.endsAt - now).coerceAtLeast(0L)
    val quote = remember(now / 30_000L, state.visible) {
        quotes[((now / 30_000L) % quotes.size).toInt()]
    }
    val wakeClock = remember(state.endsAt) {
        if (state.endsAt <= 0) "" else Instant.ofEpochMilli(state.endsAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.overlayTop, palette.overlayBottom),
                ),
            ),
    ) {
        OverlayBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OverlayHeader(state)
            Spacer(Modifier.weight(0.55f))

            when (state.kind) {
                OverlayKind.REST -> RestContent(
                    remaining = remaining,
                    quote = quote,
                )

                OverlayKind.SLEEP -> SleepContent(
                    remaining = remaining,
                    wakeClock = wakeClock,
                    lockOnSleep = state.lockOnSleep,
                    onScreenOff = onScreenOff,
                )

                OverlayKind.BLOCKED, OverlayKind.LIMIT -> BlockedContent(
                    state = state,
                    onHome = onHome,
                )
            }

            Spacer(Modifier.weight(0.45f))
            if (state.kind == OverlayKind.REST || state.kind == OverlayKind.SLEEP) {
                Text(
                    text = "“$quote”",
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.ink.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(22.dp))
            }
            when {
                state.kind == OverlayKind.BLOCKED || state.kind == OverlayKind.LIMIT -> Unit
                state.allowEmergency -> HoldToExit(onConfirmed = onEmergency)
                else -> Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun OverlayHeader(state: OverlayState) {
    val palette = LocalQizhiPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(palette.ink.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "栖",
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = palette.ink,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = when (state.kind) {
                    OverlayKind.REST -> "栖止 · 休息守护"
                    OverlayKind.SLEEP -> "栖止 · 睡眠守护"
                    else -> "栖止 · 应用管控"
                },
                style = MaterialTheme.typography.labelMedium,
                color = palette.muted,
                letterSpacing = 1.sp,
            )
        }
        if (state.kind == OverlayKind.SLEEP) {
            Icon(
                imageVector = Icons.Rounded.NightsStay,
                contentDescription = null,
                tint = palette.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RestContent(remaining: Long, quote: String) {
    val palette = LocalQizhiPalette.current
    Text(
        text = "休 息",
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 8.sp,
        color = palette.muted,
    )
    Spacer(Modifier.height(22.dp))
    BreathingOrb(remaining = remaining)
    Spacer(Modifier.height(26.dp))
    Text(
        text = "把屏幕放下",
        style = MaterialTheme.typography.displayMedium,
        color = palette.ink,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "眼睛看向远处，肩膀松下来。",
        style = MaterialTheme.typography.bodyLarge,
        color = palette.muted,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SleepContent(
    remaining: Long,
    wakeClock: String,
    lockOnSleep: Boolean,
    onScreenOff: () -> Unit,
) {
    val palette = LocalQizhiPalette.current
    MoonMark()
    Spacer(Modifier.height(12.dp))
    Text(
        text = "夜 深 了",
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 8.sp,
        color = palette.muted,
    )
    Spacer(Modifier.height(20.dp))
    Text(
        text = TimeUtils.formatCountdown(remaining),
        style = MaterialTheme.typography.displayLarge,
        color = palette.ink,
        letterSpacing = 1.sp,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "距起床 · $wakeClock",
        style = MaterialTheme.typography.bodyMedium,
        color = palette.muted,
    )
    Spacer(Modifier.height(20.dp))
    Text(
        text = "手机已经替你合上今天。\n把未完成的事留给明天。",
        style = MaterialTheme.typography.bodyLarge,
        color = palette.ink.copy(alpha = 0.8f),
        textAlign = TextAlign.Center,
        lineHeight = 27.sp,
    )
    if (lockOnSleep) {
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onScreenOff,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accent,
                contentColor = if (palette.dark) Color(0xFF102016) else Color.White,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 30.dp,
                vertical = 14.dp,
            ),
        ) {
            Text("熄屏睡觉", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun BlockedContent(state: OverlayState, onHome: () -> Unit) {
    val palette = LocalQizhiPalette.current
    val title = when (state.reason) {
        BlockReason.FOCUS -> "先专注眼前的事"
        BlockReason.REST -> "现在正在休息"
        BlockReason.SLEEP -> "夜深了，先睡吧"
        BlockReason.DAILY_LIMIT -> "今日额度已用完"
        BlockReason.SLEEP_ALL -> "睡眠时间里，先放下"
        null -> "先停一停"
    }
    val message = when (state.reason) {
        BlockReason.FOCUS -> "专注结束后，它会回到你手里。"
        BlockReason.REST -> "休息不是换个屏幕继续看。"
        BlockReason.SLEEP -> "明早醒来，一切还在。"
        BlockReason.DAILY_LIMIT -> "你已经把今天的时间用在了更重要的地方。"
        BlockReason.SLEEP_ALL -> "睡眠模式正在守护你的夜晚。"
        null -> "这个应用暂时被收起。"
    }
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(palette.ink.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(38.dp),
        )
    }
    Spacer(Modifier.height(26.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        color = palette.ink,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = state.packageLabel?.let { "「$it」已暂时收起" }.orEmpty(),
        style = MaterialTheme.typography.titleMedium,
        color = palette.muted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(18.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = palette.ink.copy(alpha = 0.72f),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    Button(
        onClick = onHome,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.ink,
            contentColor = palette.background,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 34.dp,
            vertical = 14.dp,
        ),
    ) {
        Text("回到桌面", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BreathingOrb(remaining: Long) {
    val palette = LocalQizhiPalette.current
    val transition = rememberInfiniteTransition(label = "breath")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breathProgress",
    )
    val scale = when {
        progress < 1f / 3f -> 0.82f + 0.18f * (progress / (1f / 3f))
        progress < 0.5f -> 1f
        else -> 1f - 0.18f * ((progress - 0.5f) / 0.5f)
    }
    val label = when {
        progress < 1f / 3f -> "吸气"
        progress < 0.5f -> "停留"
        else -> "呼气"
    }
    Box(
        modifier = Modifier.size(248.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val base = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = 0.24f),
                        palette.accent.copy(alpha = 0.02f),
                    ),
                    center = center,
                    radius = base,
                ),
                radius = base,
                center = center,
            )
            drawCircle(
                color = palette.accent.copy(alpha = 0.32f),
                radius = base * 0.86f,
                center = center,
                style = Stroke(width = 1.2f),
            )
        }
        Canvas(
            modifier = Modifier
                .size(196.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.glow.copy(alpha = 0.38f),
                        palette.glow.copy(alpha = 0.10f),
                    ),
                    center = center,
                    radius = size.minDimension / 2f,
                ),
                radius = size.minDimension / 2f,
                center = center,
            )
            drawCircle(
                color = palette.glow.copy(alpha = 0.55f),
                radius = size.minDimension / 2f - 2f,
                center = center,
                style = Stroke(width = 1.4f),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = TimeUtils.formatCountdown(remaining),
                style = MaterialTheme.typography.displaySmall,
                color = palette.ink,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = palette.muted,
                letterSpacing = 3.sp,
            )
        }
    }
}

@Composable
private fun MoonMark() {
    val palette = LocalQizhiPalette.current
    Canvas(Modifier.size(110.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.star.copy(alpha = 0.22f),
                    Color.Transparent,
                ),
                center = center,
                radius = size.minDimension / 2f,
            ),
            radius = size.minDimension / 2f,
            center = center,
        )
        drawCircle(
            color = palette.star.copy(alpha = 0.88f),
            radius = size.minDimension * 0.22f,
            center = center,
        )
        drawCircle(
            color = palette.overlayTop,
            radius = size.minDimension * 0.19f,
            center = Offset(center.x + size.minDimension * 0.08f, center.y - size.minDimension * 0.03f),
        )
    }
}

@Composable
private fun OverlayBackdrop() {
    val palette = LocalQizhiPalette.current
    val stars = remember {
        val random = Random(37)
        List(46) {
            Triple(random.nextFloat(), random.nextFloat() * 0.68f, random.nextFloat())
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val glowCenter = Offset(size.width * 0.82f, size.height * 0.12f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.glow.copy(alpha = 0.16f), Color.Transparent),
                center = glowCenter,
                radius = size.width * 0.55f,
            ),
            radius = size.width * 0.55f,
            center = glowCenter,
        )
        val sageCenter = Offset(size.width * 0.08f, size.height * 0.72f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.accent.copy(alpha = 0.12f), Color.Transparent),
                center = sageCenter,
                radius = size.width * 0.5f,
            ),
            radius = size.width * 0.5f,
            center = sageCenter,
        )
        stars.forEach { (x, y, alpha) ->
            drawCircle(
                color = palette.star.copy(alpha = 0.10f + alpha * 0.22f),
                radius = 1f + alpha * 1.4f,
                center = Offset(size.width * x, size.height * y),
            )
        }
        val path = Path().apply {
            moveTo(0f, size.height * 0.88f)
            cubicTo(
                size.width * 0.28f,
                size.height * 0.82f,
                size.width * 0.68f,
                size.height * 0.94f,
                size.width,
                size.height * 0.86f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, palette.ink.copy(alpha = 0.035f))
        val arcPath = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.86f)
            cubicTo(
                size.width * 0.35f,
                size.height * 0.80f,
                size.width * 0.7f,
                size.height * 0.9f,
                size.width * 0.92f,
                size.height * 0.84f,
            )
        }
        drawPath(
            arcPath,
            palette.ink.copy(alpha = 0.10f),
            style = Stroke(width = 1f),
        )
    }
}

@Composable
private fun HoldToExit(onConfirmed: () -> Unit) {
    val palette = LocalQizhiPalette.current
    val haptic = LocalHapticFeedback.current
    var progress by remember { mutableFloatStateOf(0f) }
    val description = "长按六秒紧急退出守护"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(palette.ink.copy(alpha = 0.055f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        coroutineScope {
                            var fired = false
                            val start = System.currentTimeMillis()
                            val job = launch {
                                while (isActive) {
                                    val p = ((System.currentTimeMillis() - start) / 6000f)
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
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(16.dp),
                strokeWidth = 1.8.dp,
                color = palette.muted,
                trackColor = palette.ink.copy(alpha = 0.08f),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = if (progress > 0f) "继续按住…" else "长按 6 秒 · 紧急离开",
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted,
                letterSpacing = 0.6.sp,
            )
        }
    }
}
