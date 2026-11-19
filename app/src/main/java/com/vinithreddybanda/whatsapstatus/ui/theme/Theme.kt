package com.vinithreddybanda.whatsapstatus.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WhatsAppDarkTopBar,
    onPrimary = WhatsAppDarkTextPrimary,
    primaryContainer = WhatsAppDarkSurfaceVariant,
    onPrimaryContainer = WhatsAppDarkTextPrimary,
    secondary = WhatsAppDarkSecondary,
    onSecondary = WhatsAppDarkBackground,
    background = WhatsAppDarkBackground,
    onBackground = WhatsAppDarkTextPrimary,
    surface = WhatsAppDarkSurface,
    onSurface = WhatsAppDarkTextPrimary,
    surfaceVariant = WhatsAppDarkSurfaceVariant,
    onSurfaceVariant = WhatsAppDarkTextSecondary,
    surfaceContainer = WhatsAppDarkSurfaceVariant,
    surfaceContainerHigh = WhatsAppDarkSurfaceVariant,
    surfaceContainerLow = WhatsAppDarkSurface,
    secondaryContainer = WhatsAppChipSelectedDark,
    onSecondaryContainer = WhatsAppChipSelectedTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppLightPrimary,
    onPrimary = Color.White,
    primaryContainer = WhatsAppLightSurface,
    onPrimaryContainer = WhatsAppLightTextPrimary,
    secondary = WhatsAppLightSecondary,
    onSecondary = WhatsAppLightTextPrimary,
    background = WhatsAppLightBackground,
    onBackground = WhatsAppLightTextPrimary,
    surface = WhatsAppLightSurface,
    onSurface = WhatsAppLightTextPrimary,
    surfaceVariant = WhatsAppLightSurfaceVariant,
    onSurfaceVariant = WhatsAppLightTextSecondary,
    secondaryContainer = WhatsAppChipSelectedLight,
    onSecondaryContainer = WhatsAppChipSelectedTextLight
)

@Composable
fun WhatsapStatusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
