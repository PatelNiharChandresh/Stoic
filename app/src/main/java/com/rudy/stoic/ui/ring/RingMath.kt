package com.rudy.stoic.ui.ring

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object RingMath {

    const val GAP_ANGLE_DEGREES = 4f
    private const val START_OFFSET = -90f // Start from top (12 o'clock)

    /**
     * Calculates the start angle for a block at the given index.
     * Items are evenly distributed around 360 degrees with gaps between them.
     */
    fun blockStartAngle(index: Int, itemCount: Int): Float {
        val anglePerItem = 360f / itemCount
        return START_OFFSET + (index * anglePerItem) + (GAP_ANGLE_DEGREES / 2f)
    }

    /**
     * Calculates the sweep angle for each block (how many degrees it spans).
     */
    fun blockSweepAngle(itemCount: Int): Float {
        val anglePerItem = 360f / itemCount
        return anglePerItem - GAP_ANGLE_DEGREES
    }

    /**
     * Calculates the midpoint angle of a block (used for icon/label positioning).
     */
    fun blockMidAngle(index: Int, itemCount: Int): Float {
        val anglePerItem = 360f / itemCount
        return START_OFFSET + (index * anglePerItem) + (anglePerItem / 2f)
    }

    /**
     * Converts an angle in degrees to a point on a circle at the given radius from center.
     */
    fun angleToOffset(angleDegrees: Float, radius: Float, center: Offset): Offset {
        val angleRad = angleDegrees * PI.toFloat() / 180f
        return Offset(
            x = center.x + radius * cos(angleRad),
            y = center.y + radius * sin(angleRad)
        )
    }

    /**
     * Converts a screen tap position to polar coordinates (distance from center, angle in degrees).
     * Returns Pair(distance, angleDegrees) where angle is 0-360 measured clockwise from top.
     */
    fun toPolar(point: Offset, center: Offset): Pair<Float, Float> {
        val dx = point.x - center.x
        val dy = point.y - center.y
        val distance = sqrt(dx * dx + dy * dy)
        // atan2 gives angle from positive X axis, we want from top (negative Y axis)
        var angleDeg = atan2(dy, dx) * 180f / PI.toFloat()
        // Normalize to 0-360
        angleDeg = ((angleDeg + 360f) % 360f)
        return Pair(distance, angleDeg)
    }

    /**
     * Determines which ring block index (if any) was tapped.
     * Returns the block index or -1 if the tap is not on any block.
     */
    fun hitTestBlock(
        point: Offset,
        center: Offset,
        innerRadius: Float,
        outerRadius: Float,
        itemCount: Int
    ): Int {
        val (distance, rawAngle) = toPolar(point, center)

        // Check if tap is within the ring band
        if (distance < innerRadius || distance > outerRadius) return -1

        // Normalize angle relative to the start offset
        // rawAngle is from atan2 (0 = right, clockwise)
        // We need to check against our block angles which use START_OFFSET (-90)
        for (i in 0 until itemCount) {
            val start = blockStartAngle(i, itemCount)
            val sweep = blockSweepAngle(itemCount)
            val end = start + sweep

            // Normalize all angles to 0-360 for comparison
            val normStart = ((start + 360f) % 360f)
            val normEnd = ((end + 360f) % 360f)

            val inBlock = if (normStart <= normEnd) {
                rawAngle in normStart..normEnd
            } else {
                // Block wraps around 0/360 boundary
                rawAngle >= normStart || rawAngle <= normEnd
            }

            if (inBlock) return i
        }

        return -1
    }
}
