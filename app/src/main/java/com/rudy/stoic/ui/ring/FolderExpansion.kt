package com.rudy.stoic.ui.ring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.rudy.stoic.domain.model.RingItem
import com.rudy.stoic.ui.theme.BackgroundDark
import com.rudy.stoic.ui.theme.BlockBackground
import com.rudy.stoic.ui.theme.CyanPrimary
import com.rudy.stoic.ui.theme.TextPrimary
import com.rudy.stoic.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

data class FolderApp(
    val packageName: String,
    val name: String,
    val icon: android.graphics.drawable.Drawable?
)

private const val ARC_DEPTH_DP = 40f

/**
 * Arc offset relative to the container (not screen).
 * Items at vertical center of the container get max offset, top/bottom get zero.
 */
private fun arcOffsetInContainer(
    itemY: Float,
    containerHeight: Float,
    arcDepth: Float
): Float {
    if (containerHeight <= 0f) return 0f
    val normalized = ((itemY / containerHeight) * 2f - 1f).coerceIn(-1f, 1f)
    val curveFactor = cos(normalized * PI.toFloat() / 2f)
    return arcDepth * curveFactor
}

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
                        detectTapGestures { }
                    }
            ) {
                Text(
                    text = folder.label,
                    color = CyanPrimary,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (folderApps.isEmpty()) {
                    Text(
                        text = "Empty folder\nAdd apps in Ring Config",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                } else {
                    FolderArcList(
                        apps = folderApps,
                        onAppClick = onAppClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderArcList(
    apps: List<FolderApp>,
    onAppClick: (String) -> Unit
) {
    val itemYPositions = remember { mutableStateMapOf<Int, Float>() }
    var containerHeight = remember { 0f }

    // Determine list height based on number of apps
    val listHeight = (apps.size * 56).coerceIn(120, 360).dp
    val arcDepthPx = ARC_DEPTH_DP * 2.5f // scale for the container

    LazyColumn(
        modifier = Modifier
            .width(220.dp)
            .height(listHeight)
            .onGloballyPositioned { coords ->
                containerHeight = coords.size.height.toFloat()
            }
    ) {
        itemsIndexed(apps) { index, app ->
            val itemY = itemYPositions[index] ?: (containerHeight / 2f)
            val offset = arcOffsetInContainer(itemY, containerHeight, arcDepthPx)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        itemYPositions[index] = coords.positionInParent().y + coords.size.height / 2f
                    }
                    .offset { IntOffset(x = offset.roundToInt(), y = 0) }
                    .clickable { onAppClick(app.packageName) }
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .background(
                        color = BlockBackground.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // App icon
                val bitmap = remember(app.packageName) {
                    try {
                        app.icon?.toBitmap(128, 128)?.asImageBitmap()
                    } catch (_: Exception) { null }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.name,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(CyanPrimary.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Text(
                            text = app.name.first().uppercase(),
                            color = CyanPrimary,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = app.name,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
