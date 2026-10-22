import SwiftUI

public struct NavigationPill: View {
    @Binding var selectedTab: Int
    var visualStyle: VisualStyle
    var onAddQuest: () -> Void
    var onScanMeal: () -> Void
    var onShareStory: () -> Void
    
    @State private var isMenuExpanded = false
    @State private var isTappedPulse = false
    
    private var isMono: Bool {
        visualStyle == .monochrome
    }
    
    public var body: some View {
        ZStack(alignment: .bottom) {
            // Expandable Liquid Glass Actions Menu
            if isMenuExpanded {
                Color.black.opacity(0.40)
                    .background(.ultraThinMaterial.opacity(0.6))
                    .ignoresSafeArea()
                    .onTapGesture {
                        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                            isMenuExpanded = false
                        }
                    }
                
                VStack(spacing: 12) {
                    quickActionButton(icon: "camera.fill", title: "Scan Food / Meal", color: .orange) {
                        withAnimation { isMenuExpanded = false }
                        onScanMeal()
                    }
                    quickActionButton(icon: "plus.circle.fill", title: "Add New Quest", color: .blue) {
                        withAnimation { isMenuExpanded = false }
                        onAddQuest()
                    }
                    quickActionButton(icon: "square.and.arrow.up.fill", title: "Share Story Card", color: .purple) {
                        withAnimation { isMenuExpanded = false }
                        onShareStory()
                    }
                }
                .padding(.bottom, 96)
                .transition(.scale(scale: 0.85).combined(with: .opacity))
            }
            
            // Floating Liquid Glass Bar
            HStack(spacing: 12) {
                // Main Navigation Tabs
                HStack(spacing: 6) {
                    pillItem(index: 0, title: "Home", icon: "house.fill")
                    pillItem(index: 1, title: "Quests", icon: "checklist")
                    pillItem(index: 2, title: "Bushy AI", icon: "sparkles")
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 8)
                .liquidGlassPill()
                
                // Liquid Plus Action Button
                Button {
                    HapticManager.impact(style: .medium)
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                        isMenuExpanded.toggle()
                    }
                } label: {
                    ZStack {
                        Circle()
                            .fill(
                                isMono ?
                                AnyShapeStyle(Color.primary) :
                                AnyShapeStyle(
                                    LinearGradient(
                                        colors: [Color.accentColor, Color.purple],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                            )
                        
                        Circle()
                            .stroke(
                                LinearGradient(
                                    colors: [Color.white.opacity(0.6), Color.white.opacity(0.1)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1.5
                            )
                        
                        Image(systemName: isMenuExpanded ? "xmark" : "plus")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(isMono ? Color.bushyAppBackground : .white)
                            .rotationEffect(.degrees(isMenuExpanded ? 90 : 0))
                    }
                    .frame(width: 52, height: 52)
                    .shadow(color: isMono ? Color.black.opacity(0.2) : Color.accentColor.opacity(0.35), radius: 10, x: 0, y: 5)
                }
            }
            .scaleEffect(isTappedPulse ? 1.06 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isTappedPulse)
        }
    }
    
    private func pillItem(index: Int, title: String, icon: String) -> some View {
        let isSelected = selectedTab == index
        return Button {
            HapticManager.impact(style: .light)
            isTappedPulse = true
            withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                selectedTab = index
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                isTappedPulse = false
            }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .bold))
                if isSelected {
                    Text(title)
                        .font(.system(size: 14, weight: .bold))
                }
            }
            .foregroundColor(
                isSelected ?
                    (isMono ? Color.bushyAppBackground : .white) :
                    .primary.opacity(0.85)
            )
            .padding(.horizontal, isSelected ? 16 : 12)
            .padding(.vertical, 10)
            .background(
                Capsule()
                    .fill(
                        isSelected ?
                        (isMono ? AnyShapeStyle(Color.primary) : AnyShapeStyle(
                            LinearGradient(
                                colors: [Color.accentColor, Color.purple.opacity(0.85)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )) :
                        AnyShapeStyle(Color.clear)
                    )
            )
            .overlay(
                Capsule()
                    .stroke(isSelected ? (isMono ? Color.primary.opacity(0.2) : Color.white.opacity(0.35)) : Color.clear, lineWidth: 1)
            )
        }
    }
    
    private func quickActionButton(icon: String, title: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(isMono ? .primary : color)
                    .frame(width: 32)
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.primary)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(width: 230)
            .liquidGlassCard(cornerRadius: 18)
        }
    }
}
