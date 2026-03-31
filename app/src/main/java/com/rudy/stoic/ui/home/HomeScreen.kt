package com.rudy.stoic.ui.home

import android.app.WallpaperManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.rudy.stoic.domain.model.defaultRingItems
import com.rudy.stoic.ui.ring.DualRingView

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val wallpaperBitmap = remember {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.drawable?.toBitmap()?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    val ringItems = remember { defaultRingItems() }

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
        DualRingView(
            items = ringItems,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
