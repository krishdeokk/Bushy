import SwiftUI

public struct NavigationPill: View {
    @Binding var selectedTab: Int
    var visualStyle: VisualStyle = .monochrome
    var onAddQuest: () -> Void
    var onScanMeal: () -> Void
    var onShareStory: () -> Void
    
    @State private var isMenuExpanded = false
    @State private var isTappedPulse = false
    
    public var body: some View {
        ZStack(alignment: .bottom) {
            // Dismissable Soft Dimming Backdrop
            if isMenuExpanded {
                Color.black.opacity(0.25)
                    .ignoresSafeArea()
                    .contentShape(Rectangle())
                    .onTapGesture {
                        HapticManager.impact(style: .light)
                        withAnimation(.spring(response: 0.32, dampingFraction: 0.82)) {
                            isMenuExpanded = false
                        }
                    }
                    .transition(.opacity)
            }
            
            // Bottom Bar & Anchored Trailing Menu
            VStack(alignment: .trailing, spacing: 14) {
                if isMenuExpanded {
                    fabMenuCard
                        .transition(
                            .asymmetric(
                                insertion: .scale(scale: 0.65, anchor: .bottomTrailing)
                                    .combined(with: .opacity)
                                    .combined(with: .offset(y: 12)),
                                removal: .scale(scale: 0.75, anchor: .bottomTrailing)
                                    .combined(with: .opacity)
                                    .combined(with: .offset(y: 8))
                            )
                        )
                }
                
                bottomDockBar
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
        }
    }
    
    // MARK: - Cohesive Liquid Glass Action Menu Card
    
    private var fabMenuCard: some View {
        VStack(spacing: 0) {
            menuRow(
                icon: "camera.fill",
                title: "Scan Food / Meal",
                subtitle: "ICMR nutrition & macros"
            ) {
                withAnimation(.spring(response: 0.32, dampingFraction: 0.82)) {
                    isMenuExpanded = false
                }
                onScanMeal()
            }
            
            Divider()
                .opacity(0.35)
                .padding(.horizontal, 14)
            
            menuRow(
                icon: "plus.circle.fill",
                title: "Add New Quest",
                subtitle: "Set custom daily target"
            ) {
                withAnimation(.spring(response: 0.32, dampingFraction: 0.82)) {
                    isMenuExpanded = false
                }
                onAddQuest()
            }
            
            Divider()
                .opacity(0.35)
                .padding(.horizontal, 14)
            
            menuRow(
                icon: "square.and.arrow.up.fill",
                title: "Share Story Card",
                subtitle: "Export 9:16 achievement"
            ) {
                withAnimation(.spring(response: 0.32, dampingFraction: 0.82)) {
                    isMenuExpanded = false
                }
                onShareStory()
            }
        }
        .frame(width: 254)
        .liquidGlassCard(cornerRadius: 24)
        .shadow(color: Color.black.opacity(0.18), radius: 24, x: 0, y: 12)
    }
    
    private func menuRow(
        icon: String,
        title: String,
        subtitle: String,
        action: @escaping () -> Void
    ) -> some View {
        Button {
            HapticManager.impact(style: .medium)
            action()
        } label: {
            HStack(spacing: 12) {
                // Glass-badged icon
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.primary)
                    .frame(width: 36, height: 36)
                    .background(Color.primary.opacity(0.08))
                    .clipShape(Circle())
                    .overlay(
                        Circle().stroke(Color.white.opacity(0.3), lineWidth: 0.8)
                    )
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.primary)
                    
                    Text(subtitle)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.secondary.opacity(0.5))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(MenuRowButtonStyle())
    }
    
    // MARK: - Floating Liquid Bottom Bar
    
    private var bottomDockBar: some View {
        HStack(spacing: 10) {
            // Main Navigation Tabs Capsule
            HStack(spacing: 4) {
                pillItem(index: 0, title: "Home", icon: "house.fill")
                pillItem(index: 1, title: "Quests", icon: "checklist")
                pillItem(index: 2, title: "Bushy AI", icon: "sparkles")
            }
            .padding(.horizontal, 6)
            .padding(.vertical, 6)
            .liquidGlassPill()
            
            // Liquid Action FAB Button
            Button {
                HapticManager.impact(style: .medium)
                withAnimation(.spring(response: 0.35, dampingFraction: 0.78)) {
                    isMenuExpanded.toggle()
                }
            } label: {
                ZStack {
                    Circle()
                        .fill(Color.primary)
                    
                    Circle()
                        .stroke(
                            LinearGradient(
                                colors: [Color.white.opacity(0.6), Color.white.opacity(0.15)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1.5
                        )
                    
                    Image(systemName: "plus")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color.bushyAppBackground)
                        .rotationEffect(.degrees(isMenuExpanded ? 135 : 0))
                }
                .frame(width: 52, height: 52)
                .shadow(color: Color.black.opacity(isMenuExpanded ? 0.3 : 0.15), radius: 10, x: 0, y: 5)
            }
        }
        .scaleEffect(isTappedPulse ? 1.05 : 1.0)
        .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isTappedPulse)
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
                    .font(.system(size: 15, weight: .bold))
                if isSelected {
                    Text(title)
                        .font(.system(size: 13, weight: .bold))
                }
            }
            .foregroundColor(
                isSelected ? Color.bushyAppBackground : .primary.opacity(0.85)
            )
            .padding(.horizontal, isSelected ? 15 : 11)
            .padding(.vertical, 9)
            .background(
                Capsule()
                    .fill(isSelected ? Color.primary : Color.clear)
            )
            .overlay(
                Capsule()
                    .stroke(isSelected ? Color.primary.opacity(0.2) : Color.clear, lineWidth: 1)
            )
        }
    }
}

// MARK: - Button Press Feedback Style
private struct MenuRowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(
                configuration.isPressed ? Color.primary.opacity(0.06) : Color.clear
            )
            .animation(.easeInOut(duration: 0.12), value: configuration.isPressed)
    }
}
