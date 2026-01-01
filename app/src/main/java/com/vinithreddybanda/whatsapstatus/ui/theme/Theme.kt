package com.vinithreddybanda.whatsapstatus.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WhatsAppDarkTopBar, // Top Bar (0B141A)
    onPrimary = WhatsAppDarkTextPrimary, // Text (Whiteish)
    primaryContainer = WhatsAppDarkTopBar,
    onPrimaryContainer = WhatsAppDarkTextPrimary,

    secondary = WhatsAppDarkSecondary,
    onSecondary = WhatsAppDarkBackground,

    background = WhatsAppDarkBackground, // Main BG (0B141A)
    onBackground = WhatsAppDarkTextPrimary,

    surface = WhatsAppDarkSurface, // Surface (0B141A)
    onSurface = WhatsAppDarkTextPrimary,
    
    surfaceVariant = WhatsAppDarkSurfaceVariant, // Cards (202C33)
    onSurfaceVariant = WhatsAppDarkTextSecondary,
    
    // Bottom Sheets and Dialogs often use surfaceContainer
    surfaceContainer = WhatsAppDarkSurfaceVariant, 
    surfaceContainerHigh = WhatsAppDarkSurfaceVariant,
    surfaceContainerLow = WhatsAppDarkSurface,

    // Chips in dark mode
    secondaryContainer = WhatsAppChipSelectedDark, // Selected Chip
    onSecondaryContainer = WhatsAppChipSelectedTextDark, // Selected Chip Text
)

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppLightPrimary, // White Top Bar
    onPrimary = WhatsAppLightTextPrimary, // Black Text
    primaryContainer = WhatsAppLightPrimary,
    onPrimaryContainer = WhatsAppLightTextPrimary,

    secondary = WhatsAppLightSecondary,
    onSecondary = WhatsAppLightBackground,

    background = WhatsAppLightBackground,
    onBackground = WhatsAppLightTextPrimary,

    surface = WhatsAppLightSurface,
    onSurface = WhatsAppLightTextPrimary,
    
    surfaceVariant = WhatsAppLightSurfaceVariant,
    onSurfaceVariant = WhatsAppLightTextSecondary,

    // Chips in light mode
    secondaryContainer = WhatsAppChipSelectedLight, // Selected Chip
    onSecondaryContainer = WhatsAppChipSelectedTextLight, // Selected Chip Text
)

@Composable
fun WhatsapStatusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+

    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        //     val context = LocalContext.current
        //     if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        // }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}