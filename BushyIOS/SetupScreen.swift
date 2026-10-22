import SwiftUI

public struct SetupScreen: View {
    @ObservedObject var viewModel: GameViewModel
    
    @State private var currentStep = 0
    @State private var name: String = ""
    @State private var ageText: String = "22"
    @State private var heightText: String = "175"
    @State private var selectedGender: AvatarType = .male
    @State private var selectedVisualStyle: VisualStyle = .material3
    @State private var selectedThemeMode: ThemeMode = .system
    @State private var selectedCountry: String = "India"
    
    public init(viewModel: GameViewModel) {
        self.viewModel = viewModel
    }
    
    private var isMono: Bool {
        selectedVisualStyle == .monochrome
    }
    
    public var body: some View {
        ZStack {
            LinearGradient(
                colors: isMono ? [
                    Color(white: 0.12),
                    Color.bushyAppBackground
                ] : [
                    Color.accentColor.opacity(0.15),
                    Color.bushyAppBackground
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            VStack(spacing: 24) {
                // Header / Step progress
                HStack(spacing: 6) {
                    ForEach(0..<7) { index in
                        Capsule()
                            .fill(index <= currentStep ? (isMono ? Color.primary : Color.accentColor) : Color.secondary.opacity(0.2))
                            .frame(height: 5)
                    }
                }
                .padding(.horizontal, 32)
                .padding(.top, 16)
                
                Spacer()
                
                // Step Content
                Group {
                    switch currentStep {
                    case 0:
                        permissionStep
                    case 1:
                        genderStep
                    case 2:
                        vibeStep
                    case 3:
                        themeStep
                    case 4:
                        nameStep
                    case 5:
                        statsStep
                    case 6:
                        countryStep
                    default:
                        EmptyView()
                    }
                }
                .frame(maxWidth: .infinity)
                .transition(.opacity.combined(with: .scale(scale: 0.96)))
                .animation(.easeInOut(duration: 0.25), value: currentStep)
                
                Spacer()
                
                // Navigation Buttons
                HStack(spacing: 16) {
                    if currentStep > 0 {
                        Button {
                            withAnimation { currentStep -= 1 }
                        } label: {
                            Image(systemName: "arrow.left")
                                .font(.system(size: 20, weight: .bold))
                                .frame(width: 54, height: 54)
                                .background(Color.secondary.opacity(0.12))
                                .clipShape(Circle())
                        }
                    }
                    
                    Button {
                        if currentStep < 6 {
                            withAnimation { currentStep += 1 }
                        } else {
                            let age = Int(ageText) ?? 22
                            let height = Int(heightText) ?? 175
                            viewModel.setAvatar(selectedGender)
                            viewModel.setVisualStyle(selectedVisualStyle)
                            viewModel.setThemeMode(selectedThemeMode)
                            viewModel.setCountry(selectedCountry)
                            viewModel.completeSetup(name: name.isEmpty ? "Hero" : name, age: age, height: height)
                        }
                    } label: {
                        HStack {
                            Text(currentStep == 6 ? "Let's Play!" : "Continue")
                                .fontWeight(.bold)
                            Image(systemName: "arrow.right")
                        }
                        .foregroundColor(isMono ? (selectedThemeMode == .dark ? .black : .white) : .white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(isMono ? Color.primary : Color.accentColor)
                        .clipShape(Capsule())
                    }
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 24)
            }
        }
        .grayscale(isMono ? 1.0 : 0.0)
    }
    
    // MARK: - Step Views
    
    private var permissionStep: some View {
        VStack(spacing: 20) {
            Image(systemName: "heart.text.square.fill")
                .font(.system(size: 72))
                .foregroundColor(isMono ? .primary : .accentColor)
            
            Text("Sync with Apple Health")
                .font(.title2)
                .fontWeight(.bold)
            
            Text("Bushy automatically converts your real-world daily steps and fitness activity into XP and game quests.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            
            Button {
                Task {
                    _ = await HealthManager.shared.requestPermissions()
                    await MainActor.run {
                        withAnimation { currentStep += 1 }
                    }
                }
            } label: {
                Text("Grant Health Access")
                    .fontWeight(.semibold)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(isMono ? Color.primary.opacity(0.12) : Color.accentColor.opacity(0.15))
                    .clipShape(Capsule())
            }
        }
    }
    
    private var genderStep: some View {
        VStack(spacing: 24) {
            Text("Choose Your Hero")
                .font(.title)
                .fontWeight(.bold)
            
            HStack(spacing: 20) {
                ForEach(AvatarType.allCases) { type in
                    Button {
                        selectedGender = type
                    } label: {
                        VStack(spacing: 12) {
                            BloubAvatarView(
                                expression: .happy,
                                visualStyle: selectedVisualStyle,
                                themeMode: selectedThemeMode
                            )
                            .frame(width: 120, height: 120)
                            
                            Text(type.displayName)
                                .fontWeight(.bold)
                                .foregroundColor(selectedGender == type ? (isMono ? .primary : .accentColor) : .primary)
                        }
                        .padding()
                        .frame(width: 140, height: 180)
                        .background(selectedGender == type ? (isMono ? Color.primary.opacity(0.12) : Color.accentColor.opacity(0.12)) : Color.secondary.opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .overlay(
                            RoundedRectangle(cornerRadius: 24)
                                .stroke(selectedGender == type ? (isMono ? Color.primary : Color.accentColor) : Color.clear, lineWidth: 2)
                        )
                    }
                }
            }
        }
    }
    
    private var vibeStep: some View {
        VStack(spacing: 24) {
            Text("Select Visual Style")
                .font(.title)
                .fontWeight(.bold)
            
            HStack(spacing: 20) {
                VStack(spacing: 12) {
                    BloubAvatarView(expression: .excited, visualStyle: .material3)
                        .frame(width: 110, height: 110)
                    Text("Vibrant M3")
                        .fontWeight(.bold)
                }
                .padding()
                .frame(width: 140, height: 180)
                .background(selectedVisualStyle == .material3 ? Color.accentColor.opacity(0.12) : Color.secondary.opacity(0.08))
                .clipShape(RoundedRectangle(cornerRadius: 24))
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(selectedVisualStyle == .material3 ? Color.accentColor : Color.clear, lineWidth: 2))
                .onTapGesture {
                    withAnimation { selectedVisualStyle = .material3 }
                }
                
                VStack(spacing: 12) {
                    BloubAvatarView(expression: .excited, visualStyle: .monochrome)
                        .frame(width: 110, height: 110)
                    Text("Monochrome")
                        .fontWeight(.bold)
                }
                .padding()
                .frame(width: 140, height: 180)
                .background(selectedVisualStyle == .monochrome ? Color.primary.opacity(0.12) : Color.secondary.opacity(0.08))
                .clipShape(RoundedRectangle(cornerRadius: 24))
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(selectedVisualStyle == .monochrome ? Color.primary : Color.clear, lineWidth: 2))
                .onTapGesture {
                    withAnimation { selectedVisualStyle = .monochrome }
                }
            }
        }
    }
    
    private var themeStep: some View {
        VStack(spacing: 24) {
            Text("Choose App Theme")
                .font(.title)
                .fontWeight(.bold)
            
            VStack(spacing: 12) {
                ForEach(ThemeMode.allCases, id: \.self) { mode in
                    Button {
                        selectedThemeMode = mode
                    } label: {
                        HStack {
                            Image(systemName: mode == .light ? "sun.max.fill" : (mode == .dark ? "moon.fill" : "circle.lefthalf.filled"))
                                .font(.title3)
                            Text(mode.rawValue.capitalized)
                                .fontWeight(.semibold)
                            Spacer()
                            if selectedThemeMode == mode {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(isMono ? .primary : .accentColor)
                            }
                        }
                        .padding()
                        .frame(maxWidth: 280)
                        .background(Color.secondary.opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                }
            }
        }
    }
    
    private var nameStep: some View {
        VStack(spacing: 20) {
            Text("What's Your Name?")
                .font(.title)
                .fontWeight(.bold)
            
            Text("Bushy will address you as your fitness alter-ego.")
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            TextField("Enter your name", text: $name)
                .font(.title3)
                .padding()
                .background(Color.secondary.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .padding(.horizontal, 40)
        }
    }
    
    private var statsStep: some View {
        VStack(spacing: 24) {
            Text("Your Stats")
                .font(.title)
                .fontWeight(.bold)
            
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Age (years)").font(.caption).foregroundColor(.secondary)
                    TextField("22", text: $ageText)
                        #if os(iOS)
                        .keyboardType(.numberPad)
                        #endif
                        .padding()
                        .background(Color.secondary.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                
                VStack(alignment: .leading, spacing: 8) {
                    Text("Height (cm)").font(.caption).foregroundColor(.secondary)
                    TextField("175", text: $heightText)
                        #if os(iOS)
                        .keyboardType(.numberPad)
                        #endif
                        .padding()
                        .background(Color.secondary.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }
            .padding(.horizontal, 40)
        }
    }
    
    private var countryStep: some View {
        VStack(spacing: 24) {
            Text("Nutrition Region")
                .font(.title)
                .fontWeight(.bold)
            
            Text("Bushy customizes meal calories & macros according to your culinary region.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            
            HStack(spacing: 12) {
                Text("🇮🇳")
                    .font(.system(size: 36))
                Text("India (ICMR / INDB Database)")
                    .fontWeight(.bold)
            }
            .padding()
            .frame(maxWidth: 320)
            .background(isMono ? Color.primary.opacity(0.1) : Color.accentColor.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }
}
