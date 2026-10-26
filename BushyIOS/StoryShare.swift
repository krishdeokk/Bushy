import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

public struct StoryShare: View {
    @ObservedObject var viewModel: HealthViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    
    #if os(iOS)
    @State private var renderedImage: UIImage? = nil
    #endif
    @State private var showShareSheet: Bool = false
    
    public init(viewModel: HealthViewModel) {
        self.viewModel = viewModel
    }
    
    public var body: some View {
        NavigationStack {
            GeometryReader { geo in
                let isDuo = DuoSplitLayoutHelper.isDuoLayout(size: geo.size, horizontalSizeClass: horizontalSizeClass)
                
                ZStack {
                    Color.bushyAppBackground.ignoresSafeArea()
                    
                    if isDuo {
                        duoSplitBody(geo: geo)
                    } else {
                        standardPortraitBody
                    }
                }
            }
            .navigationTitle("Share Story")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            #if os(iOS)
            .sheet(isPresented: $showShareSheet) {
                if let image = renderedImage {
                    ShareActivityView(activityItems: [image])
                }
            }
            #endif
        }
    }
    
    // MARK: - Standard Single Column Body
    
    private var standardPortraitBody: some View {
        VStack(spacing: 20) {
            // The Story Card (9:16 aspect ratio preview)
            storyCardContent
                .frame(maxWidth: 320)
                .aspectRatio(9/16, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 32))
                .overlay(
                    RoundedRectangle(cornerRadius: 32)
                        .stroke(Color.primary.opacity(0.3), lineWidth: 1.5)
                )
                .shadow(color: Color.black.opacity(0.35), radius: 16, x: 0, y: 8)
                .padding(.top, 10)
            
            Spacer()
            
            // Share Action Button
            shareButton
                .padding(.horizontal, 32)
                .padding(.bottom, 20)
        }
    }
    
    // MARK: - Duo Split Screen Body
    
    private func duoSplitBody(geo: GeometryProxy) -> some View {
        let cardHeight = min(geo.size.height * 0.82, 480)
        
        return HStack(spacing: 32) {
            // Left Column: Story Card Preview
            storyCardContent
                .frame(height: cardHeight)
                .aspectRatio(9/16, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 28))
                .overlay(
                    RoundedRectangle(cornerRadius: 28)
                        .stroke(Color.primary.opacity(0.3), lineWidth: 1.5)
                )
                .shadow(color: Color.black.opacity(0.35), radius: 16, x: 0, y: 8)
            
            // Right Column: Summary & Share Actions
            VStack(alignment: .leading, spacing: 18) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Ready to Inspire 🚀")
                        .font(.title2)
                        .fontWeight(.black)
                        .foregroundColor(.primary)
                    
                    Text("Export your high-contrast achievement card and share with friends or on social media.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                VStack(spacing: 10) {
                    HStack {
                        Label("Current Streak", systemImage: "flame.fill")
                        Spacer()
                        Text("\(viewModel.uiState.streakDays) Days").fontWeight(.bold)
                    }
                    Divider()
                    HStack {
                        Label("Avatar Level", systemImage: "star.fill")
                        Spacer()
                        Text("Level \(viewModel.uiState.level)").fontWeight(.bold)
                    }
                    Divider()
                    HStack {
                        Label("Steps Logged", systemImage: "figure.walk")
                        Spacer()
                        Text("\(viewModel.uiState.steps)").fontWeight(.bold)
                    }
                }
                .font(.caption)
                .padding(14)
                .liquidGlassCard(cornerRadius: 18)
                
                Spacer()
                
                shareButton
            }
            .frame(maxWidth: 320)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 16)
    }
    
    private var shareButton: some View {
        Button {
            renderAndShare()
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "square.and.arrow.up.fill")
                Text("Share Story Card")
                    .fontWeight(.bold)
            }
            .foregroundColor(Color.bushyAppBackground)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(Color.primary)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(Color.white.opacity(0.35), lineWidth: 1.2))
            .shadow(color: Color.black.opacity(0.18), radius: 10, x: 0, y: 5)
        }
    }
    
    // MARK: - Story Card Content
    
    private var storyCardContent: some View {
        ZStack {
            // Background Gradient (Pure Monochrome Noir)
            LinearGradient(
                colors: [
                    Color(white: 0.08),
                    Color(white: 0.14),
                    Color(white: 0.05)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            
            // Decorative background circles
            Circle()
                .fill(Color.white.opacity(0.04))
                .frame(width: 240, height: 240)
                .offset(x: -80, y: -120)
            
            Circle()
                .fill(Color.white.opacity(0.03))
                .frame(width: 280, height: 280)
                .offset(x: 100, y: 140)
            
            VStack(spacing: 14) {
                Spacer().frame(height: 20)
                
                // Level Badge
                Text("LEVEL \(viewModel.uiState.level)")
                    .font(.caption)
                    .fontWeight(.black)
                    .foregroundColor(.black)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 6)
                    .background(Color.white)
                    .clipShape(Capsule())
                
                Text(viewModel.uiState.userName.isEmpty ? "My Bushy Journey" : "\(viewModel.uiState.userName)'s Grind")
                    .font(.title2)
                    .fontWeight(.black)
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                
                // Progress Bar
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.white.opacity(0.2)).frame(height: 6)
                        Capsule().fill(Color.white).frame(width: geo.size.width * CGFloat(viewModel.uiState.levelProgress), height: 6)
                    }
                }
                .frame(width: 140, height: 6)
                
                Spacer()
                
                // Hero Bloub Avatar
                BloubAvatar(
                    expression: viewModel.uiState.expression,
                    visualStyle: .monochrome,
                    themeMode: viewModel.uiState.themeMode
                )
                .frame(width: 180, height: 180)
                
                Spacer()
                
                // Stats 2x2 Grid
                VStack(spacing: 8) {
                    HStack(spacing: 8) {
                        miniStat(icon: "figure.walk", value: "\(viewModel.uiState.steps)", label: "Steps")
                        miniStat(icon: "flame.fill", value: "\(viewModel.uiState.calories)", label: "Calories")
                    }
                    HStack(spacing: 8) {
                        miniStat(icon: "figure.strengthtraining.traditional", value: "\(viewModel.uiState.pushups)", label: "Pushups")
                        miniStat(icon: "bolt.fill", value: "\(viewModel.uiState.streakDays) Days", label: "Streak")
                    }
                }
                .padding(.horizontal, 20)
                
                Spacer()
                
                // App Branding Footer
                HStack(spacing: 6) {
                    Text("Bushy ☁️")
                        .font(.headline)
                        .fontWeight(.black)
                        .foregroundColor(.white)
                    Text("• Turn your grind into health")
                        .font(.caption2)
                        .foregroundColor(Color(white: 0.6))
                }
                .padding(.bottom, 24)
            }
        }
    }
    
    private func miniStat(icon: String, value: String, label: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.white)
            VStack(alignment: .leading, spacing: 2) {
                Text(value)
                    .font(.system(size: 15, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                Text(label)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(Color(white: 0.6))
            }
            Spacer()
        }
        .padding(10)
        .background(Color(white: 0.16))
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(Color.white.opacity(0.12), lineWidth: 1)
        )
    }
    
    @MainActor
    private func renderAndShare() {
        #if os(iOS)
        let renderer = ImageRenderer(content: storyCardContent.frame(width: 360, height: 640))
        renderer.scale = 3.0
        if let uiImage = renderer.uiImage {
            self.renderedImage = uiImage
            self.showShareSheet = true
        }
        #endif
    }
}

// Backwards-compatible alias for existing call-sites
public typealias StoryShareView = StoryShare

#if os(iOS)
public struct ShareActivityView: UIViewControllerRepresentable {
    public var activityItems: [Any]
    public var applicationActivities: [UIActivity]? = nil
    
    public init(activityItems: [Any], applicationActivities: [UIActivity]? = nil) {
        self.activityItems = activityItems
        self.applicationActivities = applicationActivities
    }
    
    public func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: applicationActivities)
    }
    
    public func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
#endif
