package com.bushy.health.ui.bloub

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.bushy.health.AvatarExpression
import kotlin.math.cos
import kotlin.math.sin

data class BloubEyeRender(
    val path: Path = Path(),
    val center: Offset = Offset.Zero,
    val width: Float = 0f,
    val height: Float = 0f
)

data class BloubFrame(
    val bodyPath: Path = Path(),
    val eyes: List<BloubEyeRender> = emptyList(),
    val bodyAlpha: Float = 1f,
    val eyeAlpha: Float = 1f
)

class BloubEngine {
    private var currentExpression: AvatarExpression = AvatarExpression.NEUTRAL
    private var previousPose: BloubPose = BloubStates.getPoseForExpression(AvatarExpression.NEUTRAL)
    private var targetPose: BloubPose = BloubStates.getPoseForExpression(AvatarExpression.NEUTRAL)
    private var transitionStartTime: Float = 0f
    private var transitionDuration: Float = 0.45f

    // Smooth touch gaze tracking variables
    private var activeTouchYaw: Float = 0f
    private var activeTouchPitch: Float = 0f
    private var activeTouchWeight: Float = 0f

    private val cachedPoints: MutableList<BloubPoint> = MutableList(PROFILE_SAMPLES) { BloubPoint() }
    private val cachedBodyPath = Path()
    private val cachedLeftEyePath = Path()
    private val cachedRightEyePath = Path()

    fun updateExpression(expression: AvatarExpression, currentTimeInSeconds: Float) {
        if (this.currentExpression != expression) {
            val currentCompositePose = samplePoseAt(currentTimeInSeconds)
            this.previousPose = currentCompositePose
            this.targetPose = BloubStates.getPoseForExpression(expression)
            this.currentExpression = expression
            this.transitionStartTime = currentTimeInSeconds
        }
    }

    private fun samplePoseAt(timeInSeconds: Float): BloubPose {
        val elapsed = timeInSeconds - transitionStartTime
        val rawProgress = if (transitionDuration > 0f) clamp(elapsed / transitionDuration) else 1f
        val t = BloubEasings.easeOutQuint(rawProgress)

        val blendedGaze = HeadGaze(
            yaw = lerp(previousPose.gaze.yaw, targetPose.gaze.yaw, t),
            pitch = lerp(previousPose.gaze.pitch, targetPose.gaze.pitch, t),
            roll = lerp(previousPose.gaze.roll, targetPose.gaze.roll, t)
        )

        val blendedSplit = lerp(previousPose.split, targetPose.split, t)

        val blendedLeftEye = EyeCfg(
            w = lerp(previousPose.leftEye.w, targetPose.leftEye.w, t),
            h = lerp(previousPose.leftEye.h, targetPose.leftEye.h, t),
            open = lerp(previousPose.leftEye.open, targetPose.leftEye.open, t),
            tilt = lerp(previousPose.leftEye.tilt, targetPose.leftEye.tilt, t)
        )

        val blendedRightEye = EyeCfg(
            w = lerp(previousPose.rightEye.w, targetPose.rightEye.w, t),
            h = lerp(previousPose.rightEye.h, targetPose.rightEye.h, t),
            open = lerp(previousPose.rightEye.open, targetPose.rightEye.open, t),
            tilt = lerp(previousPose.rightEye.tilt, targetPose.rightEye.tilt, t)
        )

        return BloubPose(
            silName = targetPose.silName,
            offX = lerp(previousPose.offX, targetPose.offX, t),
            offY = lerp(previousPose.offY, targetPose.offY, t),
            gaze = blendedGaze,
            split = blendedSplit,
            leftEye = blendedLeftEye,
            rightEye = blendedRightEye,
            eyeAlpha = lerp(previousPose.eyeAlpha, targetPose.eyeAlpha, t),
            bodyAlpha = lerp(previousPose.bodyAlpha, targetPose.bodyAlpha, t)
        )
    }

