import Foundation

public enum PortionUnit: String, Codable, CaseIterable, Identifiable {
    case piece = "Piece"
    case katori = "Katori (Bowl)"
    case cup = "Cup"
    case tablespoon = "Tbsp"
    case gram = "Grams (g)"
    
    public var id: String { rawValue }
    
    public var displayName: String { rawValue }
    
    public var defaultGrams: Double {
        switch self {
        case .piece: return 40.0
        case .katori: return 150.0
        case .cup: return 200.0
        case .tablespoon: return 15.0
        case .gram: return 1.0
        }
    }
    
    public static func fromString(_ name: String) -> PortionUnit {
        let clean = name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        for unit in PortionUnit.allCases {
            if unit.rawValue.lowercased() == clean || "\(unit)".lowercased() == clean {
                return unit
            }
        }
        return .katori
    }
}

public enum PrepStyle: String, Codable, CaseIterable, Identifiable {
    case homemade = "Homemade (Low Oil)"
    case standard = "Standard Prep"
    case restaurantRich = "Restaurant Rich (Ghee/Butter)"
    
    public var id: String { rawValue }
    public var displayName: String { rawValue }
    
    public var calorieMultiplier: Double {
        switch self {
        case .homemade: return 0.90
        case .standard: return 1.00
        case .restaurantRich: return 1.25
        }
    }
    
    public var fatMultiplier: Double {
        switch self {
        case .homemade: return 0.80
        case .standard: return 1.00
        case .restaurantRich: return 1.40
        }
    }
}

public enum AnalysisMethod: String, Codable {
    case localHeuristic = "Local Feature Analyzer (Simulated)"
    case localQwen = "Local Qwen VLM (Pending Integration)"
    case manualEntry = "Manual Meal Entry"
    
    public var displayName: String { rawValue }
    public var isLocalAiModel: Bool {
        self == .localQwen
    }
}

public struct NutrientBreakdown: Equatable, Codable {
    public var calories: Int
    public var proteinG: Double
    public var carbsG: Double
    public var fatG: Double
    
    public init(calories: Int = 0, proteinG: Double = 0.0, carbsG: Double = 0.0, fatG: Double = 0.0) {
        self.calories = calories
        self.proteinG = proteinG
        self.carbsG = carbsG
        self.fatG = fatG
    }
    
    public static func + (lhs: NutrientBreakdown, rhs: NutrientBreakdown) -> NutrientBreakdown {
        NutrientBreakdown(
            calories: lhs.calories + rhs.calories,
            proteinG: lhs.proteinG + rhs.proteinG,
            carbsG: lhs.carbsG + rhs.carbsG,
            fatG: lhs.fatG + rhs.fatG
        )
    }
}

public struct CalorieRange: Equatable, Codable {
    public var minCalories: Int
    public var maxCalories: Int
    public var assumptionsNote: String
}

public struct EditableFoodItem: Identifiable, Equatable, Codable {
    public var id: String
    public var foodName: String
    public var confidenceScore: Double // 0.0 to 1.0
    public var quantity: Double
    public var unit: PortionUnit
    public var prepStyle: PrepStyle
    public var caloriesPer100g: Int
    public var proteinPer100g: Double
    public var carbsPer100g: Double
    public var fatPer100g: Double
    public var preparationNote: String
    public var originalUserText: String?
    
    public init(
        id: String = UUID().uuidString,
        foodName: String,
        confidenceScore: Double = 0.90,
        quantity: Double = 1.0,
        unit: PortionUnit = .katori,
        prepStyle: PrepStyle = .standard,
        caloriesPer100g: Int,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        preparationNote: String = "",
        originalUserText: String? = nil
    ) {
        self.id = id
        self.foodName = foodName
        self.confidenceScore = confidenceScore
        self.quantity = quantity
        self.unit = unit
        self.prepStyle = prepStyle
        self.caloriesPer100g = caloriesPer100g
        self.proteinPer100g = proteinPer100g
        self.carbsPer100g = carbsPer100g
        self.fatPer100g = fatPer100g
        self.preparationNote = preparationNote
        self.originalUserText = originalUserText
    }
    
    public var totalWeightGrams: Double {
        quantity * unit.defaultGrams
    }
    
    public var calculatedNutrients: NutrientBreakdown {
        let weightRatio = totalWeightGrams / 100.0
        let baseCal = Int(Double(caloriesPer100g) * weightRatio * prepStyle.calorieMultiplier)
        let baseProtein = proteinPer100g * weightRatio
        let baseCarbs = carbsPer100g * weightRatio
        let baseFat = fatPer100g * weightRatio * prepStyle.fatMultiplier
        
        return NutrientBreakdown(
            calories: max(0, baseCal),
            proteinG: (baseProtein * 10).rounded() / 10,
            carbsG: (baseCarbs * 10).rounded() / 10,
            fatG: (baseFat * 10).rounded() / 10
        )
    }
}

public struct MealAnalysisResult: Identifiable, Equatable, Codable {
    public var id: String = UUID().uuidString
    public var mealName: String
    public var items: [EditableFoodItem]
    public var analysisMethod: AnalysisMethod
    public var isFoodDetected: Bool
    public var detectionWarning: String?
    public var timestamp: Date
    
    public init(
        mealName: String,
        items: [EditableFoodItem],
        analysisMethod: AnalysisMethod = .localHeuristic,
        isFoodDetected: Bool = true,
        detectionWarning: String? = nil,
        timestamp: Date = Date()
    ) {
        self.mealName = mealName
        self.items = items
        self.analysisMethod = analysisMethod
        self.isFoodDetected = isFoodDetected
        self.detectionWarning = detectionWarning
        self.timestamp = timestamp
    }
    
    public var totalNutrients: NutrientBreakdown {
        items.reduce(NutrientBreakdown()) { $0 + $1.calculatedNutrients }
    }
    
    public var calorieRange: CalorieRange {
        let totalCal = totalNutrients.calories
        if totalCal == 0 {
            return CalorieRange(minCalories: 0, maxCalories: 0, assumptionsNote: "Zero Calorie / Hydration")
        }
        let minCal = Int(Double(totalCal) * 0.85)
        let maxCal = Int(Double(totalCal) * 1.20)
        let note = "Reflects standard Indian preparation variance (~85-120% range due to oil/ghee usage)."
        return CalorieRange(minCalories: minCal, maxCalories: maxCal, assumptionsNote: note)
    }
}
