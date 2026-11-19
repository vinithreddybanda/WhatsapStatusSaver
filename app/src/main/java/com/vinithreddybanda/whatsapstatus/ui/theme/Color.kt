package com.vinithreddybanda.whatsapstatus.ui.theme

import androidx.compose.ui.graphics.Color

data class AppPalette(
    val background: Color,
    val foreground: Color,
    val muted: Color,
    val accent: Color,
    val glass: Color,
    val glassBorder: Color,
    val scrim: Color,
    val placeholder: Color
)

val LightPalette = AppPalette(
    background = Color(0xFFF2F2F7),
    foreground = Color(0xFF1C1C1E),
    muted = Color(0xFF8E8E93),
    accent = Color(0xFFE2E5EC),
    glass = Color(0xDDF9FAFC),
    glassBorder = Color(0x99FFFFFF),
    scrim = Color(0x4D000000),
    placeholder = Color(0xFFE5E5EA)
)

val DarkPalette = AppPalette(
    background = Color(0xFF000000),
    foreground = Color(0xFFF2F2F7),
    muted = Color(0xFF8E8E93),
    accent = Color(0xFF48484A),
    glass = Color(0xCC242428),
    glassBorder = Color(0x40FFFFFF),
    scrim = Color(0x99000000),
    placeholder = Color(0xFF1C1C1E)
)
