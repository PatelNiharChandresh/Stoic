package com.rudy.stoic.ui.quicksettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudy.stoic.domain.model.QuickSettingTile
import com.rudy.stoic.ui.theme.BlockBackground
import com.rudy.stoic.ui.theme.StateActive
import com.rudy.stoic.ui.theme.StateInactive
import com.rudy.stoic.ui.theme.TextPrimary
import com.rudy.stoic.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

private const val ARC_DEPTH_DP = 100f

/**
 * Calculates the horizontal offset based on the item's actual Y position on screen.
 * Arc bulges outward from the right edge:
 * - Items at the vertical CENTER are pushed FURTHEST left (peak of arc)
 * - Items at TOP/BOTTOM are closest to the right edge
 */
private fun arcOffsetForScreenY(
    screenY: Float,
    screenHeight: Float,
    arcDepth: Float
): Float {
    val normalized = ((screenY / screenHeight) * 2f - 1f).coerceIn(-1f, 1f)
    val curveFactor = cos(normalized * PI.toFloat() / 2f)
    // Negative: pushes items left from right edge at center
    return -arcDepth * curveFactor
}

@Composable
fun ArcQuickSettings(
    tiles: List<QuickSettingTile>,
    visible: Boolean,
    onTileClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    val density = LocalDensity.current
    val screenHeightPx = with(density) { screenHeightDp.dp.toPx() }
    val arcDepthPx = with(density) { ARC_DEPTH_DP.dp.toPx() }

    val itemYPositions = remember { mutableStateMapOf<Int, Float>() }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Tap-to-dismiss overlay on the left side
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (offset.x < size.width * 0.45f) {
                                onDismiss()
                            }
                        }
                    }
            )

            // The curved list anchored to right
            LazyColumn(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(260.dp)
                    .align(Alignment.CenterEnd)
                    .padding(top = 60.dp, bottom = 60.dp)
            ) {
                itemsIndexed(tiles) { index, tile ->
                    val screenY = itemYPositions[index] ?: (screenHeightPx / 2f)
                    val offset = arcOffsetForScreenY(screenY, screenHeightPx, arcDepthPx)

                    val stateColor = if (tile.isEnabled) StateActive else StateInactive
                    val textColor = if (tile.isEnabled) TextPrimary else TextSecondary
                    val bgAlpha = if (tile.isEnabled) 0.7f else 0.5f

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                itemYPositions[index] = coords.positionInRoot().y + coords.size.height / 2f
                            }
                            .offset { IntOffset(x = offset.roundToInt(), y = 0) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { onTileClick(tile.id) }
                            .background(
                                color = BlockBackground.copy(alpha = bgAlpha),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = tile.label,
                            color = textColor,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (tile.isEnabled) FontWeight.Bold else FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                    }
                }
            }
        }
    }
}
