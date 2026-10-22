import SwiftUI

public struct HomeScreen: View {
    @ObservedObject var viewModel: GameViewModel
    var onOpenQuests: () -> Void
    var onShare: () -> Void
    var onSwitchHero: () -> Void
    
    private var isMono: Bool {
        viewModel.uiState.visualStyle == .monochrome
    }
    
    public var body: some View {
        ZStack {
            // Ambient Liquid Fluid Background
            LiquidFluidBackground(isMonochrome: isMono)
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 22) {
                    // Top Action Bar
                    topBar
                    
                    // Level & XP Bar
                    levelProgressSection
                    
                    // Hero Avatar Section (Clean procedural Bloub)
                    heroAvatarSection
                    
                    // Stats Grid
                    statsGrid
                    
                    // Quick Quests Snapshot
                    questsSnapshot
                    
                    Spacer(minLength: 120) // Bottom spacing for navigation pill
                }
                .padding(.horizontal, 20)
                .padding(.top, 12)
            }
        }
    }
    
    // MARK: - Top Bar
    
    private var topBar: some View {
        HStack {
            Menu {
                Button {
                    viewModel.refreshStats()
                } label: {
                    Label("Sync Apple Health", systemImage: "arrow.triangle.2.circlepath")
                }
                
                Button {
                    onSwitchHero()
                } label: {
                    Label("Switch Hero Avatar", systemImage: "person.crop.circle")
                }
                
                Divider()
                
                Button {
                    viewModel.setVisualStyle(viewModel.uiState.visualStyle == .material3 ? .monochrome : .material3)
                } label: {
                    Label(viewModel.uiState.visualStyle == .material3 ? "Switch to Monochrome" : "Switch to Vibrant M3", systemImage: "paintbrush.fill")
                }
                
                Button {
                    let next: ThemeMode = (viewModel.uiState.themeMode == .light) ? .dark : ((viewModel.uiState.themeMode == .dark) ? .system : .light)
                    viewModel.setThemeMode(next)
                } label: {
                    Label("Theme: \(viewModel.uiState.themeMode.rawValue.capitalized)", systemImage: "circle.lefthalf.filled")
                }
                
                Divider()
                
                Button(role: .destructive) {
                    viewModel.resetAllStats()
                } label: {
                    Label("Reset Progress", systemImage: "trash")
                }
            } label: {
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.primary)
                    .frame(width: 44, height: 44)
                    .liquidGlassCircle()
            }
            
            Spacer()
            
            // Streak Flame Badge
            HStack(spacing: 6) {
                Image(systemName: "flame.fill")
                    .foregroundColor(isMono ? .primary : .orange)
                Text("\(viewModel.uiState.streakDays) Days")
                    .font(.subheadline)
                    .fontWeight(.bold)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .liquidGlassPill()
            
            // Level Badge
            Text("LVL \(viewModel.uiState.level)")
                .font(.subheadline)
                .fontWeight(.black)
                .foregroundColor(isMono ? .primary : .accentColor)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .liquidGlassPill()
            
            // Share Button
            Button(action: onShare) {
                Image(systemName: "square.and.arrow.up")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(.primary)
                    .frame(width: 44, height: 44)
                    .liquidGlassCircle()
            }
        }
    }
    
    // MARK: - Level Progress Bar
    
    private var levelProgressSection: some View {
        VStack(spacing: 8) {
            HStack {
                Text("Level \(viewModel.uiState.level)")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.secondary)
                Spacer()
                Text("\(viewModel.uiState.xpInCurrentLevel) / \(viewModel.uiState.xpRequiredForNextLevel) XP")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.secondary)
            }
            
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.secondary.opacity(0.18))
                        .frame(height: 8)
                    
                    Capsule()
                        .fill(
                            isMono ?
                            AnyShapeStyle(Color.primary) :
                            AnyShapeStyle(
                                LinearGradient(
                                    colors: [Color.accentColor, Color.purple],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                        )
                        .frame(width: max(0, geo.size.width * CGFloat(viewModel.uiState.levelProgress)), height: 8)
                        .shadow(color: isMono ? Color.clear : Color.accentColor.opacity(0.4), radius: 6, x: 0, y: 2)
                }
            }
            .frame(height: 8)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .liquidGlassCard(cornerRadius: 18)
    }
    
    // MARK: - Hero Avatar Section
    
    private var heroAvatarSection: some View {
        VStack(spacing: 8) {
            ZStack {
                // Subtle organic background aura (clean, no glass effect)
                Circle()
                    .fill(isMono ? Color.secondary.opacity(0.06) : Color.accentColor.opacity(0.08))
                    .frame(width: 280, height: 280)
                
                BloubAvatarView(
                    expression: viewModel.uiState.expression,
                    visualStyle: viewModel.uiState.visualStyle,
                    themeMode: viewModel.uiState.themeMode,
                    onTap: {
                        HapticManager.impact(style: .light)
                        viewModel.onAvatarTap()
                    },
                    onDoubleTap: {
                        HapticManager.impact(style: .medium)
                        viewModel.onAvatarDoubleTap()
                    }
                )
                .frame(width: 300, height: 300)
            }
            .frame(height: 320)
            
            Text(viewModel.uiState.userName.isEmpty ? "Bushy ☁️" : "\(viewModel.uiState.userName)'s Bushy ☁️")
                .font(.title2)
                .fontWeight(.black)
                .foregroundColor(isMono ? .primary : .accentColor)
        }
    }
    
    // MARK: - Stats Grid
    
    private var statsGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)], spacing: 14) {
            statCard(
                icon: "figure.walk",
                value: "\(viewModel.uiState.steps)",
                label: "Steps Today",
                color: .blue
            )
            statCard(
                icon: "flame.fill",
                value: "\(viewModel.uiState.calories)",
                label: "Burned (kcal)",
                color: .orange
            )
            statCard(
                icon: "figure.strengthtraining.traditional",
                value: "\(viewModel.uiState.pushups)",
                label: "Pushups",
                color: .green
            )
            statCard(
                icon: "star.fill",
                value: "\(viewModel.uiState.xp)",
                label: "Total XP",
                color: .purple
            )
        }
    }
    
    private func statCard(icon: String, value: String, label: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(isMono ? .primary : color)
                Spacer()
            }
            
            Text(value)
                .font(.system(size: 24, weight: .black, design: .rounded))
                .foregroundColor(.primary)
            
            Text(label)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.secondary)
        }
        .padding(18)
        .liquidGlassCard(cornerRadius: 22)
    }
    
    // MARK: - Quests Snapshot
    
    private var questsSnapshot: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Active Missions")
                    .font(.headline)
                    .fontWeight(.bold)
                Spacer()
                Button {
                    onOpenQuests()
                } label: {
                    Text("View All")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(isMono ? .primary : .accentColor)
                }
            }
            
            ForEach(viewModel.uiState.tasks.prefix(3)) { task in
                HStack(spacing: 12) {
                    Image(systemName: task.type.iconName)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(task.isCompleted ? (isMono ? .primary : .green) : (isMono ? .primary : .accentColor))
                        .frame(width: 36, height: 36)
                        .liquidGlassCircle()
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text(task.title)
                            .font(.subheadline)
                            .fontWeight(.bold)
                        
                        Text("\(task.current) / \(task.target) \(task.type.unitLabel)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    if task.isCompleted {
                        Image(systemName: "checkmark.seal.fill")
                            .foregroundColor(isMono ? .primary : .green)
                            .font(.title3)
                    } else {
                        Button {
                            HapticManager.impact(style: .medium)
                            viewModel.singleTapIncrement(task.id)
                        } label: {
                            Image(systemName: "plus")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(isMono ? Color.bushyAppBackground : .white)
                                .frame(width: 32, height: 32)
                                .background(isMono ? Color.primary : Color.accentColor)
                                .clipShape(Circle())
                                .shadow(color: isMono ? Color.clear : Color.accentColor.opacity(0.3), radius: 6, x: 0, y: 3)
                        }
                    }
                }
                .padding(14)
                .liquidGlassCard(cornerRadius: 18)
            }
        }
        .padding(.top, 8)
    }
}
