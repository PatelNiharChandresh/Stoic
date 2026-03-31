package com.rudy.stoic.ui.ring

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rudy.stoic.domain.model.GestureDirection
import com.rudy.stoic.domain.model.RingItem
import com.rudy.stoic.ui.theme.AmberAccent
import com.rudy.stoic.ui.theme.BlockBackground
import com.rudy.stoic.ui.theme.CyanBorder
import com.rudy.stoic.ui.theme.CyanGlow
import com.rudy.stoic.ui.theme.CyanPrimary
import com.rudy.stoic.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val OUTER_RADIUS_RATIO = 0.40f
private const val INNER_RADIUS_RATIO = 0.65f
private const val CIRCLE_STROKE_WIDTH = 2f
private const val BLOCK_BORDER_WIDTH = 1.5f
private const val GLOW_RADIUS = 8f
private const val ICON_SIZE_RATIO = 0.35f
private const val LABEL_TEXT_SIZE_SP = 10f

@Composable
fun DualRingView(
    items: List<RingItem>,
    modifier: Modifier = Modifier,
    outerRadiusDp: Dp? = null,
    innerRadiusDp: Dp? = null,
    onItemTapped: ((RingItem) -> Unit)? = null,
    onCenterGesture: ((GestureDirection) -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val screenWidthDp = configuration.screenWidthDp.dp
    val ringSizeDp = outerRadiusDp?.times(2) ?: (screenWidthDp * OUTER_RADIUS_RATIO * 2)

    var pressedBlockIndex by remember { mutableIntStateOf(-1) }

    // Pre-compute radii in px for gesture handling
    val ringSizePx = with(density) { ringSizeDp.toPx() }
    val outerRadiusPx = ringSizePx / 2f
    val innerRadiusPx = innerRadiusDp?.let { with(density) { it.toPx() } }
        ?: (outerRadiusPx * INNER_RADIUS_RATIO)

    Canvas(
        modifier = modifier
            .size(ringSizeDp)
            .pointerInput(items.size) {
                detectCenterSwipe(
                    center = Offset(outerRadiusPx, outerRadiusPx),
                    innerRadius = innerRadiusPx,
                    onSwipe = { direction ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCenterGesture?.invoke(direction)
                    }
                )
            }
            .pointerInput(items.size) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val pos = change.position
                        val center = Offset(outerRadiusPx, outerRadiusPx)

                        val hitIndex = RingMath.hitTestBlock(
                            point = pos,
                            center = center,
                            innerRadius = innerRadiusPx,
                            outerRadius = outerRadiusPx,
                            itemCount = items.size
                        )

                        if (change.pressed) {
                            if (hitIndex >= 0) {
                                pressedBlockIndex = hitIndex
                            }
                        } else {
                            // Finger lifted
                            if (hitIndex >= 0 && hitIndex == pressedBlockIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onItemTapped?.invoke(items[hitIndex])
                            }
                            pressedBlockIndex = -1
                        }
                    }
                }
            }
    ) {
        val canvasSize = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = canvasSize / 2f
        val innerRadius = innerRadiusDp?.let {
            with(density) { it.toPx() }
        } ?: (outerRadius * INNER_RADIUS_RATIO)

        // Draw glow behind circles
        drawCircleGlow(center, outerRadius, CyanGlow, GLOW_RADIUS)
        drawCircleGlow(center, innerRadius, CyanGlow, GLOW_RADIUS)

        // Draw blocks between the two circles
        drawBlocks(items, center, outerRadius, innerRadius, pressedBlockIndex)

        // Draw outer circle
        drawCircle(
            color = CyanPrimary,
            radius = outerRadius,
            center = center,
            style = Stroke(width = CIRCLE_STROKE_WIDTH)
        )

        // Draw inner circle
        drawCircle(
            color = CyanPrimary,
            radius = innerRadius,
            center = center,
            style = Stroke(width = CIRCLE_STROKE_WIDTH)
        )

        // Draw icons and labels
        drawIconsAndLabels(items, center, outerRadius, innerRadius, pressedBlockIndex)
    }
}

private fun DrawScope.drawCircleGlow(center: Offset, radius: Float, color: Color, blurRadius: Float) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = CIRCLE_STROKE_WIDTH * 3
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            this.color = color.toArgb()
        }
        canvas.nativeCanvas.drawCircle(center.x, center.y, radius, paint)
    }
}

