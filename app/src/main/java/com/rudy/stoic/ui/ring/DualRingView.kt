package com.rudy.stoic.ui.ring

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rudy.stoic.domain.model.RingItem
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
private const val INNER_RADIUS_RATIO = 0.65f // inner = outer * this
private const val CIRCLE_STROKE_WIDTH = 2f
private const val BLOCK_BORDER_WIDTH = 1.5f
private const val GLOW_RADIUS = 8f
private const val ICON_SIZE_RATIO = 0.35f // icon size relative to band width
private const val LABEL_TEXT_SIZE_SP = 10f

@Composable
fun DualRingView(
    items: List<RingItem>,
    modifier: Modifier = Modifier,
    outerRadiusDp: Dp? = null,
    innerRadiusDp: Dp? = null
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthDp = configuration.screenWidthDp.dp
    val ringSizeDp = outerRadiusDp?.times(2) ?: (screenWidthDp * OUTER_RADIUS_RATIO * 2)

    Canvas(
        modifier = modifier.size(ringSizeDp)
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
        drawBlocks(items, center, outerRadius, innerRadius)

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
        drawIconsAndLabels(items, center, outerRadius, innerRadius)
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
    innerRadius: Float
) {
    val itemCount = items.size
    if (itemCount == 0) return

    val sweepAngle = RingMath.blockSweepAngle(itemCount)

    for (i in items.indices) {
        val startAngle = RingMath.blockStartAngle(i, itemCount)

        val blockPath = createBlockPath(
            center = center,
            outerRadius = outerRadius,
            innerRadius = innerRadius,
            startAngleDeg = startAngle,
            sweepAngleDeg = sweepAngle
        )

        // Fill block
        drawPath(
            path = blockPath,
            color = BlockBackground
        )

        // Block border with glow
        drawBlockBorderGlow(blockPath, CyanGlow)

        // Block border
        drawPath(
            path = blockPath,
            color = CyanBorder,
            style = Stroke(width = BLOCK_BORDER_WIDTH)
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

    // Outer arc
    val outerRect = Rect(
        left = center.x - outerRadius,
        top = center.y - outerRadius,
        right = center.x + outerRadius,
        bottom = center.y + outerRadius
    )
    path.arcTo(outerRect, startAngleDeg, sweepAngleDeg, forceMoveTo = true)

    // Straight line inward to inner circle (at end angle)
    val endAngleRad = endAngleDeg * PI.toFloat() / 180f
    path.lineTo(
        center.x + innerRadius * cos(endAngleRad),
        center.y + innerRadius * sin(endAngleRad)
    )

    // Inner arc (reversed — from end angle back to start angle)
    val innerRect = Rect(
        left = center.x - innerRadius,
        top = center.y - innerRadius,
        right = center.x + innerRadius,
        bottom = center.y + innerRadius
    )
    path.arcTo(innerRect, endAngleDeg, -sweepAngleDeg, forceMoveTo = false)

    // Straight line outward to close the path
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
    innerRadius: Float
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
            val midAngle = RingMath.blockMidAngle(i, itemCount)
            val midAngleRad = midAngle * PI.toFloat() / 180f

            // Icon position (centered in band)
            val iconCenterX = center.x + midRadius * cos(midAngleRad)
            val iconCenterY = center.y + midRadius * sin(midAngleRad)

            // Draw icon placeholder (circle with first letter)
            drawIconPlaceholder(
                center = Offset(iconCenterX, iconCenterY),
                size = iconSizePx,
                label = items[i].label
            )

            // Label position (lower part of band)
            val labelX = center.x + labelRadius * cos(midAngleRad)
            val labelY = center.y + labelRadius * sin(midAngleRad)

            // Draw label
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
    label: String
) {
    // Draw a small circle as icon background
    drawCircle(
        color = CyanPrimary.copy(alpha = 0.2f),
        radius = size,
        center = center
    )
    drawCircle(
        color = CyanBorder,
        radius = size,
        center = center,
        style = Stroke(width = 1f)
    )

    // Draw first letter of label
    drawIntoCanvas { canvas ->
        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = CyanPrimary.toArgb()
            textSize = size * 1.2f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        }
        canvas.nativeCanvas.drawText(
            label.first().uppercase(),
            center.x,
            center.y + (textPaint.textSize / 3f),
            textPaint
        )
    }
}
