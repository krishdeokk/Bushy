import SwiftUI

public struct BushyAIScreen: View {
    @ObservedObject var aiViewModel: BushyAIViewModel
    @ObservedObject var healthViewModel: HealthViewModel
    var onScanMeal: () -> Void
    
    @State private var inputText: String = ""
    @FocusState private var isInputFocused: Bool
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    
    public init(
        aiViewModel: BushyAIViewModel,
        healthViewModel: HealthViewModel,
        onScanMeal: @escaping () -> Void
    ) {
        self.aiViewModel = aiViewModel
        self.healthViewModel = healthViewModel
        self.onScanMeal = onScanMeal
    }
    
    public var body: some View {
        GeometryReader { geo in
            let isDuo = DuoSplitLayoutHelper.isDuoLayout(size: geo.size, horizontalSizeClass: horizontalSizeClass)
            
            ZStack {
                // Ambient Liquid Background (Pure Monochrome)
                LiquidFluidBackground(isMonochrome: true)
                
                if isDuo {
                    duoSplitBody(geo: geo)
                } else {
                    standardPortraitBody
                }
            }
        }
    }
    
    // MARK: - Standard Single Column Body (Portrait iPhone)
    
    private var standardPortraitBody: some View {
        VStack(spacing: 0) {
            // Frosted Liquid Header Bar with Bloub Avatar
            headerBar
            
            // Messages Scroll View
            messagesScrollView
            
            // Suggested Prompts Carousel
            suggestedPrompts
            
            // Bottom Floating Liquid Chat Dock
            chatInputBar
        }
    }
    
    // MARK: - Duo Split Screen Body (Landscape iPhone, Plus/Max, iPad, Foldables)
    
