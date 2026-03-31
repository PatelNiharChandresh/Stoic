package com.rudy.stoic.ui.home

import android.app.WallpaperManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.rudy.stoic.ui.drawer.ArcAppDrawer
import com.rudy.stoic.ui.drawer.DrawerViewModel
import com.rudy.stoic.ui.quicksettings.ArcQuickSettings
import com.rudy.stoic.ui.quicksettings.QuickSettingsViewModel
import com.rudy.stoic.ui.ring.DualRingView

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    drawerViewModel: DrawerViewModel = hiltViewModel(),
    quickSettingsViewModel: QuickSettingsViewModel = hiltViewModel()
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
    val installedApps by drawerViewModel.installedApps.collectAsState()
    val quickSettingTiles by quickSettingsViewModel.tiles.collectAsState()

    // Refresh quick settings states when panel opens
    LaunchedEffect(panelState) {
        if (panelState == PanelState.QUICK_SETTINGS_OPEN) {
            quickSettingsViewModel.refreshStates()
        }
    }

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

        // Dual ring centered on screen (visible only on HOME state)
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

        // App Drawer — left edge semi-circle
        ArcAppDrawer(
            apps = installedApps,
            visible = panelState == PanelState.DRAWER_OPEN,
            onAppClick = { app ->
                drawerViewModel.launchApp(app, context)
                viewModel.dismissPanel()
            },
            onDismiss = { viewModel.dismissPanel() }
        )

        // Quick Settings — right edge semi-circle
        ArcQuickSettings(
            tiles = quickSettingTiles,
            visible = panelState == PanelState.QUICK_SETTINGS_OPEN,
            onTileClick = { tileId ->
                quickSettingsViewModel.toggleTile(tileId)
            },
            onDismiss = { viewModel.dismissPanel() }
        )
    }
}
