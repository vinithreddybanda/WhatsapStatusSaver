package com.vinithreddybanda.whatsapstatus

import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vinithreddybanda.whatsapstatus.ui.theme.WhatsapStatusTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setOnExitAnimationListener { splash ->
            splash.iconView.animate()
                .alpha(0f)
                .translationY(-12f)
                .scaleX(0.84f)
                .scaleY(0.84f)
                .setDuration(260L)
                .setInterpolator(DecelerateInterpolator())
                .start()
            splash.view.animate()
                .alpha(0f)
                .setDuration(260L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { splash.remove() }
                .start()
        }

        setContent {
            WhatsapStatusTheme {
                GalleryScreen()
            }
        }
    }
}
