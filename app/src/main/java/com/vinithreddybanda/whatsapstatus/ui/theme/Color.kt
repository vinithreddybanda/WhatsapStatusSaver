package com.vinithreddybanda.whatsapstatus.ui.theme

import androidx.compose.ui.graphics.Color

data class AppPalette(
    val background: Color,
    val foreground: Color,
    val muted: Color,
    val accent: Color,
    val glass: Color,
    val glassBorder: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val indicatorTop: Color,
    val indicatorMiddle: Color,
    val indicatorBottom: Color,
    val sheetTop: Color,
    val sheetBottom: Color,
    val scrim: Color,
    val placeholder: Color
)

val LightPalette = AppPalette(
    background = Color(0xFFF4F2EE),
    foreground = Color(0xFF242321),
    muted = Color(0xFF88847F),
    accent = Color(0xFFE8E4DD),
    glass = Color(0xFFF7F5F1),
    glassBorder = Color.Transparent,
    glassTop = Color(0xBDFBF9F5),
    glassBottom = Color(0x8FE5E1DA),
    indicatorTop = Color(0xCFFFFFFF),
    indicatorMiddle = Color(0xA8E9E5DE),
    indicatorBottom = Color(0x66D7D1C7),
    sheetTop = Color(0xF2F8F6F1),
    sheetBottom = Color(0xE8E8E4DE),
    scrim = Color(0x660F0E0D),
    placeholder = Color(0xFFE7E3DC)
)

val DarkPalette = AppPalette(
    background = Color(0xFF111114),
    foreground = Color(0xFFF0EEE9),
    muted = Color(0xFF96939A),
    accent = Color(0xFF29282D),
    glass = Color(0xFF242329),
    glassBorder = Color.Transparent,
    glassTop = Color(0x702F2E35),
    glassBottom = Color(0x66201F25),
    indicatorTop = Color(0x38FFFFFF),
    indicatorMiddle = Color(0x292B2A31),
    indicatorBottom = Color(0x1819181D),
    sheetTop = Color(0xF0252429),
    sheetBottom = Color(0xF018171B),
    scrim = Color(0x99060508),
    placeholder = Color(0xFF242329)
)