    fun sampleFrame(
        timeInSeconds: Float,
        center: Offset,
        baseRadius: Float,
        targetTouchGaze: HeadGaze? = null
    ): BloubFrame {
        val pose = samplePoseAt(timeInSeconds)

        // Smoothly blend touch gaze tracking
        if (targetTouchGaze != null) {
            activeTouchYaw = lerp(activeTouchYaw, targetTouchGaze.yaw, 0.30f)
            activeTouchPitch = lerp(activeTouchPitch, targetTouchGaze.pitch, 0.30f)
            activeTouchWeight = lerp(activeTouchWeight, 1f, 0.25f)
        } else {
            activeTouchWeight = lerp(activeTouchWeight, 0f, 0.18f)
        }

        val effectiveYaw = lerp(pose.gaze.yaw, activeTouchYaw, activeTouchWeight)
        val effectivePitch = lerp(pose.gaze.pitch, activeTouchPitch, activeTouchWeight)

        // Subtle organic breathing / pulsing
        val breath = 1f + sin((timeInSeconds / 3.4f) * TAU) * 0.012f
        val currentScale = baseRadius * breath

        // Subtle eye blinking schedule
        val blinkCycle = (timeInSeconds % 3.2f)
        val blinkScale = if (blinkCycle in 1.4f..1.58f) {
            val k = (blinkCycle - 1.4f) / 0.18f
            if (k < 0.45f) 1f - (k / 0.45f) else (k - 0.45f) / 0.55f
        } else {
            1f
        }

        // Generate cloud silhouette
        val sil = when (pose.silName) {
            "egg" -> Silhouette(BloubProfiles.egg, cx = pose.offX, cy = pose.offY)
            "hexagon" -> Silhouette(BloubProfiles.hexagon, cx = pose.offX, cy = pose.offY)
            "triangle" -> Silhouette(BloubProfiles.triangle, cx = pose.offX, cy = pose.offY)
            else -> Silhouette(BloubProfiles.cloud, cx = pose.offX, cy = pose.offY)
        }

        val points = toPoints(sil, currentScale, cachedPoints)
        val bodyPath = closedPointsToComposePath(
            pts = points,
            centerOffset = center,
            targetPath = cachedBodyPath
        )

        // Pronounced 3D eye gaze shift
        val yawRad = (effectiveYaw * Math.PI / 180.0).toFloat()
        val pitchRad = (effectivePitch * Math.PI / 180.0).toFloat()
        val splitRad = (pose.split * Math.PI / 180.0).toFloat()

        // Responsive eye shifts
        val eyeShiftX = sin(yawRad) * currentScale * 0.50f
        val eyeShiftY = -sin(pitchRad) * currentScale * 0.55f

        val eyeSeparation = sin(splitRad) * currentScale * 0.55f

        val leftEyeX = center.x - eyeSeparation + eyeShiftX
        val rightEyeX = center.x + eyeSeparation + eyeShiftX
        val eyeY = center.y + eyeShiftY

        // Slight lid squeeze when looking down steep pitch
        val pitchLidSquish = if (effectivePitch < -10f) {
            1f + (effectivePitch / 100f)
        } else {
            1f
        }

        val leftEyeW = pose.leftEye.w * currentScale
        val leftEyeH = pose.leftEye.h * currentScale * maxOf(pose.leftEye.open * blinkScale * pitchLidSquish, 0.10f)

        val rightEyeW = pose.rightEye.w * currentScale
        val rightEyeH = pose.rightEye.h * currentScale * maxOf(pose.rightEye.open * blinkScale * pitchLidSquish, 0.10f)

        val leftEyePath = capsuleToComposePath(
            width = leftEyeW,
            height = leftEyeH,
            center = Offset(leftEyeX, eyeY),
            targetPath = cachedLeftEyePath
        )

        val rightEyePath = capsuleToComposePath(
            width = rightEyeW,
            height = rightEyeH,
            center = Offset(rightEyeX, eyeY),
            targetPath = cachedRightEyePath
        )

        return BloubFrame(
            bodyPath = bodyPath,
            eyes = listOf(
                BloubEyeRender(path = leftEyePath, center = Offset(leftEyeX, eyeY), width = leftEyeW, height = leftEyeH),
                BloubEyeRender(path = rightEyePath, center = Offset(rightEyeX, eyeY), width = rightEyeW, height = rightEyeH)
            ),
            bodyAlpha = pose.bodyAlpha,
            eyeAlpha = pose.eyeAlpha
        )
    }
}
