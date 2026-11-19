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
    background = Color(0xFFF1F2EF),
    foreground = Color(0xFF171A16),
    muted = Color(0xFF747A72),
    accent = Color(0xFF6D8D35),
    glass = Color(0xDDFBFCF8),
    glassBorder = Color(0x66FFFFFF),
    scrim = Color(0x52090D08),
    placeholder = Color(0xFFE3E6DF)
)

val DarkPalette = AppPalette(
    background = Color(0xFF090B09),
    foreground = Color(0xFFF3F5EF),
    muted = Color(0xFF9BA197),
    accent = Color(0xFFD0F879),
    glass = Color(0xC91A1E19),
    glassBorder = Color(0x33FFFFFF),
    scrim = Color(0x99000000),
    placeholder = Color(0xFF1B201A)
)

