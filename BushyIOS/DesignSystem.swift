import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

public enum BushyHapticStyle {
    case light, medium, heavy
}

public enum BushyHapticNotification {
    case success, warning, error
}

public struct HapticManager {
    public static func impact(style: BushyHapticStyle = .medium) {
        #if os(iOS)
        let feedbackStyle: UIImpactFeedbackGenerator.FeedbackStyle
        switch style {
        case .light: feedbackStyle = .light
        case .medium: feedbackStyle = .medium
        case .heavy: feedbackStyle = .heavy
        }
        let impact = UIImpactFeedbackGenerator(style: feedbackStyle)
        impact.impactOccurred()
        #endif
    }
    
    public static func notify(type: BushyHapticNotification = .success) {
        #if os(iOS)
        let feedbackType: UINotificationFeedbackGenerator.FeedbackType
        switch type {
        case .success: feedbackType = .success
        case .warning: feedbackType = .warning
        case .error: feedbackType = .error
        }
        let notification = UINotificationFeedbackGenerator()
        notification.notificationOccurred(feedbackType)
        #endif
    }
}

// MARK: - Liquid Glass Elements

public struct LiquidGlassCardModifier: ViewModifier {
    var cornerRadius: CGFloat
    var isInteractive: Bool
    
    public func body(content: Content) -> some View {
        content
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(.ultraThinMaterial)
                    .shadow(color: Color.black.opacity(0.08), radius: 16, x: 0, y: 8)
            )
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(0.48),
                                Color.white.opacity(0.12),
                                Color.black.opacity(0.04),
                                Color.white.opacity(0.20)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.2
                    )
            )
    }
}

public struct LiquidGlassPillModifier: ViewModifier {
    public func body(content: Content) -> some View {
        content
            .background(
                Capsule(style: .continuous)
                    .fill(.ultraThinMaterial)
                    .shadow(color: Color.black.opacity(0.12), radius: 14, x: 0, y: 6)
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(0.60),
                                Color.white.opacity(0.15),
                                Color.black.opacity(0.05),
                                Color.white.opacity(0.30)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.2
                    )
            )
    }
}

public struct LiquidGlassCircleModifier: ViewModifier {
    public func body(content: Content) -> some View {
        content
            .background(
                Circle()
                    .fill(.ultraThinMaterial)
                    .shadow(color: Color.black.opacity(0.10), radius: 12, x: 0, y: 6)
            )
            .overlay(
                Circle()
                    .stroke(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(0.60),
                                Color.white.opacity(0.15),
                                Color.clear,
                                Color.white.opacity(0.25)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.2
                    )
            )
    }
}

// Ambient Liquid Fluid Background
public struct LiquidFluidBackground: View {
    var isMonochrome: Bool
    
    @State private var animateBlobs: Bool = false
    
    public init(isMonochrome: Bool = false) {
        self.isMonochrome = isMonochrome
    }
    
    public var body: some View {
        GeometryReader { proxy in
            let w = proxy.size.width
            let h = proxy.size.height
            
            ZStack {
                // Base background tone
                Color.bushyAppBackground
                    .ignoresSafeArea()
                
                // Fluid Blob 1 (Top Left)
                Circle()
                    .fill(
                        isMonochrome ?
                        Color.white.opacity(0.06) :
                        Color.blue.opacity(0.22)
                    )
                    .frame(width: w * 0.85, height: w * 0.85)
                    .blur(radius: 65)
                    .offset(x: animateBlobs ? -w * 0.15 : -w * 0.35, y: animateBlobs ? -h * 0.10 : -h * 0.20)
                
                // Fluid Blob 2 (Mid Right)
                Circle()
                    .fill(
                        isMonochrome ?
                        Color.white.opacity(0.04) :
                        Color.purple.opacity(0.20)
                    )
                    .frame(width: w * 0.90, height: w * 0.90)
                    .blur(radius: 75)
                    .offset(x: animateBlobs ? w * 0.20 : w * 0.35, y: animateBlobs ? h * 0.10 : h * 0.0)
                
                // Fluid Blob 3 (Bottom Center)
                Circle()
                    .fill(
                        isMonochrome ?
                        Color.white.opacity(0.05) :
                        Color.cyan.opacity(0.18)
                    )
                    .frame(width: w * 0.80, height: w * 0.80)
                    .blur(radius: 70)
                    .offset(x: animateBlobs ? -w * 0.10 : w * 0.10, y: animateBlobs ? h * 0.35 : h * 0.25)
            }
            .onAppear {
                withAnimation(.easeInOut(duration: 8.0).repeatForever(autoreverses: true)) {
                    animateBlobs.toggle()
                }
            }
        }
        .ignoresSafeArea()
    }
}

extension View {
    public func liquidGlassCard(cornerRadius: CGFloat = 22, isInteractive: Bool = false) -> some View {
        self.modifier(LiquidGlassCardModifier(cornerRadius: cornerRadius, isInteractive: isInteractive))
    }
    
    public func liquidGlassPill() -> some View {
        self.modifier(LiquidGlassPillModifier())
    }
    
    public func liquidGlassCircle() -> some View {
        self.modifier(LiquidGlassCircleModifier())
    }
}

extension Color {
    public static var bushyCardBackground: Color {
        #if os(iOS)
        return Color(uiColor: .secondarySystemBackground)
        #elseif os(macOS)
        return Color(nsColor: .windowBackgroundColor)
        #else
        return Color.secondary.opacity(0.15)
        #endif
    }
    
    public static var bushyAppBackground: Color {
        #if os(iOS)
        return Color(uiColor: .systemBackground)
        #elseif os(macOS)
        return Color(nsColor: .controlBackgroundColor)
        #else
        return Color.black
        #endif
    }
}
