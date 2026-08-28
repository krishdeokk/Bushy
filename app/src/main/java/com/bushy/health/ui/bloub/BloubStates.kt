package com.bushy.health.ui.bloub

import com.bushy.health.AvatarExpression

data class HeadGaze(
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f
)

data class EyeCfg(
    val w: Float = 0.48f,
    val h: Float = 0.50f,
    val open: Float = 1f,
    val tilt: Float = 0f
)

data class BloubPose(
    val silName: String = "cloud",
    val customRadii: FloatArray? = null,
    val offX: Float = 0f,
    val offY: Float = 0f,
    val gaze: HeadGaze = HeadGaze(),
    val split: Float = 30f,
    val leftEye: EyeCfg = EyeCfg(),
    val rightEye: EyeCfg = EyeCfg(),
    val eyeAlpha: Float = 1f,
    val bodyAlpha: Float = 1f
)

object BloubStates {
    fun getPoseForExpression(expression: AvatarExpression): BloubPose {
        return when (expression) {
            AvatarExpression.NEUTRAL -> BloubPose(
                silName = "cloud",
                gaze = HeadGaze(0f, 0f, 0f),
                split = 30f,
                leftEye = EyeCfg(0.48f, 0.50f),
                rightEye = EyeCfg(0.48f, 0.50f)
            )
            AvatarExpression.HAPPY -> BloubPose(
                silName = "cloud",
                gaze = HeadGaze(0f, -2f, 0f),
                split = 30f,
                leftEye = EyeCfg(0.48f, 0.40f),
                rightEye = EyeCfg(0.48f, 0.40f)
            )
            AvatarExpression.EXCITED -> BloubPose(
                silName = "cloud",
                gaze = HeadGaze(0f, -4f, 0f),
                split = 32f,
                leftEye = EyeCfg(0.52f, 0.56f),
                rightEye = EyeCfg(0.52f, 0.56f)
            )
            AvatarExpression.WORKING_OUT -> BloubPose(
                silName = "cloud",
                gaze = HeadGaze(-4f, 2f, 2f),
                split = 30f,
                leftEye = EyeCfg(0.48f, 0.50f),
                rightEye = EyeCfg(0.48f, 0.50f)
            )
            AvatarExpression.CELEBRATING -> BloubPose(
                silName = "cloud",
                gaze = HeadGaze(0f, -6f, -3f),
                split = 32f,
                leftEye = EyeCfg(0.52f, 0.54f),
                rightEye = EyeCfg(0.52f, 0.54f)
            )
            AvatarExpression.TIRED -> BloubPose(
                silName = "cloud",
                gaze = HeadGaze(0f, 10f, 0f),
                split = 30f,
                leftEye = EyeCfg(0.48f, 0.15f, open = 0.3f),
                rightEye = EyeCfg(0.48f, 0.15f, open = 0.3f)
            )
            AvatarExpression.ATTENTIVE -> BloubPose(
                silName = "cloud",
                gaze = HeadGaze(0f, -4f, 0f),
                split = 30f,
                leftEye = EyeCfg(0.48f, 0.54f),
                rightEye = EyeCfg(0.48f, 0.54f)
            )
            AvatarExpression.SURPRISED -> BloubPose(
                silName = "cloud",
                gaze = HeadGaze(0f, -8f, 0f),
                split = 32f,
                leftEye = EyeCfg(0.54f, 0.60f),
                rightEye = EyeCfg(0.54f, 0.60f)
            )
            AvatarExpression.LAUGHING -> BloubPose(
                silName = "cloud",
                gaze = HeadGaze(4f, 4f, -3f),
                split = 30f,
                leftEye = EyeCfg(0.48f, 0.18f),
                rightEye = EyeCfg(0.48f, 0.18f)
            )
            AvatarExpression.ANGRY -> BloubPose(
                silName = "cloud",
                gaze = HeadGaze(-4f, -8f, 6f),
                split = 26f,
                leftEye = EyeCfg(0.24f, 0.44f, tilt = -10f),
                rightEye = EyeCfg(0.24f, 0.16f, tilt = -6f)
            )
            AvatarExpression.THINKING -> BloubPose(
                silName = "cloud",
                gaze = HeadGaze(-8f, -10f, 5f),
                split = 30f,
                leftEye = EyeCfg(0.44f, 0.46f),
                rightEye = EyeCfg(0.44f, 0.46f)
            )
        }
    }
}
