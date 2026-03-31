package com.rudy.stoic.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rudy.stoic.ui.theme.CyanPrimary

/**
 * Adds a neon glow effect behind the composable.
 *
 * @param color The glow color
 * @param radius The blur radius of the glow
 * @param intensity Number of layered glow passes (higher = stronger glow)
 */
fun Modifier.glowEffect(
    color: Color = CyanPrimary,
    radius: Dp = 12.dp,
    intensity: Int = 2
): Modifier = this.drawBehind {
    drawGlow(color, radius.toPx(), intensity)
}

private fun DrawScope.drawGlow(color: Color, radius: Float, intensity: Int) {
    drawIntoCanvas { canvas ->
        val frameworkPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
            this.color = color.toArgb()
        }
        repeat(intensity) {
            canvas.nativeCanvas.drawRect(
                0f,
                0f,
                size.width,
                size.height,
                frameworkPaint
            )
        }
    }
}

/**
 * Draws a circular glow behind the composable.
 */
fun Modifier.circularGlowEffect(
    color: Color = CyanPrimary,
    radius: Dp = 16.dp,
    intensity: Int = 2
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val frameworkPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(radius.toPx(), BlurMaskFilter.Blur.NORMAL)
            this.color = color.toArgb()
        }
        val cx = size.width / 2
        val cy = size.height / 2
        val r = minOf(cx, cy)
        repeat(intensity) {
            canvas.nativeCanvas.drawCircle(cx, cy, r, frameworkPaint)
        }
    }
}
