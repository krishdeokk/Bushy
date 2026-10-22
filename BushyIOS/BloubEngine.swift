import SwiftUI

public struct BloubEyeRender {
    public var path: Path
    public var center: CGPoint
    public var width: CGFloat
    public var height: CGFloat
}

public struct BloubFrame {
    public var bodyPath: Path
    public var eyes: [BloubEyeRender]
    public var bodyAlpha: CGFloat
    public var eyeAlpha: CGFloat
}

public class BloubEngine {
    private var currentExpression: AvatarExpression = .neutral
    private var previousPose: BloubPose = BloubStates.getPose(for: .neutral)
    private var targetPose: BloubPose = BloubStates.getPose(for: .neutral)
    private var transitionStartTime: Double = 0.0
    private var transitionDuration: Double = 0.45
    
    // Smooth touch gaze tracking variables
    private var activeTouchYaw: CGFloat = 0.0
    private var activeTouchPitch: CGFloat = 0.0
    private var activeTouchWeight: CGFloat = 0.0
    
    public init() {}
    
    public func updateExpression(_ expression: AvatarExpression, currentTime: Double) {
        if self.currentExpression != expression {
            let currentCompositePose = samplePose(at: currentTime)
            self.previousPose = currentCompositePose
            self.targetPose = BloubStates.getPose(for: expression)
            self.currentExpression = expression
            self.transitionStartTime = currentTime
        }
    }
    
    private func samplePose(at time: Double) -> BloubPose {
        let elapsed = time - transitionStartTime
        let rawProgress = (transitionDuration > 0) ? clamp(CGFloat(elapsed / transitionDuration)) : 1.0
        let t = BloubEasings.easeOutQuint(rawProgress)
        
        let blendedGaze = HeadGaze(
            yaw: lerp(previousPose.gaze.yaw, targetPose.gaze.yaw, t),
            pitch: lerp(previousPose.gaze.pitch, targetPose.gaze.pitch, t),
            roll: lerp(previousPose.gaze.roll, targetPose.gaze.roll, t)
        )
        
        let blendedSplit = lerp(previousPose.split, targetPose.split, t)
        
        let blendedLeftEye = EyeCfg(
            w: lerp(previousPose.leftEye.w, targetPose.leftEye.w, t),
            h: lerp(previousPose.leftEye.h, targetPose.leftEye.h, t),
            open: lerp(previousPose.leftEye.open, targetPose.leftEye.open, t),
            tilt: lerp(previousPose.leftEye.tilt, targetPose.leftEye.tilt, t)
        )
        
        let blendedRightEye = EyeCfg(
            w: lerp(previousPose.rightEye.w, targetPose.rightEye.w, t),
            h: lerp(previousPose.rightEye.h, targetPose.rightEye.h, t),
            open: lerp(previousPose.rightEye.open, targetPose.rightEye.open, t),
            tilt: lerp(previousPose.rightEye.tilt, targetPose.rightEye.tilt, t)
        )
        
        return BloubPose(
            silName: targetPose.silName,
            offX: lerp(previousPose.offX, targetPose.offX, t),
            offY: lerp(previousPose.offY, targetPose.offY, t),
            gaze: blendedGaze,
            split: blendedSplit,
            leftEye: blendedLeftEye,
            rightEye: blendedRightEye,
            eyeAlpha: lerp(previousPose.eyeAlpha, targetPose.eyeAlpha, t),
            bodyAlpha: lerp(previousPose.bodyAlpha, targetPose.bodyAlpha, t)
        )
    }
    
