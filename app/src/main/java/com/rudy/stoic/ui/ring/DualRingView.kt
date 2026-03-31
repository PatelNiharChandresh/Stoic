package com.rudy.stoic.ui.ring

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.rotate
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

    val screenWidthDp = configuration.screenWidthDp.dp
    val ringSizeDp = outerRadiusDp?.times(2) ?: (screenWidthDp * OUTER_RADIUS_RATIO * 2)

    var pressedBlockIndex by remember { mutableIntStateOf(-1) }

    val ringSizePx = with(density) { ringSizeDp.toPx() }
    val outerRadiusPx = ringSizePx / 2f
    val innerRadiusPx = innerRadiusDp?.let { with(density) { it.toPx() } }
        ?: (outerRadiusPx * INNER_RADIUS_RATIO)

    // === ANIMATIONS ===

    // 8.1 Pulsing glow — oscillates glow radius
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // 8.1 Slow rotating scan line angle
    val scanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    // 8.2 Ring entrance — animate sweep from 0 to 360
    var entranceTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { entranceTarget = 1f }
    val entranceProgress by animateFloatAsState(
        targetValue = entranceTarget,
        animationSpec = tween(durationMillis = 1200),
        label = "ringEntrance"
    )

    // 8.2 Block fade-in (staggered per block)
    val blockAlpha by animateFloatAsState(
        targetValue = entranceTarget,
        animationSpec = tween(durationMillis = 800, delayMillis = 600),
        label = "blockFade"
    )

    // 8.5 Gesture hint arrows fade out
    val hintAlpha by animateFloatAsState(
        targetValue = if (entranceTarget == 1f) 0f else 0.6f,
        animationSpec = tween(durationMillis = 3000, delayMillis = 2000),
        label = "hintFade"
    )

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

        val animatedGlowRadius = GLOW_RADIUS * pulseGlow
        val sweepProgress = entranceProgress * 360f

        // 8.1 Pulsing glow behind circles
        drawCircleGlow(center, outerRadius, CyanGlow, animatedGlowRadius)
        drawCircleGlow(center, innerRadius, CyanGlow, animatedGlowRadius)

        // 8.5 Center zone scan effect
        drawCenterScanEffect(center, innerRadius, scanAngle)

        // 8.5 Gesture hint arrows
        if (hintAlpha > 0.01f) {
            drawGestureHints(center, innerRadius, hintAlpha)
        }

        // Draw blocks with entrance fade
        if (blockAlpha > 0.01f) {
            drawBlocks(items, center, outerRadius, innerRadius, pressedBlockIndex, blockAlpha)
        }

        // 8.2 Outer circle — tracing entrance animation
        drawArc(
            color = CyanPrimary,
            startAngle = -90f,
            sweepAngle = sweepProgress,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = CIRCLE_STROKE_WIDTH)
        )

        // 8.2 Inner circle — tracing entrance (slightly delayed feel via same progress)
        drawArc(
            color = CyanPrimary,
            startAngle = 90f,
            sweepAngle = sweepProgress,
            useCenter = false,
            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
            size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
            style = Stroke(width = CIRCLE_STROKE_WIDTH)
        )

        // 8.1 Rotating scan line on outer circle
        drawScanLine(center, outerRadius, innerRadius, scanAngle, CyanPrimary.copy(alpha = 0.15f))

        // Icons and labels with entrance fade
        if (blockAlpha > 0.01f) {
            drawIconsAndLabels(items, center, outerRadius, innerRadius, pressedBlockIndex, blockAlpha)
        }
    }
}

// === DRAWING FUNCTIONS ===

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

private fun DrawScope.drawScanLine(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    angle: Float,
    color: Color
) {
    val angleRad = angle * PI.toFloat() / 180f
    val startX = center.x + innerRadius * cos(angleRad)
    val startY = center.y + innerRadius * sin(angleRad)
    val endX = center.x + outerRadius * cos(angleRad)
    val endY = center.y + outerRadius * sin(angleRad)

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f
            maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
            this.color = color.toArgb()
        }
        canvas.nativeCanvas.drawLine(startX, startY, endX, endY, paint)
    }
}

private fun DrawScope.drawCenterScanEffect(center: Offset, innerRadius: Float, angle: Float) {
    // Subtle crosshair grid in the center
    val gridColor = CyanPrimary.copy(alpha = 0.04f)
    val gridRadius = innerRadius * 0.85f

    // Horizontal and vertical lines
    drawLine(gridColor, Offset(center.x - gridRadius, center.y), Offset(center.x + gridRadius, center.y), strokeWidth = 0.5f)
    drawLine(gridColor, Offset(center.x, center.y - gridRadius), Offset(center.x, center.y + gridRadius), strokeWidth = 0.5f)

    // Rotating crosshair
    rotate(angle, center) {
        drawLine(
            CyanPrimary.copy(alpha = 0.06f),
            Offset(center.x - gridRadius * 0.6f, center.y),
            Offset(center.x + gridRadius * 0.6f, center.y),
            strokeWidth = 0.8f
        )
    }

    // Small center dot
    drawCircle(CyanPrimary.copy(alpha = 0.1f), radius = 3f, center = center)
}

