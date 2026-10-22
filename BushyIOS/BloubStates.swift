import Foundation
import CoreGraphics

public struct HeadGaze: Equatable {
    public var yaw: CGFloat
    public var pitch: CGFloat
    public var roll: CGFloat
    
    public init(yaw: CGFloat = 0.0, pitch: CGFloat = 0.0, roll: CGFloat = 0.0) {
        self.yaw = yaw
        self.pitch = pitch
        self.roll = roll
    }
}

public struct EyeCfg: Equatable {
    public var w: CGFloat
    public var h: CGFloat
    public var open: CGFloat
    public var tilt: CGFloat
    
    public init(w: CGFloat = 0.48, h: CGFloat = 0.50, open: CGFloat = 1.0, tilt: CGFloat = 0.0) {
        self.w = w
        self.h = h
        self.open = open
        self.tilt = tilt
    }
}

public struct BloubPose: Equatable {
    public var silName: String
    public var customRadii: [CGFloat]?
    public var offX: CGFloat
    public var offY: CGFloat
    public var gaze: HeadGaze
    public var split: CGFloat
    public var leftEye: EyeCfg
    public var rightEye: EyeCfg
    public var eyeAlpha: CGFloat
    public var bodyAlpha: CGFloat
    
    public init(
        silName: String = "cloud",
        customRadii: [CGFloat]? = nil,
        offX: CGFloat = 0.0,
        offY: CGFloat = 0.0,
        gaze: HeadGaze = HeadGaze(),
        split: CGFloat = 30.0,
        leftEye: EyeCfg = EyeCfg(),
        rightEye: EyeCfg = EyeCfg(),
        eyeAlpha: CGFloat = 1.0,
        bodyAlpha: CGFloat = 1.0
    ) {
        self.silName = silName
        self.customRadii = customRadii
        self.offX = offX
        self.offY = offY
        self.gaze = gaze
        self.split = split
        self.leftEye = leftEye
        self.rightEye = rightEye
        self.eyeAlpha = eyeAlpha
        self.bodyAlpha = bodyAlpha
    }
}

public struct BloubStates {
    public static func getPose(for expression: AvatarExpression) -> BloubPose {
        switch expression {
        case .neutral:
            return BloubPose(
                silName: "cloud",
                gaze: HeadGaze(yaw: 0, pitch: 0, roll: 0),
                split: 30,
                leftEye: EyeCfg(w: 0.48, h: 0.50),
                rightEye: EyeCfg(w: 0.48, h: 0.50)
            )
        case .happy:
            return BloubPose(
                silName: "cloud",
                gaze: HeadGaze(yaw: 0, pitch: -2, roll: 0),
                split: 30,
                leftEye: EyeCfg(w: 0.48, h: 0.40),
                rightEye: EyeCfg(w: 0.48, h: 0.40)
            )
        case .excited:
            return BloubPose(
                silName: "cloud",
                gaze: HeadGaze(yaw: 0, pitch: -4, roll: 0),
                split: 32,
                leftEye: EyeCfg(w: 0.52, h: 0.56),
                rightEye: EyeCfg(w: 0.52, h: 0.56)
            )
        case .workingOut:
            return BloubPose(
                silName: "cloud",
                gaze: HeadGaze(yaw: -4, pitch: 2, roll: 2),
                split: 30,
                leftEye: EyeCfg(w: 0.48, h: 0.50),
                rightEye: EyeCfg(w: 0.48, h: 0.50)
            )
        case .celebrating:
            return BloubPose(
                silName: "cloud",
                gaze: HeadGaze(yaw: 0, pitch: -6, roll: -3),
                split: 32,
                leftEye: EyeCfg(w: 0.52, h: 0.54),
                rightEye: EyeCfg(w: 0.52, h: 0.54)
            )
        case .tired:
            return BloubPose(
                silName: "cloud",
                gaze: HeadGaze(yaw: 0, pitch: 10, roll: 0),
                split: 30,
                leftEye: EyeCfg(w: 0.48, h: 0.15, open: 0.3),
                rightEye: EyeCfg(w: 0.48, h: 0.15, open: 0.3)
            )
        case .attentive:
            return BloubPose(
                silName: "cloud",
                gaze: HeadGaze(yaw: 0, pitch: -4, roll: 0),
                split: 30,
                leftEye: EyeCfg(w: 0.48, h: 0.54),
                rightEye: EyeCfg(w: 0.48, h: 0.54)
            )
        case .surprised:
            return BloubPose(
                silName: "cloud",
                gaze: HeadGaze(yaw: 0, pitch: -8, roll: 0),
                split: 32,
                leftEye: EyeCfg(w: 0.54, h: 0.60),
                rightEye: EyeCfg(w: 0.54, h: 0.60)
            )
        case .laughing:
            return BloubPose(
                silName: "cloud",
                gaze: HeadGaze(yaw: 4, pitch: 4, roll: -3),
                split: 30,
                leftEye: EyeCfg(w: 0.48, h: 0.18),
                rightEye: EyeCfg(w: 0.48, h: 0.18)
            )
        case .angry:
            return BloubPose(
                silName: "cloud",
                gaze: HeadGaze(yaw: -4, pitch: -8, roll: 6),
                split: 26,
                leftEye: EyeCfg(w: 0.24, h: 0.44, tilt: -10),
                rightEye: EyeCfg(w: 0.24, h: 0.16, tilt: -6)
            )
        case .thinking:
            return BloubPose(
                silName: "cloud",
                gaze: HeadGaze(yaw: -8, pitch: -10, roll: 5),
                split: 30,
                leftEye: EyeCfg(w: 0.44, h: 0.46),
                rightEye: EyeCfg(w: 0.44, h: 0.46)
            )
        }
    }
}
