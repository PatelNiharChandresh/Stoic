package com.rudy.stoic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rudy.stoic.ui.home.HomeScreen
import com.rudy.stoic.ui.theme.StoicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupTransparentSystemBars()

        setContent {
            StoicTheme {
                HomeScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // When user presses home while already on launcher,
        // reset to the default state (dismiss any open panels)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Launcher should not exit on back press — do nothing
        // Future: dismiss open panels/overlays if any
    }

    private fun setupTransparentSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
}