    private func duoSplitBody(geo: GeometryProxy) -> some View {
        HStack(alignment: .top, spacing: 18) {
            // Left Column: Companion Identity, Offline Status, Quick Prompts & Scanner
            VStack(spacing: 16) {
                duoCompanionProfileCard
                
                duoQuickPromptsPanel
                
                Spacer(minLength: 110)
            }
            .frame(maxWidth: min(geo.size.width * 0.38, 320))
            
            // Right Column: Chat messages stream & dock
            VStack(spacing: 0) {
                duoChatHeader
                
                messagesScrollView
                
                chatInputBar
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
    }
    
    // MARK: - Duo Left Column Cards
    
    private var duoCompanionProfileCard: some View {
        VStack(spacing: 12) {
            BloubAvatar(
                expression: .attentive,
                visualStyle: .monochrome,
                themeMode: healthViewModel.uiState.themeMode
            )
            .frame(width: 80, height: 80)
            
            VStack(spacing: 4) {
                HStack(spacing: 6) {
                    Text("Bushy AI ☁️")
                        .font(.headline)
                        .fontWeight(.black)
                    
                    Text("OFFLINE")
                        .font(.system(size: 9, weight: .black))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color.primary.opacity(0.12))
                        .clipShape(Capsule())
                }
                
                Text("Health & Nutrition Companion")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Button {
                HapticManager.impact(style: .medium)
                onScanMeal()
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "camera.fill")
                    Text("Scan Food / Meal")
                        .fontWeight(.bold)
                }
                .font(.caption)
                .foregroundColor(Color.bushyAppBackground)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(Color.primary)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(Color.white.opacity(0.3), lineWidth: 1))
            }
            .padding(.top, 4)
        }
        .padding(16)
        .liquidGlassCard(cornerRadius: 22)
    }
    
    private var duoQuickPromptsPanel: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Suggested Missions & Qs")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
            
            promptPill("Walk 5,000 steps today")
            promptPill("How much protein in 2 eggs?")
            promptPill("Do 25 pushups")
            promptPill("Drink 8 glasses of water")
        }
        .padding(14)
        .liquidGlassCard(cornerRadius: 20)
    }
    
    private var duoChatHeader: some View {
        HStack {
            Text("AI Fitness Conversation")
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            
            Spacer()
            
            Text("\(aiViewModel.messages.count) messages")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .liquidGlassCard(cornerRadius: 16)
        .padding(.bottom, 8)
    }
    
    // MARK: - Messages Scroll View
    
    private var messagesScrollView: some View {
        ScrollViewReader { proxy in
            ScrollView(showsIndicators: false) {
                LazyVStack(spacing: 14) {
                    ForEach(aiViewModel.messages) { msg in
                        messageRow(msg)
                    }
                    
                    if aiViewModel.isGenerating {
                        typingIndicator
                    }
                    
                    Spacer(minLength: 120) // Spacing for input and nav pill
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
            }
            .onChange(of: aiViewModel.messages.count) {
                if let last = aiViewModel.messages.last {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        proxy.scrollTo(last.id, anchor: .bottom)
                    }
                }
            }
        }
    }
    
    // MARK: - Header (Portrait Header with Bloub Avatar)
    
    private var headerBar: some View {
        HStack(spacing: 12) {
            BloubAvatar(
                expression: .attentive,
                visualStyle: .monochrome,
                themeMode: healthViewModel.uiState.themeMode
            )
            .frame(width: 42, height: 42)
            
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text("Bushy AI ☁️")
                        .font(.headline)
                        .fontWeight(.black)
                    
                    Text("OFFLINE")
                        .font(.system(size: 9, weight: .black))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color.primary.opacity(0.12))
                        .clipShape(Capsule())
                }
                
                Text("Health & Nutrition Companion")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Button {
                HapticManager.impact(style: .medium)
                onScanMeal()
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "camera.fill")
                    Text("Scan")
                        .fontWeight(.bold)
                }
                .font(.caption)
                .foregroundColor(.primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .liquidGlassPill()
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 14)
        .padding(.bottom, 12)
        .background(.ultraThinMaterial.opacity(0.85))
    }
    
    // MARK: - Message Row (Clean, modern bubbles without repetitive avatar icons)
    
    private func messageRow(_ msg: BushyAIMessage) -> some View {
        HStack(alignment: .bottom, spacing: 8) {
            if msg.isUser {
                Spacer(minLength: 40)
            }
            
            VStack(alignment: msg.isUser ? .trailing : .leading, spacing: 4) {
                Text(msg.text)
                    .font(.subheadline)
                    .foregroundColor(msg.isUser ? Color.bushyAppBackground : .primary)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(
                        msg.isUser
                            ? AnyView(Color.primary)
                            : AnyView(Color.secondary.opacity(0.12))
                    )
                    .clipShape(
                        RoundedRectangle(cornerRadius: 18)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 18)
                            .stroke(
                                msg.isUser ? Color.clear : Color.primary.opacity(0.15),
                                lineWidth: 1
                            )
                    )
            }
            
            if !msg.isUser {
                Spacer(minLength: 40)
            }
        }
    }
    
    // MARK: - Typing Indicator
    
    private var typingIndicator: some View {
        HStack(alignment: .bottom, spacing: 8) {
            HStack(spacing: 4) {
                Circle().fill(Color.primary.opacity(0.5)).frame(width: 6, height: 6)
                Circle().fill(Color.primary.opacity(0.5)).frame(width: 6, height: 6)
                Circle().fill(Color.primary.opacity(0.5)).frame(width: 6, height: 6)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(Color.secondary.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            
            Spacer()
        }
    }
    
    // MARK: - Suggested Prompts
    
    private var suggestedPrompts: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                promptPill("Walk 5,000 steps today")
                promptPill("How much protein in 2 eggs?")
                promptPill("Do 25 pushups")
                promptPill("Drink 8 glasses of water")
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 6)
        }
    }
    
    private func promptPill(_ text: String) -> some View {
        Button {
            aiViewModel.sendMessage(text, userStats: healthViewModel.uiState) { title, target, type in
                healthViewModel.addNewTask(title: title, target: target, type: type)
            }
        } label: {
            Text(text)
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.primary)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .liquidGlassPill()
        }
    }
    
    // MARK: - Chat Input Bar
    
    private var chatInputBar: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                TextField("Ask Bushy anything about fitness...", text: $inputText)
                    .font(.subheadline)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .liquidGlassCard(cornerRadius: 22)
                    .focused($isInputFocused)
                    .onSubmit {
                        sendCurrentText()
                    }
                
                Button {
                    HapticManager.impact(style: .medium)
                    sendCurrentText()
                } label: {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.system(size: 34))
                        .foregroundColor(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .secondary.opacity(0.4) : .primary)
                }
                .disabled(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)
            .padding(.bottom, 96) // clearance above floating navigation pill
            .background(.ultraThinMaterial.opacity(0.65))
        }
    }
    
    private func sendCurrentText() {
        let trimmed = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        inputText = ""
        aiViewModel.sendMessage(trimmed, userStats: healthViewModel.uiState) { title, target, type in
            healthViewModel.addNewTask(title: title, target: target, type: type)
        }
    }
}
