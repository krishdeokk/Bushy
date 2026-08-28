package com.bushy.health.ui.bloub

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

const val TAU = (2.0 * PI).toFloat()
const val PROFILE_SAMPLES = 64

data class BloubPoint(var x: Float = 0f, var y: Float = 0f)

fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

fun clamp(value: Float, min: Float = 0f, max: Float = 1f): Float = value.coerceIn(min, max)

object BloubEasings {
    fun easeOutQuint(t: Float): Float {
        val clamped = clamp(t)
        val inv = 1f - clamped
        return 1f - inv * inv * inv * inv * inv
    }

    fun easeOutCubic(t: Float): Float {
        val clamped = clamp(t)
        val inv = 1f - clamped
        return 1f - inv * inv * inv
    }
}

val ANGLES: FloatArray = FloatArray(PROFILE_SAMPLES) { i -> (i.toFloat() / PROFILE_SAMPLES) * TAU }
val COS_ANGLES: FloatArray = FloatArray(PROFILE_SAMPLES) { i -> cos(ANGLES[i]) }
val SIN_ANGLES: FloatArray = FloatArray(PROFILE_SAMPLES) { i -> sin(ANGLES[i]) }

data class Silhouette(
    val radii: FloatArray = FloatArray(PROFILE_SAMPLES) { 1f },
    var rot: Float = 0f,
    var cx: Float = 0f,
    var cy: Float = 0f,
    var sx: Float = 1f,
    var sy: Float = 1f
)

fun circleSilhouette(radius: Float): Silhouette {
    return Silhouette(radii = FloatArray(PROFILE_SAMPLES) { radius })
}

fun blendSilhouette(a: Silhouette, b: Silhouette, t: Float): Silhouette {
    val dstRadii = FloatArray(PROFILE_SAMPLES)
    for (i in 0 until PROFILE_SAMPLES) {
        val rA = if (i < a.radii.size) a.radii[i] else 1f
        val rB = if (i < b.radii.size) b.radii[i] else 1f
        dstRadii[i] = lerp(rA, rB, t)
    }

    var dRot = b.rot - a.rot
    while (dRot > PI.toFloat()) dRot -= TAU
    while (dRot < -PI.toFloat()) dRot += TAU

    return Silhouette(
        radii = dstRadii,
        rot = a.rot + dRot * t,
        cx = lerp(a.cx, b.cx, t),
        cy = lerp(a.cy, b.cy, t),
        sx = lerp(a.sx, b.sx, t),
        sy = lerp(a.sy, b.sy, t)
    )
}

fun toPoints(s: Silhouette, scale: Float, out: MutableList<BloubPoint>): List<BloubPoint> {
    val cr = cos(s.rot)
    val sr = sin(s.rot)

    while (out.size < PROFILE_SAMPLES) {
        out.add(BloubPoint())
    }

    for (i in 0 until PROFILE_SAMPLES) {
        val r = if (i < s.radii.size) s.radii[i] else 1f
        val x = r * COS_ANGLES[i]
        val y = r * SIN_ANGLES[i]

        val rx = x * cr - y * sr
        val ry = x * sr + y * cr

        val p = out[i]
        p.x = (rx * s.sx + s.cx) * scale
        p.y = (ry * s.sy + s.cy) * scale
    }

    return out
}

fun closedPointsToComposePath(
    pts: List<BloubPoint>,
    centerOffset: Offset,
    scaleMultiplier: Float = 1f,
    tension: Float = 1f / 6f,
    targetPath: Path = Path()
): Path {
    targetPath.reset()
    val n = pts.size
    if (n < 3) return targetPath

    val first = pts[0]
    targetPath.moveTo(
        centerOffset.x + first.x * scaleMultiplier,
        centerOffset.y + first.y * scaleMultiplier
    )

    for (i in 0 until n) {
        val p0 = pts[(i - 1 + n) % n]
        val p1 = pts[i]
        val p2 = pts[(i + 1) % n]
        val p3 = pts[(i + 2) % n]

        val c1x = p1.x + (p2.x - p0.x) * tension
        val c1y = p1.y + (p2.y - p0.y) * tension
        val c2x = p2.x - (p3.x - p1.x) * tension
        val c2y = p2.y - (p3.y - p1.y) * tension

        targetPath.cubicTo(
            centerOffset.x + c1x * scaleMultiplier,
            centerOffset.y + c1y * scaleMultiplier,
            centerOffset.x + c2x * scaleMultiplier,
            centerOffset.y + c2y * scaleMultiplier,
            centerOffset.x + p2.x * scaleMultiplier,
            centerOffset.y + p2.y * scaleMultiplier
        )
    }

    targetPath.close()
    return targetPath
}

fun capsuleToComposePath(
    width: Float,
    height: Float,
    center: Offset,
    targetPath: Path = Path()
): Path {
    targetPath.reset()
    val w = maxOf(width, 0.01f)
    val h = maxOf(height, 0.01f)
    val hw = w / 2f
    val hh = h / 2f
    val r = minOf(hw, hh)

    val rect = Rect(
        left = center.x - hw,
        top = center.y - hh,
        right = center.x + hw,
        bottom = center.y + hh
    )
    val roundRect = RoundRect(
        rect = rect,
        cornerRadius = CornerRadius(r, r)
    )
    targetPath.addRoundRect(roundRect)
    return targetPath
}

fun superellipseProfile(n: Float, sx: Float = 1f, sy: Float = 1f): FloatArray {
    val result = FloatArray(PROFILE_SAMPLES)
    for (i in 0 until PROFILE_SAMPLES) {
        val c = Math.pow(abs(COS_ANGLES[i] / sx).toDouble(), n.toDouble()).toFloat()
        val s = Math.pow(abs(SIN_ANGLES[i] / sy).toDouble(), n.toDouble()).toFloat()
        result[i] = Math.pow((c + s).toDouble(), (-1f / n).toDouble()).toFloat()
    }
    return result
}
