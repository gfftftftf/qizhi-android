package com.qizhi.rest.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.qizhi.rest.data.ThemeMode

val LocalQizhiPalette = staticCompositionLocalOf { LightPalette }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3F5C49),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE5D8),
    onPrimaryContainer = Color(0xFF14251A),
    secondary = Color(0xFF9A6045),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1D9CB),
    onSecondaryContainer = Color(0xFF34180D),
    tertiary = Color(0xFF5B6350),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F0E6),
    onBackground = Color(0xFF24231F),
    surface = Color(0xFFFFFCF4),
    onSurface = Color(0xFF24231F),
    surfaceVariant = Color(0xFFECE4D5),
    onSurfaceVariant = Color(0xFF5E5A50),
    outline = Color(0xFF8A8376),
    outlineVariant = Color(0xFFDED5C5),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA9C4A8),
    onPrimary = Color(0xFF17301F),
    primaryContainer = Color(0xFF2B4433),
    onPrimaryContainer = Color(0xFFD5E8D3),
    secondary = Color(0xFFE2B69E),
    onSecondary = Color(0xFF42210F),
    secondaryContainer = Color(0xFF5A3725),
    onSecondaryContainer = Color(0xFFF5DCCC),
    tertiary = Color(0xFFC2C9B4),
    onTertiary = Color(0xFF2C3225),
    background = Color(0xFF0E1211),
    onBackground = Color(0xFFECE7DB),
    surface = Color(0xFF171D1A),
    onSurface = Color(0xFFECE7DB),
    surfaceVariant = Color(0xFF202824),
    onSurfaceVariant = Color(0xFFC3C5BA),
    outline = Color(0xFF8C9188),
    outlineVariant = Color(0xFF2E3732),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

val QizhiShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp),
)

@Composable
fun QizhiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (dark) DarkPalette else LightPalette
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalQizhiPalette provides palette) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = QizhiTypography,
            shapes = QizhiShapes,
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
