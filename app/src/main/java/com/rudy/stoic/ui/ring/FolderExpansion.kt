package com.rudy.stoic.ui.ring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.rudy.stoic.domain.model.RingItem
import com.rudy.stoic.ui.theme.BackgroundDark
import com.rudy.stoic.ui.theme.BlockBackground
import com.rudy.stoic.ui.theme.CyanPrimary
import com.rudy.stoic.ui.theme.TextPrimary
import com.rudy.stoic.ui.theme.TextSecondary

data class FolderApp(
    val packageName: String,
    val name: String,
    val icon: android.graphics.drawable.Drawable?
)

@Composable
fun FolderExpansionOverlay(
    folder: RingItem?,
    visible: Boolean,
    onAppClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible || folder == null) return

    val context = LocalContext.current
    val pm = context.packageManager

    val folderApps = remember(folder.appList) {
        folder.appList.orEmpty().mapNotNull { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                FolderApp(
                    packageName = pkg,
                    name = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            } catch (_: Exception) { null }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark.copy(alpha = 0.8f))
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(BlockBackground.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
                    .pointerInput(Unit) {
                        // Consume taps inside the folder panel to prevent dismiss
                        detectTapGestures { }
                    }
            ) {
                Text(
                    text = folder.label,
                    color = CyanPrimary,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (folderApps.isEmpty()) {
                    Text(
                        text = "Empty folder\nAdd apps in Ring Config",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        items(folderApps) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onAppClick(app.packageName) }
                            ) {
                                val bitmap = remember(app.packageName) {
                                    try {
                                        app.icon?.toBitmap(128, 128)?.asImageBitmap()
                                    } catch (_: Exception) { null }
                                }
                                if (bitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap,
                                        contentDescription = app.name,
                                        modifier = Modifier.size(48.dp)
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(CyanPrimary.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Text(
                                            text = app.name.first().uppercase(),
                                            color = CyanPrimary,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Text(
                                    text = app.name,
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
