import SwiftUI

public struct SetupScreen: View {
    @ObservedObject var viewModel: GameViewModel
    
    @State private var currentStep = 0
    @State private var name: String = ""
    @State private var ageText: String = "22"
    @State private var heightText: String = "175"
    @State private var selectedGender: AvatarType = .male
    @State private var selectedThemeMode: ThemeMode = .system
    @State private var selectedCountry: String = "India"
    
    public init(viewModel: GameViewModel) {
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
                
                Spacer()
                
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
                            Text(currentStep == 5 ? "Let's Play!" : "Continue")
                                .fontWeight(.bold)
                            Image(systemName: "arrow.right")
                        }
                        .foregroundColor(Color.bushyAppBackground)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(Color.primary)
                        .clipShape(Capsule())
                    }
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 24)
            }
        }
    }
    
    // MARK: - Steps
    
    private var permissionStep: some View {
        VStack(spacing: 20) {
            Image(systemName: "heart.text.square.fill")
                .font(.system(size: 80))
                .foregroundColor(.primary)
            
            Text("Apple Health Sync")
                .font(.title)
                .fontWeight(.black)
            
            Text("Bushy reads your steps and active energy burned from Apple Health to level up your Bloub avatar automatically.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
    }
    
    private var genderStep: some View {
        VStack(spacing: 24) {
            Text("Choose Your Avatar")
                .font(.title)
                .fontWeight(.bold)
            
            HStack(spacing: 20) {
                ForEach([AvatarType.male, AvatarType.female], id: \.self) { type in
                    Button {
                        selectedGender = type
                    } label: {
                        VStack(spacing: 12) {
                            BloubAvatarView(
                                expression: .happy,
                                visualStyle: .monochrome,
                                themeMode: selectedThemeMode
                            )
                            .frame(width: 120, height: 120)
                            
                            Text(type.displayName)
                                .fontWeight(.bold)
                                .foregroundColor(selectedGender == type ? .primary : .secondary)
                        }
                        .padding()
                        .frame(width: 140, height: 180)
                        .background(selectedGender == type ? Color.primary.opacity(0.12) : Color.secondary.opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .overlay(
                            RoundedRectangle(cornerRadius: 24)
                                .stroke(selectedGender == type ? Color.primary : Color.clear, lineWidth: 2)
                        )
                    }
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
                                    .foregroundColor(.primary)
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
            .background(Color.secondary.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }
}
