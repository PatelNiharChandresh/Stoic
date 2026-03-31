package com.rudy.stoic.ui.ring

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import com.rudy.stoic.domain.model.GestureDirection
import kotlin.math.abs

private const val MIN_SWIPE_DISTANCE = 50f
private const val DIRECTION_THRESHOLD = 1.2f // dx/dy ratio to determine dominant axis

suspend fun PointerInputScope.detectCenterSwipe(
    center: Offset,
    innerRadius: Float,
    onSwipe: (GestureDirection) -> Unit
) {
    awaitPointerEventScope {
        while (true) {
            // Wait for finger down
            val down = awaitPointerEvent().changes.firstOrNull() ?: continue
            val downPos = down.position

            // Check if touch is inside the inner circle (center zone)
            val dx = downPos.x - center.x
            val dy = downPos.y - center.y
            val distSq = dx * dx + dy * dy
            if (distSq > innerRadius * innerRadius) continue

            down.consume()

            var lastPos = downPos
            var totalDx = 0f
            var totalDy = 0f
            var consumed = false

            // Track movement until finger is lifted
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break

                if (!change.pressed) {
                    // Finger lifted — evaluate the swipe
                    totalDx = change.position.x - downPos.x
                    totalDy = change.position.y - downPos.y

                    val absDx = abs(totalDx)
                    val absDy = abs(totalDy)
                    val totalDist = maxOf(absDx, absDy)

                    if (totalDist >= MIN_SWIPE_DISTANCE) {
                        val direction = if (absDy > absDx * DIRECTION_THRESHOLD) {
                            // Vertical dominant
                            if (totalDy > 0) GestureDirection.DOWN else null // Only down, not up
                        } else if (absDx > absDy * DIRECTION_THRESHOLD) {
                            // Horizontal dominant
                            if (totalDx < 0) GestureDirection.LEFT else GestureDirection.RIGHT
                        } else {
                            null // Diagonal — ambiguous, ignore
                        }

                        if (direction != null) {
                            onSwipe(direction)
                        }
                    }
                    change.consume()
                    break
                }

                change.consume()
                lastPos = change.position
            }
        }
    }
}
