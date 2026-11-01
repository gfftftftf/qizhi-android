package com.qizhi.rest.ui.theme

import androidx.compose.ui.graphics.Color

data class QizhiPalette(
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentSoft: Color,
    val line: Color,
    val heroTop: Color,
    val heroBottom: Color,
    val overlayTop: Color,
    val overlayBottom: Color,
    val glow: Color,
    val star: Color,
)

val LightPalette = QizhiPalette(
    dark = false,
    background = Color(0xFFF5F0E6),
    surface = Color(0xFFFFFCF4),
    surfaceHigh = Color(0xFFECE4D5),
    ink = Color(0xFF24231F),
    muted = Color(0xFF777267),
    accent = Color(0xFF4F6B57),
    accentDeep = Color(0xFF34503E),
    accentSoft = Color(0xFFDCE5D8),
    line = Color(0xFFDED5C5),
    heroTop = Color(0xFFE8DDC9),
    heroBottom = Color(0xFFF7F0E3),
    overlayTop = Color(0xFFF1E7D5),
    overlayBottom = Color(0xFFD9C7AD),
    glow = Color(0xFFC98A63),
    star = Color(0xFF8E7A5E),
)

val DarkPalette = QizhiPalette(
    dark = true,
    background = Color(0xFF0E1211),
    surface = Color(0xFF171D1A),
    surfaceHigh = Color(0xFF202824),
    ink = Color(0xFFECE7DB),
    muted = Color(0xFF9B9C90),
    accent = Color(0xFFA9C4A8),
    accentDeep = Color(0xFF7FA083),
    accentSoft = Color(0xFF263A2E),
    line = Color(0xFF2E3732),
    heroTop = Color(0xFF1A2721),
    heroBottom = Color(0xFF111714),
    overlayTop = Color(0xFF101A17),
    overlayBottom = Color(0xFF070B0A),
    glow = Color(0xFFD79A76),
    star = Color(0xFFD8C9A8),
)
