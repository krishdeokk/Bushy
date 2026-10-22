import SwiftUI

public struct MealAnalysisView: View {
    @ObservedObject var gameViewModel: GameViewModel
    @ObservedObject var aiViewModel: BushyAIViewModel
    @Environment(\.dismiss) private var dismiss
    
    @State private var inputText: String = ""
    @State private var selectedUnit: PortionUnit = .katori
    @State private var quantity: Double = 1.0
    @State private var selectedPrepStyle: PrepStyle = .standard
    @State private var currentItem: EditableFoodItem? = nil
    
    private var isMono: Bool {
        gameViewModel.uiState.visualStyle == .monochrome
    }
    
    public var body: some View {
        NavigationStack {
            ZStack {
                LiquidFluidBackground(isMonochrome: isMono)
                
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 20) {
                        // Header / Search Bar
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Search or Type Meal")
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(.secondary)
                            
                            HStack {
                                Image(systemName: "magnifyingglass")
                                    .foregroundColor(.secondary)
                                
                                TextField("e.g. Dal Makhani, Poha, 2 Rotis, Paneer...", text: $inputText)
                                    .onSubmit {
                                        analyzeDish()
                                    }
                                
                                if !inputText.isEmpty {
                                    Button {
                                        inputText = ""
                                    } label: {
                                        Image(systemName: "xmark.circle.fill")
                                            .foregroundColor(.secondary)
                                    }
                                }
                            }
                            .padding(14)
                            .liquidGlassCard(cornerRadius: 18)
                        }
                        
                        // Quick Suggestions
                        quickPills
                        
                        // Food Analysis Card
                        if let item = currentItem {
                            analysisCard(item)
                        } else {
                            emptyPrompt
                        }
                    }
                    .padding(20)
                }
            }
            .navigationTitle("Scan / Log Meal 🍲")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") {
                        dismiss()
                    }
                }
            }
        }
    }
    
    private var quickPills: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(["Dal Tadka", "Steamed Rice", "Roti", "Paneer Butter Masala", "Masala Dosa", "Poha", "Chicken Biryani"], id: \.self) { dish in
                    Button {
                        inputText = dish
                        analyzeDish()
                    } label: {
                        Text(dish)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .liquidGlassPill()
                    }
                }
            }
        }
    }
    
    private var emptyPrompt: some View {
        VStack(spacing: 16) {
            Image(systemName: "camera.viewfinder")
                .font(.system(size: 64))
                .foregroundColor(.secondary.opacity(0.5))
            
            Text("Multilingual Indian Nutrition AI")
                .font(.headline)
                .fontWeight(.bold)
            
            Text("Enter any Indian dish in Hindi, English, Tamil, Marathi, or Gujarati to get precise ICMR nutrition facts & macro breakdown.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
        }
        .padding(.vertical, 40)
    }
    
    private func analysisCard(_ item: EditableFoodItem) -> some View {
        let nutrients = item.calculatedNutrients
        
        return VStack(spacing: 16) {
            // Food Name & Confidence
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.foodName)
                        .font(.title3)
                        .fontWeight(.black)
                    
                    Text(item.preparationNote)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
                Text("\(Int(item.confidenceScore * 100))% Match")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundColor(isMono ? .primary : .green)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .liquidGlassPill()
            }
            
            Divider()
            
            // Portion Controls
            VStack(alignment: .leading, spacing: 10) {
                Text("Portion & Quantity")
                    .font(.subheadline)
                    .fontWeight(.bold)
                
                HStack(spacing: 12) {
                    Picker("Unit", selection: $selectedUnit) {
                        ForEach(PortionUnit.allCases) { unit in
                            Text(unit.displayName).tag(unit)
                        }
                    }
                    .pickerStyle(.menu)
                    .padding(8)
                    .liquidGlassCard(cornerRadius: 12)
                    
                    Spacer()
                    
                    HStack(spacing: 12) {
                        Button {
                            if quantity > 0.5 {
                                quantity -= 0.5
                                updateItem()
                            }
                        } label: {
                            Image(systemName: "minus.circle.fill")
                                .font(.title3)
                                .foregroundColor(.secondary)
                        }
                        
                        Text(String(format: "%.1f", quantity))
                            .font(.headline)
                            .fontWeight(.bold)
                            .frame(minWidth: 36)
                        
                        Button {
                            quantity += 0.5
                            updateItem()
                        } label: {
                            Image(systemName: "plus.circle.fill")
                                .font(.title3)
                                .foregroundColor(isMono ? .primary : .accentColor)
                        }
                    }
                }
                
                Text("Total Weight: ~\(Int(quantity * selectedUnit.defaultGrams))g")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            // Prep Style Picker
            VStack(alignment: .leading, spacing: 8) {
                Text("Preparation Style")
                    .font(.subheadline)
                    .fontWeight(.bold)
                
                #if os(iOS)
                Picker("Prep Style", selection: $selectedPrepStyle) {
                    ForEach(PrepStyle.allCases) { style in
                        Text(style.displayName).tag(style)
                    }
                }
                .pickerStyle(.segmented)
                .onChange(of: selectedPrepStyle) { updateItem() }
                .onChange(of: selectedUnit) { updateItem() }
                #else
                Picker("Prep Style", selection: $selectedPrepStyle) {
                    ForEach(PrepStyle.allCases) { style in
                        Text(style.displayName).tag(style)
                    }
                }
                .onChange(of: selectedPrepStyle) { updateItem() }
                .onChange(of: selectedUnit) { updateItem() }
                #endif
            }
            
            // Macro Breakdown Numbers
            VStack(spacing: 10) {
                HStack {
                    Text("Total Energy")
                        .font(.headline)
                        .fontWeight(.bold)
                    Spacer()
                    Text("\(nutrients.calories) kcal")
                        .font(.title2)
                        .fontWeight(.black)
                        .foregroundColor(isMono ? .primary : .accentColor)
                }
                
                HStack(spacing: 8) {
                    macroBadge(label: "Protein", val: "\(nutrients.proteinG)g", color: .purple)
                    macroBadge(label: "Carbs", val: "\(nutrients.carbsG)g", color: .orange)
                    macroBadge(label: "Fats", val: "\(nutrients.fatG)g", color: .blue)
                }
            }
            .padding(14)
            .liquidGlassCard(cornerRadius: 18)
            
            // Calorie Range Variance Note
            let minCal = Int(Double(nutrients.calories) * 0.85)
            let maxCal = Int(Double(nutrients.calories) * 1.20)
            Text("Range: \(minCal) - \(maxCal) kcal (accounts for varying oil/ghee usage)")
                .font(.caption2)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            // Log Button
            Button {
                HapticManager.notify(type: .success)
                gameViewModel.logMeal(
                    name: item.foodName,
                    calories: nutrients.calories,
                    protein: Int(nutrients.proteinG),
                    carbs: Int(nutrients.carbsG),
                    fat: Int(nutrients.fatG)
                )
                dismiss()
            } label: {
                HStack {
                    Image(systemName: "fork.knife")
                    Text("Log to Bushy Diary (+50 XP)")
                        .fontWeight(.bold)
                }
                .foregroundColor(isMono ? (gameViewModel.uiState.themeMode == .dark ? .black : .white) : .white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(
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
                .clipShape(Capsule())
                .overlay(Capsule().stroke(Color.white.opacity(0.3), lineWidth: 1))
                .shadow(color: isMono ? Color.black.opacity(0.15) : Color.accentColor.opacity(0.35), radius: 8, x: 0, y: 4)
            }
            .padding(.top, 4)
        }
        .padding(18)
        .liquidGlassCard(cornerRadius: 24)
    }
    
    private func macroBadge(label: String, val: String, color: Color) -> some View {
        VStack(spacing: 4) {
            Text(label)
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
            Text(val)
                .font(.subheadline)
                .fontWeight(.black)
                .foregroundColor(isMono ? .primary : color)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .liquidGlassCard(cornerRadius: 14)
    }
    
    private func analyzeDish() {
        guard !inputText.isEmpty else { return }
        let interp = MultilingualDishInterpreter.interpretDishText(inputText)
        if let match = interp.bestMatch {
            self.selectedUnit = match.defaultUnit
            self.quantity = 1.0
            self.selectedPrepStyle = .standard
            self.currentItem = EditableFoodItem(
                foodName: match.name,
                confidenceScore: interp.confidenceScore,
                quantity: 1.0,
                unit: match.defaultUnit,
                prepStyle: .standard,
                caloriesPer100g: match.caloriesPer100g,
                proteinPer100g: match.proteinPer100g,
                carbsPer100g: match.carbsPer100g,
                fatPer100g: match.fatPer100g,
                preparationNote: match.prepNotes,
                originalUserText: inputText
            )
        } else {
            self.selectedUnit = .katori
            self.quantity = 1.0
            self.selectedPrepStyle = .standard
            self.currentItem = EditableFoodItem(
                foodName: inputText.capitalized,
                confidenceScore: 0.65,
                quantity: 1.0,
                unit: .katori,
                prepStyle: .standard,
                caloriesPer100g: 150,
                proteinPer100g: 4.5,
                carbsPer100g: 22.0,
                fatPer100g: 5.0,
                preparationNote: "Estimated standard Indian dish profile",
                originalUserText: inputText
            )
        }
    }
    
    private func updateItem() {
        guard var item = currentItem else { return }
        item.unit = selectedUnit
        item.quantity = quantity
        item.prepStyle = selectedPrepStyle
        self.currentItem = item
    }
}
