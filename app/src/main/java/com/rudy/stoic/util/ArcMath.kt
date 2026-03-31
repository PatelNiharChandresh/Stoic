package com.rudy.stoic.util

import androidx.compose.ui.geometry.Offset
import com.rudy.stoic.domain.model.ArcSide
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ArcMath {

    /**
     * Calculate the arc center position (off-screen to the left or right).
     *
     * LEFT: center is off-screen to the left
     * RIGHT: center is off-screen to the right
     */
    fun arcCenter(
        side: ArcSide,
        screenWidth: Float,
        screenHeight: Float,
        arcRadius: Float
    ): Offset {
        val offsetRatio = 0.3f
        return when (side) {
            ArcSide.LEFT -> Offset(
                x = -arcRadius * offsetRatio,
                y = screenHeight / 2f
            )
            ArcSide.RIGHT -> Offset(
                x = screenWidth + arcRadius * offsetRatio,
                y = screenHeight / 2f
            )
        }
    }

    /**
     * Calculate the angular range of the arc.
     *
     * LEFT arc: starts at -90 (top), sweeps 180 degrees clockwise to +90 (bottom)
     * RIGHT arc: starts at +90 (bottom), sweeps 180 degrees clockwise to +270 / -90 (top)
     */
    fun arcStartAngle(side: ArcSide): Float {
        return when (side) {
            ArcSide.LEFT -> -90f   // top, going clockwise to bottom
            ArcSide.RIGHT -> 90f   // bottom, going clockwise to top
        }
    }

    const val ARC_SWEEP = 180f

    /**
     * Calculate the position of an item along the arc given its normalized position (0.0 to 1.0).
     * normalizedPos 0.0 = start of arc, 1.0 = end of arc
     */
    fun itemPosition(
        normalizedPos: Float,
        arcCenter: Offset,
        arcRadius: Float,
        side: ArcSide
    ): Offset {
        val startAngle = arcStartAngle(side)
        val angle = startAngle + (normalizedPos * ARC_SWEEP)
        val angleRad = angle * PI.toFloat() / 180f
        return Offset(
            x = arcCenter.x + arcRadius * cos(angleRad),
            y = arcCenter.y + arcRadius * sin(angleRad)
        )
    }

    /**
     * Calculate rotation angle for tangent-aligned text/icon at a given position on the arc.
     * This makes items "follow" the curve.
     */
    fun tangentRotation(normalizedPos: Float, side: ArcSide): Float {
        val startAngle = arcStartAngle(side)
        val angle = startAngle + (normalizedPos * ARC_SWEEP)
        return when (side) {
            ArcSide.LEFT -> angle + 90f  // perpendicular to radius, reading left-to-right
            ArcSide.RIGHT -> angle - 90f
        }
    }

    /**
     * How many items are visible on the arc at once (rough estimate).
     */
    fun visibleItemCount(arcRadius: Float, itemSpacing: Float): Int {
        val arcLength = PI.toFloat() * arcRadius // half circumference
        return (arcLength / itemSpacing).toInt().coerceAtLeast(1)
    }

    /**
     * Maps a vertical scroll delta to an angular scroll delta.
     */
    fun verticalDragToAngle(
        dragDeltaY: Float,
        screenHeight: Float
    ): Float {
        // Full screen drag = full 180 degree arc
        return (dragDeltaY / screenHeight) * ARC_SWEEP
    }
}
