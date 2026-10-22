import SwiftUI
import Foundation

// MARK: - Radial Constants

public let SAMPLES = 64
public let STEP_RAD = (2.0 * Double.pi) / Double(SAMPLES)
public let TAU: CGFloat = 2.0 * .pi

public let ANGLES: [Double] = (0..<SAMPLES).map { Double($0) * STEP_RAD }
public let COS_ANGLES: [Double] = ANGLES.map { Foundation.cos($0) }
public let SIN_ANGLES: [Double] = ANGLES.map { Foundation.sin($0) }

// MARK: - Silhouette Data Structure

public struct Silhouette {
    public var radii: [Double]
    public var cx: CGFloat = 0.0
    public var cy: CGFloat = 0.0
    
    public init(radii: [Double] = Array(repeating: 1.0, count: SAMPLES), cx: CGFloat = 0.0, cy: CGFloat = 0.0) {
        self.radii = radii
        self.cx = cx
        self.cy = cy
    }
    
    public init(radii: [CGFloat], cx: CGFloat = 0.0, cy: CGFloat = 0.0) {
        self.radii = radii.map { Double($0) }
        self.cx = cx
        self.cy = cy
    }
    
    public static var zero: Silhouette {
        Silhouette(radii: Array(repeating: 0.0, count: SAMPLES))
    }
}

// MARK: - Clamping & Lerping

public func clamp(_ val: CGFloat, min: CGFloat, max: CGFloat) -> CGFloat {
    Swift.min(Swift.max(val, min), max)
}

public func clamp(_ val: CGFloat, _ minVal: CGFloat = 0.0, _ maxVal: CGFloat = 1.0) -> CGFloat {
    Swift.min(Swift.max(val, minVal), maxVal)
}

public func lerp(_ a: CGFloat, _ b: CGFloat, _ t: CGFloat) -> CGFloat {
    a + (b - a) * t
}

public func lerp(_ a: Double, _ b: Double, _ t: Double) -> Double {
    a + (b - a) * t
}

public func lerpDouble(_ a: Double, _ b: Double, _ t: Double) -> Double {
    a + (b - a) * t
}

public func lerpCGFloat(_ a: CGFloat, _ b: CGFloat, _ t: CGFloat) -> CGFloat {
    a + (b - a) * t
}

public func lerpPoint(_ a: CGPoint, _ b: CGPoint, _ t: CGFloat) -> CGPoint {
    CGPoint(
        x: a.x + (b.x - a.x) * t,
        y: a.y + (b.y - a.y) * t
    )
}

// MARK: - Easing Functions

public func quinticEaseOut(_ t: Double) -> Double {
    let clamped = Swift.min(Swift.max(t, 0.0), 1.0)
    let inv = 1.0 - clamped
    return 1.0 - (inv * inv * inv * inv * inv)
}

public func cubicEaseOut(_ t: Double) -> Double {
    let clamped = Swift.min(Swift.max(t, 0.0), 1.0)
    let inv = 1.0 - clamped
    return 1.0 - (inv * inv * inv)
}

public struct BloubEasings {
    public static func easeOutQuint(_ t: CGFloat) -> CGFloat {
        CGFloat(quinticEaseOut(Double(t)))
    }
}

// MARK: - Catmull-Rom Spline Interpolation

public func catmullRomSplineClosed(points: [CGPoint]) -> Path {
    var path = Path()
    let n = points.count
    guard n >= 3 else { return path }
    
    path.move(to: points[0])
    
    for i in 0..<n {
        let p0 = points[(i - 1 + n) % n]
        let p1 = points[i]
        let p2 = points[(i + 1) % n]
        let p3 = points[(i + 2) % n]
        
        let c1x = p1.x + (p2.x - p0.x) / 6.0
        let c1y = p1.y + (p2.y - p0.y) / 6.0
        let c2x = p2.x - (p3.x - p1.x) / 6.0
        let c2y = p2.y - (p3.y - p1.y) / 6.0
        
        path.addCurve(
            to: p2,
            control1: CGPoint(x: c1x, y: c1y),
            control2: CGPoint(x: c2x, y: c2y)
        )
    }
    path.closeSubpath()
    return path
}

public func closedPointsToPath(pts: [CGPoint], center: CGPoint = .zero, scaleMultiplier: CGFloat = 1.0) -> Path {
    if center == .zero && scaleMultiplier == 1.0 {
        return catmullRomSplineClosed(points: pts)
    }
    let scaled = pts.map { pt in
        CGPoint(x: center.x + (pt.x - center.x) * scaleMultiplier, y: center.y + (pt.y - center.y) * scaleMultiplier)
    }
    return catmullRomSplineClosed(points: scaled)
}

// MARK: - Silhouette Point Sampling

public func silhouetteToPoints(
    silhouette: Silhouette,
    center: CGPoint,
    radius: CGFloat,
    aspectRatio: Double = 1.0
) -> [CGPoint] {
    var points = [CGPoint]()
    points.reserveCapacity(SAMPLES)
    
    let radD = Double(radius)
    let cx = Double(center.x)
    let cy = Double(center.y)
    
    for i in 0..<SAMPLES {
        let r = silhouette.radii[i] * radD
        let x = cx + r * COS_ANGLES[i]
        let y = cy + (r * SIN_ANGLES[i]) * aspectRatio
        points.append(CGPoint(x: x, y: y))
    }
    
    return points
}

public func toPoints(silhouette: Silhouette, center: CGPoint = .zero, scale: CGFloat) -> [CGPoint] {
    silhouetteToPoints(
        silhouette: silhouette,
        center: CGPoint(x: center.x + silhouette.cx * scale, y: center.y + silhouette.cy * scale),
        radius: scale
    )
}

// MARK: - Silhouette Interpolation

public func lerpSilhouette(
    from a: Silhouette,
    to b: Silhouette,
    t: Double
) -> Silhouette {
    var result = [Double](repeating: 0.0, count: SAMPLES)
    for i in 0..<SAMPLES {
        result[i] = a.radii[i] + (b.radii[i] - a.radii[i]) * t
    }
    return Silhouette(radii: result)
}

// MARK: - Tilted Capsule Path

public func createTiltedCapsule(
    center: CGPoint,
    width: CGFloat,
    height: CGFloat,
    tiltAngleRad: CGFloat
) -> Path {
    let w = Swift.max(width, 0.5)
    let h = Swift.max(height, 0.5)
    let r = Swift.min(w, h) / 2.0
    let rect = CGRect(x: center.x - w / 2, y: center.y - h / 2, width: w, height: h)
    
    var path = Path(roundedRect: rect, cornerRadius: r)
    if tiltAngleRad != 0.0 {
        let transform = CGAffineTransform.identity
            .translatedBy(x: center.x, y: center.y)
            .rotated(by: tiltAngleRad)
            .translatedBy(x: -center.x, y: -center.y)
        path = path.applying(transform)
    }
    return path
}

public func capsuleToPath(
    width: CGFloat,
    height: CGFloat,
    center: CGPoint,
    tiltAngleRad: CGFloat
) -> Path {
    createTiltedCapsule(center: center, width: width, height: height, tiltAngleRad: tiltAngleRad)
}
