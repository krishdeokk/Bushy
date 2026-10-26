import SwiftUI

public struct MealAnalysisScreen: View {
    @ObservedObject var healthViewModel: HealthViewModel
    @ObservedObject var aiViewModel: BushyAIViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    
    @State private var inputText: String = ""
    @State private var currentItem: EditableFoodItem? = nil
    @State private var selectedUnit: PortionUnit = .katori
    @State private var quantity: Double = 1.0
    @State private var selectedPrepStyle: PrepStyle = .homemade
    
    public init(healthViewModel: HealthViewModel, aiViewModel: BushyAIViewModel) {
        self.healthViewModel = healthViewModel
        self.aiViewModel = aiViewModel
    }
    
    public var body: some View {
        NavigationStack {
            GeometryReader { geo in
                let isDuo = DuoSplitLayoutHelper.isDuoLayout(size: geo.size, horizontalSizeClass: horizontalSizeClass)
                
                ZStack {
                    LiquidFluidBackground(isMonochrome: true)
                    
                    if isDuo {
                        duoSplitBody(geo: geo)
                    } else {
                        standardPortraitBody
                    }
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
    
    // MARK: - Standard Single Column Body
    
    private var standardPortraitBody: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 20) {
                searchCard
                
                quickPillsHorizontal
                
                if let item = currentItem {
                    analysisCard(item)
                } else {
                    emptyPrompt
                }
            }
            .padding(20)
        }
    }
    
    // MARK: - Duo Split Screen Body
    
    private func duoSplitBody(geo: GeometryProxy) -> some View {
        HStack(alignment: .top, spacing: 20) {
            // Left Column: Search & Quick Dishes & Guide
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 16) {
                    searchCard
                    
                    Text("Popular Indian Dishes")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.secondary)
                    
                    quickPillsVertical
                    
                    icmrGuideCard
                }
                .padding(.vertical, 16)
            }
            .frame(maxWidth: min(geo.size.width * 0.42, 360))
            
            // Right Column: Analysis Result / Empty Prompt
            ScrollView(showsIndicators: false) {
                VStack(spacing: 16) {
                    if let item = currentItem {
                        analysisCard(item)
                    } else {
                        emptyPrompt
                    }
                }
                .padding(.vertical, 16)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 24)
    }
    
    // MARK: - Search Card
    
    private var searchCard: some View {
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
    }
    
    // MARK: - Quick Pills
    
    private let popularDishes = ["Dal Tadka", "Steamed Rice", "Roti", "Paneer Butter Masala", "Masala Dosa", "Poha", "Chicken Biryani"]
    
    private var quickPillsHorizontal: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(popularDishes, id: \.self) { dish in
                    dishChip(dish)
                }
            }
        }
    }
    
    private var quickPillsVertical: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
            ForEach(popularDishes, id: \.self) { dish in
                dishChip(dish)
            }
        }
    }
    
    private func dishChip(_ dish: String) -> some View {
        Button {
            inputText = dish
            analyzeDish()
        } label: {
            Text(dish)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.primary)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .liquidGlassPill()
        }
    }
    
    private var icmrGuideCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Image(systemName: "info.circle.fill")
                    .foregroundColor(.primary)
                Text("ICMR 2024 Guidelines")
                    .font(.caption)
                    .fontWeight(.bold)
            }
            Text("Calculations consider home-cooking fat variations (tadka, ghee, frying) with standard Indian portion metrics (katori, pieces).")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding(14)
        .liquidGlassCard(cornerRadius: 16)
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
                    .foregroundColor(.primary)
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
                                .foregroundColor(.primary)
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
                        .foregroundColor(.primary)
                }
                
                HStack(spacing: 8) {
                    macroBadge(label: "Protein", val: "\(nutrients.proteinG)g")
                    macroBadge(label: "Carbs", val: "\(nutrients.carbsG)g")
                    macroBadge(label: "Fats", val: "\(nutrients.fatG)g")
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
                healthViewModel.logMeal(
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
                .foregroundColor(Color.bushyAppBackground)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color.primary)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(Color.white.opacity(0.3), lineWidth: 1))
                .shadow(color: Color.black.opacity(0.15), radius: 8, x: 0, y: 4)
            }
            .padding(.top, 4)
        }
        .padding(18)
        .liquidGlassCard(cornerRadius: 24)
    }
    
    private func macroBadge(label: String, val: String) -> some View {
        VStack(spacing: 4) {
            Text(label)
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
            
            Text(val)
                .font(.headline)
                .fontWeight(.black)
                .foregroundColor(.primary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(Color.secondary.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
    
    private func analyzeDish() {
        let trimmed = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        
        let item = MultilingualDishInterpreter.interpret(userInput: trimmed)
        self.currentItem = item
        self.selectedUnit = item.unit
        self.quantity = item.quantity
        self.selectedPrepStyle = item.prepStyle
    }
    
    private func updateItem() {
        guard var item = currentItem else { return }
        item.unit = selectedUnit
        item.quantity = quantity
        item.prepStyle = selectedPrepStyle
        self.currentItem = item
    }
}

// Backwards-compatible alias for existing call-sites
public typealias MealAnalysisView = MealAnalysisScreen
