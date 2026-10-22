import SwiftUI

public struct BushyAIScreen: View {
    @ObservedObject var aiViewModel: BushyAIViewModel
    @ObservedObject var gameViewModel: GameViewModel
    var onScanMeal: () -> Void
    
    @State private var inputText: String = ""
    @FocusState private var isInputFocused: Bool
    
    public var body: some View {
        ZStack {
            // Ambient Liquid Background (Pure Monochrome)
            LiquidFluidBackground(isMonochrome: true)
            
            VStack(spacing: 0) {
                // Frosted Liquid Header Bar
                headerBar
                
                // Messages Scroll View
                ScrollViewReader { proxy in
                    ScrollView(showsIndicators: false) {
                        LazyVStack(spacing: 16) {
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
                            withAnimation {
                                proxy.scrollTo(last.id, anchor: .bottom)
                            }
                        }
                    }
                }
                
                // Suggested Prompts Carousel
                suggestedPrompts
                
                // Bottom Floating Liquid Chat Dock
                chatInputBar
            }
        }
    }
    
    // MARK: - Header
    
    private var headerBar: some View {
        HStack(spacing: 12) {
            BloubAvatarView(
                expression: .happy,
                visualStyle: .monochrome,
                themeMode: gameViewModel.uiState.themeMode
            )
            .frame(width: 44, height: 44)
            
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text("Bushy AI ☁️")
                        .font(.headline)
                        .fontWeight(.black)
                    
                    Text("OFFLINE")
                        .font(.system(size: 9, weight: .black))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .liquidGlassPill()
                        .foregroundColor(.primary)
                }
                
                Text("Your gamified fitness & Indian diet mentor")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(.ultraThinMaterial)
        .overlay(
            Rectangle()
                .frame(height: 1)
                .foregroundColor(Color.white.opacity(0.18)),
            alignment: .bottom
        )
    }
    
    // MARK: - Message Row
    
    private func messageRow(_ msg: BushyAIMessage) -> some View {
        HStack(alignment: .bottom, spacing: 8) {
            if msg.isUser {
                Spacer(minLength: 40)
            } else {
                BloubAvatarView(expression: .attentive, visualStyle: .monochrome)
                    .frame(width: 32, height: 32)
            }
            
            VStack(alignment: msg.isUser ? .trailing : .leading, spacing: 4) {
                Text(msg.text)
                    .font(.system(size: 15))
                    .foregroundColor(msg.isUser ? Color.bushyAppBackground : .primary)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(
                        msg.isUser ?
                        AnyShapeStyle(Color.primary) :
                        AnyShapeStyle(.ultraThinMaterial)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20, style: .continuous)
                            .stroke(Color.white.opacity(msg.isUser ? 0.35 : 0.25), lineWidth: 1)
                    )
                    .shadow(color: Color.black.opacity(0.08), radius: 8, x: 0, y: 4)
            }
            
            if !msg.isUser {
                Spacer(minLength: 40)
            }
        }
        .id(msg.id)
    }
    
    private var typingIndicator: some View {
        HStack(spacing: 8) {
            BloubAvatarView(expression: .thinking, visualStyle: .monochrome)
                .frame(width: 32, height: 32)
            
            HStack(spacing: 4) {
                Circle().fill(Color.secondary).frame(width: 6, height: 6)
                Circle().fill(Color.secondary).frame(width: 6, height: 6)
                Circle().fill(Color.secondary).frame(width: 6, height: 6)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .liquidGlassPill()
            
            Spacer()
        }
    }
    
    // MARK: - Suggested Prompts
    
    private var suggestedPrompts: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                promptPill("🍳 Suggest high protein breakfast")
                promptPill("🎯 Give me a quest for 25 pushups")
                promptPill("🍛 How many calories in Dal Tadka?")
                promptPill("⚡ 15-minute home workout")
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 6)
        }
    }
    
    private func promptPill(_ text: String) -> some View {
        Button {
            aiViewModel.sendMessage(text, userStats: gameViewModel.uiState) { title, target, type in
                gameViewModel.addNewTask(title: title, target: target, type: type)
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
                Button(action: onScanMeal) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 18))
                        .foregroundColor(.primary)
                        .frame(width: 44, height: 44)
                        .liquidGlassCircle()
                }
                
                TextField("Ask anything or create a quest...", text: $inputText)
                    .font(.subheadline)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .liquidGlassPill()
                    .focused($isInputFocused)
                    .onSubmit {
                        sendCurrentText()
                    }
                
                Button(action: sendCurrentText) {
                    Image(systemName: "arrow.up")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(Color.bushyAppBackground)
                        .frame(width: 44, height: 44)
                        .background(
                            inputText.isEmpty ?
                            AnyShapeStyle(Color.secondary.opacity(0.3)) :
                            AnyShapeStyle(Color.primary)
                        )
                        .clipShape(Circle())
                        .overlay(Circle().stroke(Color.white.opacity(0.3), lineWidth: 1))
                        .shadow(color: inputText.isEmpty ? Color.clear : Color.black.opacity(0.2), radius: 8, x: 0, y: 4)
                }
                .disabled(inputText.isEmpty)
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
        aiViewModel.sendMessage(trimmed, userStats: gameViewModel.uiState) { title, target, type in
            gameViewModel.addNewTask(title: title, target: target, type: type)
        }
    }
}
