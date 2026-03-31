package com.rudy.stoic.ui.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.rudy.stoic.domain.model.InstalledApp
import com.rudy.stoic.ui.theme.BlockBackground
import com.rudy.stoic.ui.theme.CyanPrimary
import com.rudy.stoic.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

private const val ARC_DEPTH_DP = 60f
private const val ICON_SIZE_DP = 36f

/**
 * Calculates the horizontal offset based on the item's actual Y position on screen.
 * Arc bulges outward from the left edge:
 * - Items at the vertical CENTER of the screen are pushed FURTHEST right (peak of arc)
 * - Items at TOP/BOTTOM are closest to the left edge
 */
private fun arcOffsetForScreenY(
    screenY: Float,
    screenHeight: Float,
    arcDepth: Float
): Float {
    val normalized = ((screenY / screenHeight) * 2f - 1f).coerceIn(-1f, 1f)
    val curveFactor = cos(normalized * PI.toFloat() / 2f)
    return arcDepth * curveFactor
}

@Composable
fun ArcAppDrawer(
    apps: List<InstalledApp>,
    visible: Boolean,
    onAppClick: (InstalledApp) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    val density = LocalDensity.current
    val screenHeightPx = with(density) { screenHeightDp.dp.toPx() }
    val arcDepthPx = with(density) { ARC_DEPTH_DP.dp.toPx() }

    // Track each item's actual screen Y position
    val itemYPositions = remember { mutableStateMapOf<Int, Float>() }

    val groupedApps = remember(apps) {
        val result = mutableListOf<Any>()
        var currentLetter = ' '
        for (app in apps) {
            val letter = app.name.first().uppercaseChar()
            if (letter != currentLetter) {
                currentLetter = letter
                result.add(letter.toString())
            }
            result.add(app)
        }
        result
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = androidx.compose.animation.core.tween(300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Tap-to-dismiss overlay on the right side
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (offset.x > size.width * 0.55f) {
                                onDismiss()
                            }
                        }
                    }
            )

            // The curved list
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .padding(top = 40.dp, bottom = 40.dp)
            ) {
                itemsIndexed(groupedApps) { index, item ->
                    val screenY = itemYPositions[index] ?: (screenHeightPx / 2f)
                    val offset = arcOffsetForScreenY(screenY, screenHeightPx, arcDepthPx)

                    when (item) {
                        is String -> {
                            Text(
                                text = item,
                                color = CyanPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .onGloballyPositioned { coords ->
                                        itemYPositions[index] = coords.positionInRoot().y + coords.size.height / 2f
                                    }
                                    .offset { IntOffset(x = offset.roundToInt(), y = 0) }
                                    .padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        is InstalledApp -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .onGloballyPositioned { coords ->
                                        itemYPositions[index] = coords.positionInRoot().y + coords.size.height / 2f
                                    }
                                    .offset { IntOffset(x = offset.roundToInt(), y = 0) }
                                    .clickable { onAppClick(item) }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .background(
                                        color = BlockBackground.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                AppIcon(app = item, sizeDp = ICON_SIZE_DP)

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = item.name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(app: InstalledApp, sizeDp: Float) {
    val bitmap = remember(app.packageName) {
        try {
            app.icon?.toBitmap(width = 128, height = 128)?.asImageBitmap()
        } catch (_: Exception) { null }
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = app.name,
            modifier = Modifier.size(sizeDp.dp)
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(sizeDp.dp)
                .background(color = CyanPrimary.copy(alpha = 0.2f), shape = CircleShape)
        ) {
            Text(
                text = app.name.first().uppercase(),
                color = CyanPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
