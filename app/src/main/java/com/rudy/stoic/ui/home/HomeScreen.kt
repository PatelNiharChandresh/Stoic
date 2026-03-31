package com.rudy.stoic.ui.home

import android.app.WallpaperManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.rudy.stoic.ui.ring.DualRingView

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val wallpaperBitmap = remember {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.drawable?.toBitmap()?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    val ringItems by viewModel.ringItems.collectAsState()
    val panelState by viewModel.panelState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        // Wallpaper background
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Dual ring centered on screen
        if (panelState == PanelState.HOME) {
            DualRingView(
                items = ringItems,
                modifier = Modifier.align(Alignment.Center),
                onItemTapped = { item ->
                    viewModel.onRingItemTapped(item, context)
                },
                onCenterGesture = { direction ->
                    viewModel.onCenterGesture(direction, context)
                }
            )
        }

        // Panels will be added in Step 5
        // when (panelState) {
        //     PanelState.DRAWER_OPEN -> { ... }
        //     PanelState.QUICK_SETTINGS_OPEN -> { ... }
        //     else -> { }
        // }
    }
}
