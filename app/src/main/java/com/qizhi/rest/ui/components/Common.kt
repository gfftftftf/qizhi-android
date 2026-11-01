package com.qizhi.rest.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qizhi.rest.ui.theme.LocalQizhiPalette

@Composable
fun QizhiBackground(content: @Composable () -> Unit) {
    val palette = LocalQizhiPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.background,
                        palette.background,
                        palette.background.copy(alpha = 0.97f),
                    ),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = if (palette.dark) 0.12f else 0.09f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.9f, size.height * 0.05f),
                    radius = size.width * 0.8f,
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.9f, size.height * 0.05f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.glow.copy(alpha = if (palette.dark) 0.08f else 0.06f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.02f, size.height * 0.7f),
                    radius = size.width * 0.7f,
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.02f, size.height * 0.7f),
            )
            drawCircle(
                color = palette.line.copy(alpha = 0.25f),
                radius = size.width * 0.46f,
                center = Offset(size.width * 0.92f, size.height * 0.16f),
                style = Stroke(width = 1f),
            )
        }
        content()
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    val palette = LocalQizhiPalette.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = palette.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, palette.line.copy(alpha = 0.7f)),
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalQizhiPalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = palette.ink,
        )
        trailing?.invoke()
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    val palette = LocalQizhiPalette.current
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = palette.muted,
        letterSpacing = 2.sp,
    )
}

@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalQizhiPalette.current
    val background by animateColorAsState(
        targetValue = if (selected) palette.ink else palette.surfaceHigh.copy(alpha = 0.7f),
        animationSpec = tween(220),
        label = "pillBackground",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) palette.background else palette.muted,
        animationSpec = tween(220),
        label = "pillForeground",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val palette = LocalQizhiPalette.current
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.ink,
            contentColor = palette.background,
            disabledContainerColor = palette.line,
            disabledContentColor = palette.muted,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalQizhiPalette.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, palette.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.ink),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SettingToggle(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalQizhiPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) palette.ink else palette.muted,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.background,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.muted,
                uncheckedTrackColor = palette.surfaceHigh,
                uncheckedBorderColor = palette.line,
            ),
        )
    }
}

@Composable
fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalQizhiPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled && !granted, onClick = onAction)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (granted) palette.accentSoft else palette.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) palette.accentDeep else palette.muted,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.ink,
                )
                if (granted) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = palette.accentDeep,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (granted) {
            Text(
                text = "已开启",
                style = MaterialTheme.typography.labelSmall,
                color = palette.accentDeep,
            )
        } else {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = palette.accentDeep,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.accentSoft)
                    .clickable(enabled = enabled, onClick = onAction)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    val palette = LocalQizhiPalette.current
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.ink,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
fun AppIcon(
    icon: ImageBitmap?,
    name: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalQizhiPalette.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(palette.surfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            androidx.compose.foundation.Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = name.take(1),
                fontFamily = FontFamily.Serif,
                fontSize = (size.value * 0.38f).sp,
                color = palette.ink,
            )
        }
    }
}

@Composable
fun WeekBars(
    values: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
) {
    val palette = LocalQizhiPalette.current
    val max = (values.maxOfOrNull { it.second } ?: 0).coerceAtLeast(30)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, (label, value) ->
            val fraction = (value.toFloat() / max).coerceIn(0.04f, 1f)
            val animated by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(600, delayMillis = index * 40),
                label = "weekBar",
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (value > 0) "$value" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                    fontSize = 9.sp,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.surfaceHigh.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((72 * animated).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (index == values.lastIndex) palette.accent else palette.accent.copy(alpha = 0.45f),
                            ),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == values.lastIndex) palette.ink else palette.muted,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
fun Chevron(modifier: Modifier = Modifier) {
    val palette = LocalQizhiPalette.current
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = palette.muted.copy(alpha = 0.7f),
        modifier = modifier.size(18.dp),
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    val palette = LocalQizhiPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.line.copy(alpha = 0.55f)),
    )
}