    public func sampleFrame(
        time: Double,
        center: CGPoint,
        baseRadius: CGFloat,
        targetTouchGaze: HeadGaze? = nil
    ) -> BloubFrame {
        let pose = samplePose(at: time)
        
        // Smoothly blend touch gaze tracking
        if let touchGaze = targetTouchGaze {
            activeTouchYaw = lerp(activeTouchYaw, touchGaze.yaw, 0.30)
            activeTouchPitch = lerp(activeTouchPitch, touchGaze.pitch, 0.30)
            activeTouchWeight = lerp(activeTouchWeight, 1.0, 0.25)
        } else {
            activeTouchWeight = lerp(activeTouchWeight, 0.0, 0.18)
        }
        
        let effectiveYaw = lerp(pose.gaze.yaw, activeTouchYaw, activeTouchWeight)
        let effectivePitch = lerp(pose.gaze.pitch, activeTouchPitch, activeTouchWeight)
        
        // Organic breathing / pulsing sine wave
        let breath = 1.0 + sin(CGFloat(time / 3.4) * TAU) * 0.015
        let currentScale = baseRadius * breath
        
        // Procedural eye blinking cycle
        let blinkCycle = time.truncatingRemainder(dividingBy: 3.2)
        let blinkScale: CGFloat
        if blinkCycle >= 1.4 && blinkCycle <= 1.58 {
            let k = CGFloat((blinkCycle - 1.4) / 0.18)
            blinkScale = (k < 0.45) ? (1.0 - (k / 0.45)) : ((k - 0.45) / 0.55)
        } else {
            blinkScale = 1.0
        }
        
        let profile = BloubProfiles.getProfile(named: pose.silName)
        let sil = Silhouette(radii: profile, cx: pose.offX, cy: pose.offY)
        let points = toPoints(silhouette: sil, center: center, scale: currentScale)
        
        let bodyPath = closedPointsToPath(pts: points)
        
        // 3D Eye Gaze Shift
        let yawRad = effectiveYaw * .pi / 180.0
        let pitchRad = effectivePitch * .pi / 180.0
        let splitRad = pose.split * .pi / 180.0
        
        let eyeShiftX = sin(yawRad) * currentScale * 0.50
        let eyeShiftY = -sin(pitchRad) * currentScale * 0.55
        let eyeSeparation = sin(splitRad) * currentScale * 0.55
        
        let leftEyeX = center.x - eyeSeparation + eyeShiftX
        let rightEyeX = center.x + eyeSeparation + eyeShiftX
        let eyeY = center.y + eyeShiftY
        
        let pitchLidSquish: CGFloat = (effectivePitch < -10.0) ? (1.0 + effectivePitch / 100.0) : 1.0
        
        let leftEyeW = pose.leftEye.w * currentScale
        let leftEyeH = pose.leftEye.h * currentScale * max(pose.leftEye.open * blinkScale * pitchLidSquish, 0.10)
        
        let rightEyeW = pose.rightEye.w * currentScale
        let rightEyeH = pose.rightEye.h * currentScale * max(pose.rightEye.open * blinkScale * pitchLidSquish, 0.10)
        
        let leftTiltRad = pose.leftEye.tilt * .pi / 180.0
        let rightTiltRad = pose.rightEye.tilt * .pi / 180.0
        
        let leftEyePath = capsuleToPath(
            width: leftEyeW,
            height: leftEyeH,
            center: CGPoint(x: leftEyeX, y: eyeY),
            tiltAngleRad: leftTiltRad
        )
        
        let rightEyePath = capsuleToPath(
            width: rightEyeW,
            height: rightEyeH,
            center: CGPoint(x: rightEyeX, y: eyeY),
            tiltAngleRad: rightTiltRad
        )
        
        return BloubFrame(
            bodyPath: bodyPath,
            eyes: [
                BloubEyeRender(path: leftEyePath, center: CGPoint(x: leftEyeX, y: eyeY), width: leftEyeW, height: leftEyeH),
                BloubEyeRender(path: rightEyePath, center: CGPoint(x: rightEyeX, y: eyeY), width: rightEyeW, height: rightEyeH)
            ],
            bodyAlpha: pose.bodyAlpha,
            eyeAlpha: pose.eyeAlpha
        )
    }
}
