import SwiftUI

public struct SetupScreen: View {
    @ObservedObject var viewModel: HealthViewModel
    
    @State private var currentStep = 0
    @State private var name: String = ""
    @State private var ageText: String = "22"
    @State private var heightText: String = "175"
    @State private var selectedGender: AvatarType = .male
    @State private var selectedThemeMode: ThemeMode = .system
    @State private var selectedCountry: String = "India"
    
    public init(viewModel: HealthViewModel) {
        self.viewModel = viewModel
    }
    
    public var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(white: 0.12),
                    Color.bushyAppBackground
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    // Header / Step progress
                    HStack(spacing: 6) {
                        ForEach(0..<6) { index in
                            Capsule()
                                .fill(index <= currentStep ? Color.primary : Color.secondary.opacity(0.2))
                                .frame(height: 5)
                        }
                    }
                    .padding(.horizontal, 32)
                    .padding(.top, 16)
                    
                    // Step Content
                    Group {
                        switch currentStep {
                        case 0:
                            permissionStep
                        case 1:
                            genderStep
                        case 2:
                            themeStep
                        case 3:
                            nameStep
                        case 4:
                            statsStep
                        case 5:
                            countryStep
                        default:
                            EmptyView()
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .transition(.opacity.combined(with: .scale(scale: 0.96)))
                    .animation(.easeInOut(duration: 0.25), value: currentStep)
                    
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
                            if currentStep < 5 {
                                withAnimation { currentStep += 1 }
                            } else {
                                let age = Int(ageText) ?? 22
                                let height = Int(heightText) ?? 175
                                viewModel.setAvatar(selectedGender)
                                viewModel.setVisualStyle(.monochrome)
                                viewModel.setThemeMode(selectedThemeMode)
                                viewModel.setCountry(selectedCountry)
                                viewModel.completeSetup(name: name.isEmpty ? "Hero" : name, age: age, height: height)
                            }
                        } label: {
                            HStack {
                                Text(currentStep == 5 ? "Let's Begin!" : "Continue")
                                    .fontWeight(.bold)
                                Image(systemName: "arrow.right")
                            }
                            .foregroundColor(Color.bushyAppBackground)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(Color.primary)
                            .clipShape(Capsule())
                            .overlay(
                                Capsule().stroke(Color.white.opacity(0.35), lineWidth: 1.2)
                            )
                        }
                    }
                    .padding(.horizontal, 32)
                    .padding(.bottom, 24)
                }
                .frame(maxWidth: 540)
                .frame(minHeight: 520)
                .padding(.horizontal, 16)
            }
        }
    }
    
    // MARK: - Step 0: Permission
    
    private var permissionStep: some View {
        VStack(spacing: 20) {
            Image(systemName: "heart.text.square.fill")
                .font(.system(size: 72))
                .foregroundColor(.primary)
            
            Text("Welcome to Bushy ☁️")
                .font(.title)
                .fontWeight(.black)
            
            Text("Bushy synchronizes with Apple Health directly on your device to convert physical movement into XP and level up your companion.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            
            Button {
                viewModel.checkHealthKit()
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "applelogo")
                    Text("Connect Apple Health")
                        .fontWeight(.bold)
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 14)
                .liquidGlassPill()
            }
        }
    }
    
    // MARK: - Step 1: Gender / Persona
    
    private var genderStep: some View {
        VStack(spacing: 20) {
            Text("Select Character Persona")
                .font(.title2)
                .fontWeight(.black)
            
            Text("Choose the morphology and personality for your companion")
                .font(.caption)
                .foregroundColor(.secondary)
            
            HStack(spacing: 16) {
                ForEach(AvatarType.allCases) { type in
                    Button {
                        selectedGender = type
                    } label: {
                        VStack(spacing: 12) {
                            Image(systemName: type == .female ? "person.crop.circle.badge.fill" : "person.crop.circle.fill")
                                .font(.system(size: 36))
                                .foregroundColor(.primary)
                            
                            Text(type.displayName)
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 24)
                        .liquidGlassCard(cornerRadius: 22)
                        .overlay(
                            RoundedRectangle(cornerRadius: 22)
                                .stroke(selectedGender == type ? Color.primary : Color.clear, lineWidth: 2)
                        )
                    }
                }
            }
            .padding(.horizontal, 20)
        }
    }
    
    // MARK: - Step 2: Theme
    
    private var themeStep: some View {
        VStack(spacing: 20) {
            Text("Appearance Theme")
                .font(.title2)
                .fontWeight(.black)
            
            Text("Bushy features a dedicated high-contrast monochrome aesthetic")
                .font(.caption)
                .foregroundColor(.secondary)
            
            HStack(spacing: 14) {
                themeButton(.system, label: "System", icon: "gearshape")
                themeButton(.dark, label: "Noir", icon: "moon.fill")
                themeButton(.light, label: "Clean", icon: "sun.max.fill")
            }
            .padding(.horizontal, 20)
        }
    }
    
    private func themeButton(_ mode: ThemeMode, label: String, icon: String) -> some View {
        Button {
            selectedThemeMode = mode
        } label: {
            VStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(.primary)
                Text(label)
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 20)
            .liquidGlassCard(cornerRadius: 18)
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(selectedThemeMode == mode ? Color.primary : Color.clear, lineWidth: 2)
            )
        }
    }
    
    // MARK: - Step 3: Name
    
    private var nameStep: some View {
        VStack(spacing: 20) {
            Text("What should we call you?")
                .font(.title2)
                .fontWeight(.black)
            
            TextField("Enter your name", text: $name)
                .font(.title3)
                .multilineTextAlignment(.center)
                .padding(18)
                .liquidGlassCard(cornerRadius: 20)
                .padding(.horizontal, 32)
        }
    }
    
    // MARK: - Step 4: Stats
    
    private var statsStep: some View {
        VStack(spacing: 20) {
            Text("Your Baseline Stats")
                .font(.title2)
                .fontWeight(.black)
            
            Text("Used for calculating custom BMR and XP multipliers")
                .font(.caption)
                .foregroundColor(.secondary)
            
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Age (years)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    TextField("22", text: $ageText)
                        .font(.title3)
                        .keyboardType(.numberPad)
                        .padding(14)
                        .liquidGlassCard(cornerRadius: 16)
                }
                
                VStack(alignment: .leading, spacing: 6) {
                    Text("Height (cm)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    TextField("175", text: $heightText)
                        .font(.title3)
                        .keyboardType(.numberPad)
                        .padding(14)
                        .liquidGlassCard(cornerRadius: 16)
                }
            }
            .padding(.horizontal, 32)
        }
    }
    
    // MARK: - Step 5: Country
    
    private var countryStep: some View {
        VStack(spacing: 20) {
            Text("Culinary & Nutrition Region")
                .font(.title2)
                .fontWeight(.black)
            
            Text("Optimizes the food dictionary for your regional cuisine")
                .font(.caption)
                .foregroundColor(.secondary)
            
            VStack(spacing: 12) {
                ForEach(["India", "United States", "United Kingdom", "Other"], id: \.self) { c in
                    Button {
                        selectedCountry = c
                    } label: {
                        HStack {
                            Text(c)
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                            Spacer()
                            if selectedCountry == c {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.primary)
                                    .fontWeight(.bold)
                            }
                        }
                        .padding(16)
                        .liquidGlassCard(cornerRadius: 18)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(selectedCountry == c ? Color.primary : Color.clear, lineWidth: 2)
                        )
                    }
                }
            }
            .padding(.horizontal, 32)
        }
    }
}
