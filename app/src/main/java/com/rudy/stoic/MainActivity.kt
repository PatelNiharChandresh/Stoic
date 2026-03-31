package com.rudy.stoic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rudy.stoic.ui.home.HomeScreen
import com.rudy.stoic.ui.home.HomeViewModel
import com.rudy.stoic.ui.home.PanelState
import com.rudy.stoic.ui.theme.StoicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

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
        // When user presses home while already on launcher, dismiss any open panel
        homeViewModel.dismissPanel()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // If a panel is open, dismiss it. Otherwise do nothing (launcher doesn't exit).
        if (homeViewModel.panelState.value != PanelState.HOME) {
            homeViewModel.dismissPanel()
        }
    }

    private fun setupTransparentSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
}
