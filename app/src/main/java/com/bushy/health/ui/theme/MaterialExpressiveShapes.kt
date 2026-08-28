package com.bushy.health.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object MaterialExpressiveShapes {
    
    /** 4-sided cookie shape (subtle amplitude to protect content) */
    val cookie4: Shape = createScallopedShape(lobes = 4, amplitude = 0.05f)

    /** 9-sided cookie shape */
    val cookie9: Shape = createScallopedShape(lobes = 9, amplitude = 0.04f)

    /** 12-sided cookie shape */
    val cookie12: Shape = createScallopedShape(lobes = 12, amplitude = 0.035f)

    /** 4-leaf clover shape */
    val clover4: Shape = createScallopedShape(lobes = 4, amplitude = 0.06f)

    /** 8-lobed soft starburst / burst shape */
    val softBurst: Shape = createScallopedShape(lobes = 8, amplitude = 0.05f)

    /** Rounded Arch shape */
    val arch: Shape = RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 50,
        bottomEndPercent = 16,
        bottomStartPercent = 16
    )

    /** Slanted asymmetric shape */
    val slanted: Shape = RoundedCornerShape(
        topStart = 38.dp,
        topEnd = 12.dp,
        bottomEnd = 38.dp,
        bottomStart = 12.dp
    )

    /** Asymmetric squircle 1 */
    val asymmetric1: Shape = RoundedCornerShape(
        topStart = 36.dp,
        topEnd = 16.dp,
        bottomEnd = 36.dp,
        bottomStart = 24.dp
    )

    /** Asymmetric squircle 2 */
    val asymmetric2: Shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 36.dp,
        bottomEnd = 24.dp,
        bottomStart = 36.dp
    )

    /** Chat bubble user */
    val userBubble: Shape = RoundedCornerShape(
        topStart = 26.dp,
        topEnd = 26.dp,
        bottomEnd = 6.dp,
        bottomStart = 26.dp
    )

    /** Chat bubble AI */
    val aiBubble: Shape = RoundedCornerShape(
        topStart = 26.dp,
        topEnd = 26.dp,
        bottomEnd = 26.dp,
        bottomStart = 6.dp
    )

    /**
     * Generates a procedural Material 3 Expressive scalloped/polygon shape
     */
    private fun createScallopedShape(
        lobes: Int,
        amplitude: Float = 0.05f,
        tension: Float = 1f / 6f
    ): Shape {
        return GenericShape { size, _ ->
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val baseR = minOf(size.width, size.height) / 2f
            val samples = maxOf(lobes * 8, 32)
            val tau = (2f * PI).toFloat()

            val pts = List(samples) { i ->
                val angle = (i.toFloat() / samples) * tau
                val r = baseR * (1f + amplitude * cos(lobes * angle))
                Offset(
                    centerX + r * cos(angle),
                    centerY + r * sin(angle)
                )
            }

            if (pts.isNotEmpty()) {
                moveTo(pts[0].x, pts[0].y)
                val n = pts.size
                for (i in 0 until n) {
                    val p0 = pts[(i - 1 + n) % n]
                    val p1 = pts[i]
                    val p2 = pts[(i + 1) % n]
                    val p3 = pts[(i + 2) % n]

                    val c1x = p1.x + (p2.x - p0.x) * tension
                    val c1y = p1.y + (p2.y - p0.y) * tension
                    val c2x = p2.x - (p3.x - p1.x) * tension
                    val c2y = p2.y - (p3.y - p1.y) * tension

                    cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
                }
                close()
            }
        }
    }
}
