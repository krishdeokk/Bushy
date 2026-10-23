import SwiftUI

public struct BloubAvatar: View {
    public var expression: AvatarExpression
    public var visualStyle: VisualStyle
    public var themeMode: ThemeMode
    public var touchGaze: HeadGaze?
    public var onTap: (() -> Void)?
    public var onDoubleTap: (() -> Void)?
    
    @State private var engine = BloubEngine()
    @State private var startDate = Date()
    @State private var localDragGaze: HeadGaze? = nil
    
    public init(
        expression: AvatarExpression = .neutral,
        visualStyle: VisualStyle = .monochrome,
        themeMode: ThemeMode = .system,
        touchGaze: HeadGaze? = nil,
        onTap: (() -> Void)? = nil,
        onDoubleTap: (() -> Void)? = nil
    ) {
        self.expression = expression
        self.visualStyle = visualStyle
        self.themeMode = themeMode
        self.touchGaze = touchGaze
        self.onTap = onTap
        self.onDoubleTap = onDoubleTap
    }
    
    public var body: some View {
        TimelineView(.animation) { timeline in
            Canvas { context, size in
                let time = timeline.date.timeIntervalSince(startDate)
                engine.updateExpression(expression, currentTime: time)
                
                let center = CGPoint(x: size.width / 2.0, y: size.height / 2.0)
                let baseRadius = min(size.width, size.height) * 0.38
                let activeGaze = localDragGaze ?? touchGaze
                
                let frame = engine.sampleFrame(
                    time: time,
                    center: center,
                    baseRadius: baseRadius,
                    targetTouchGaze: activeGaze
                )
                
                // Draw Body: Pure Minimalist Monochrome
                context.fill(frame.bodyPath, with: .color(Color.primary.opacity(0.08)))
                context.stroke(
                    frame.bodyPath,
                    with: .color(Color.primary),
                    style: StrokeStyle(lineWidth: 4.5, lineCap: .round, lineJoin: .round)
                )
                
                // Draw Eyes: Pure Minimalist Monochrome
                for eye in frame.eyes {
                    context.fill(eye.path, with: .color(Color.primary.opacity(Double(frame.eyeAlpha))))
                }
            }
        }
        .contentShape(Rectangle())
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { value in
                    let dx = value.location.x - 180.0
                    let dy = value.location.y - 180.0
                    let yaw = clamp(dx / 10.0, min: -25.0, max: 25.0)
                    let pitch = clamp(dy / 10.0, min: -25.0, max: 25.0)
                    localDragGaze = HeadGaze(yaw: yaw, pitch: pitch)
                }
                .onEnded { _ in
                    localDragGaze = nil
                }
        )
        .onTapGesture(count: 2) {
            onDoubleTap?()
        }
        .onTapGesture(count: 1) {
            onTap?()
        }
    }
}

// Backwards-compatible alias
public typealias BloubAvatarView = BloubAvatar