private fun DrawScope.drawGestureHints(center: Offset, innerRadius: Float, alpha: Float) {
    val hintColor = CyanPrimary.copy(alpha = alpha * 0.5f)
    val arrowSize = innerRadius * 0.15f
    val offset = innerRadius * 0.45f

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = hintColor.toArgb()
            textSize = arrowSize
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
        // Down arrow
        canvas.nativeCanvas.drawText("↓", center.x, center.y + offset, paint)
        // Left arrow
        canvas.nativeCanvas.drawText("←", center.x - offset, center.y, paint)
        // Right arrow
        canvas.nativeCanvas.drawText("→", center.x + offset, center.y, paint)
    }
}

private fun DrawScope.drawBlocks(
    items: List<RingItem>,
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    pressedIndex: Int,
    alpha: Float
) {
    val itemCount = items.size
    if (itemCount == 0) return

    val sweepAngle = RingMath.blockSweepAngle(itemCount)

    for (i in items.indices) {
        val startAngle = RingMath.blockStartAngle(i, itemCount)
        val isPressed = i == pressedIndex

        // 8.2 Staggered fade per block
        val blockDelay = i.toFloat() / itemCount
        val blockAlpha = ((alpha - blockDelay * 0.3f) / 0.7f).coerceIn(0f, 1f)
        if (blockAlpha <= 0f) continue

        val blockPath = createBlockPath(center, outerRadius, innerRadius, startAngle, sweepAngle)

        val fillColor = if (isPressed) {
            CyanPrimary.copy(alpha = 0.25f * blockAlpha)
        } else {
            BlockBackground.copy(alpha = BlockBackground.alpha * blockAlpha)
        }
        drawPath(path = blockPath, color = fillColor)

        val glowColor = if (isPressed) CyanPrimary.copy(alpha = 0.6f * blockAlpha) else CyanGlow.copy(alpha = CyanGlow.alpha * blockAlpha)
        drawBlockBorderGlow(blockPath, glowColor)

        val borderColor = if (isPressed) CyanPrimary.copy(alpha = blockAlpha) else CyanBorder.copy(alpha = CyanBorder.alpha * blockAlpha)
        val borderWidth = if (isPressed) BLOCK_BORDER_WIDTH * 2f else BLOCK_BORDER_WIDTH
        drawPath(path = blockPath, color = borderColor, style = Stroke(width = borderWidth))
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
        left = center.x - outerRadius, top = center.y - outerRadius,
        right = center.x + outerRadius, bottom = center.y + outerRadius
    )
    path.arcTo(outerRect, startAngleDeg, sweepAngleDeg, forceMoveTo = true)

    val endAngleRad = endAngleDeg * PI.toFloat() / 180f
    path.lineTo(center.x + innerRadius * cos(endAngleRad), center.y + innerRadius * sin(endAngleRad))

    val innerRect = Rect(
        left = center.x - innerRadius, top = center.y - innerRadius,
        right = center.x + innerRadius, bottom = center.y + innerRadius
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
    pressedIndex: Int,
    alpha: Float
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
            val blockDelay = i.toFloat() / itemCount
            val itemAlpha = ((alpha - blockDelay * 0.3f) / 0.7f).coerceIn(0f, 1f)
            if (itemAlpha <= 0f) continue

            val midAngle = RingMath.blockMidAngle(i, itemCount)
            val midAngleRad = midAngle * PI.toFloat() / 180f

            val iconCenterX = center.x + midRadius * cos(midAngleRad)
            val iconCenterY = center.y + midRadius * sin(midAngleRad)

            drawIconPlaceholder(
                center = Offset(iconCenterX, iconCenterY),
                size = iconSizePx,
                label = items[i].label,
                isPressed = isPressed,
                alpha = itemAlpha
            )

            val labelX = center.x + labelRadius * cos(midAngleRad)
            val labelY = center.y + labelRadius * sin(midAngleRad)

            val labelColor = if (isPressed) AmberAccent else TextPrimary
            labelPaint.color = labelColor.copy(alpha = labelColor.alpha * itemAlpha).toArgb()

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
    isPressed: Boolean,
    alpha: Float
) {
    val bgColor = if (isPressed) CyanPrimary.copy(alpha = 0.4f * alpha) else CyanPrimary.copy(alpha = 0.2f * alpha)
    val borderColor = if (isPressed) CyanPrimary.copy(alpha = alpha) else CyanBorder.copy(alpha = CyanBorder.alpha * alpha)
    val textColor = if (isPressed) AmberAccent else CyanPrimary

    drawCircle(color = bgColor, radius = size, center = center)
    drawCircle(color = borderColor, radius = size, center = center, style = Stroke(width = 1f))

    // 8.6 Icon glow halo
    if (alpha > 0.5f) {
        drawIntoCanvas { canvas ->
            val haloPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
                maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
                color = CyanGlow.copy(alpha = 0.3f * alpha).toArgb()
            }
            canvas.nativeCanvas.drawCircle(center.x, center.y, size, haloPaint)
        }
    }

    drawIntoCanvas { canvas ->
        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = textColor.copy(alpha = textColor.alpha * alpha).toArgb()
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