private fun DrawScope.drawBlocks(
    items: List<RingItem>,
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    pressedIndex: Int
) {
    val itemCount = items.size
    if (itemCount == 0) return

    val sweepAngle = RingMath.blockSweepAngle(itemCount)

    for (i in items.indices) {
        val startAngle = RingMath.blockStartAngle(i, itemCount)
        val isPressed = i == pressedIndex

        val blockPath = createBlockPath(
            center = center,
            outerRadius = outerRadius,
            innerRadius = innerRadius,
            startAngleDeg = startAngle,
            sweepAngleDeg = sweepAngle
        )

        // Fill block — brighter when pressed
        val fillColor = if (isPressed) {
            CyanPrimary.copy(alpha = 0.25f)
        } else {
            BlockBackground
        }
        drawPath(path = blockPath, color = fillColor)

        // Block border glow — stronger when pressed
        val glowColor = if (isPressed) CyanPrimary.copy(alpha = 0.6f) else CyanGlow
        drawBlockBorderGlow(blockPath, glowColor)

        // Block border — brighter when pressed
        val borderColor = if (isPressed) CyanPrimary else CyanBorder
        val borderWidth = if (isPressed) BLOCK_BORDER_WIDTH * 2f else BLOCK_BORDER_WIDTH
        drawPath(
            path = blockPath,
            color = borderColor,
            style = Stroke(width = borderWidth)
        )
    }
}

private fun createBlockPath(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    startAngleDeg: Float,
    sweepAngleDeg: Float
): Path {
    val path = Path()
    val endAngleDeg = startAngleDeg + sweepAngleDeg

    val outerRect = Rect(
        left = center.x - outerRadius,
        top = center.y - outerRadius,
        right = center.x + outerRadius,
        bottom = center.y + outerRadius
    )
    path.arcTo(outerRect, startAngleDeg, sweepAngleDeg, forceMoveTo = true)

    val endAngleRad = endAngleDeg * PI.toFloat() / 180f
    path.lineTo(
        center.x + innerRadius * cos(endAngleRad),
        center.y + innerRadius * sin(endAngleRad)
    )

    val innerRect = Rect(
        left = center.x - innerRadius,
        top = center.y - innerRadius,
        right = center.x + innerRadius,
        bottom = center.y + innerRadius
    )
    path.arcTo(innerRect, endAngleDeg, -sweepAngleDeg, forceMoveTo = false)

    path.close()
    return path
}

private fun DrawScope.drawBlockBorderGlow(path: Path, glowColor: Color) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = BLOCK_BORDER_WIDTH * 2
            maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
            this.color = glowColor.toArgb()
        }
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    }
}

private fun DrawScope.drawIconsAndLabels(
    items: List<RingItem>,
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    pressedIndex: Int
) {
    val itemCount = items.size
    if (itemCount == 0) return

    val bandWidth = outerRadius - innerRadius
    val midRadius = innerRadius + bandWidth * 0.45f
    val labelRadius = innerRadius + bandWidth * 0.80f
    val iconSizePx = bandWidth * ICON_SIZE_RATIO

    drawIntoCanvas { canvas ->
        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = TextPrimary.toArgb()
            textSize = LABEL_TEXT_SIZE_SP * density
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }

        for (i in items.indices) {
            val isPressed = i == pressedIndex
            val midAngle = RingMath.blockMidAngle(i, itemCount)
            val midAngleRad = midAngle * PI.toFloat() / 180f

            val iconCenterX = center.x + midRadius * cos(midAngleRad)
            val iconCenterY = center.y + midRadius * sin(midAngleRad)

            drawIconPlaceholder(
                center = Offset(iconCenterX, iconCenterY),
                size = iconSizePx,
                label = items[i].label,
                isPressed = isPressed
            )

            val labelX = center.x + labelRadius * cos(midAngleRad)
            val labelY = center.y + labelRadius * sin(midAngleRad)

            // Brighter label when pressed
            if (isPressed) {
                labelPaint.color = AmberAccent.toArgb()
            } else {
                labelPaint.color = TextPrimary.toArgb()
            }

            canvas.nativeCanvas.drawText(
                items[i].label,
                labelX,
                labelY + (labelPaint.textSize / 3f),
                labelPaint
            )
        }
    }
}

private fun DrawScope.drawIconPlaceholder(
    center: Offset,
    size: Float,
    label: String,
    isPressed: Boolean
) {
    val bgColor = if (isPressed) CyanPrimary.copy(alpha = 0.4f) else CyanPrimary.copy(alpha = 0.2f)
    val borderColor = if (isPressed) CyanPrimary else CyanBorder
    val textColor = if (isPressed) AmberAccent else CyanPrimary

    drawCircle(color = bgColor, radius = size, center = center)
    drawCircle(color = borderColor, radius = size, center = center, style = Stroke(width = 1f))

    drawIntoCanvas { canvas ->
        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = textColor.toArgb()
            textSize = size * 1.2f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.MONOSPACE,
                android.graphics.Typeface.BOLD
            )
        }
        canvas.nativeCanvas.drawText(
            label.first().uppercase(),
            center.x,
            center.y + (textPaint.textSize / 3f),
            textPaint
        )
    }
}
