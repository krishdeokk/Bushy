package com.bushy.health

enum class PortionUnit(val displayName: String, val defaultGrams: Float) {
    PIECE("Piece", 40f),
    KATORI("Katori (Bowl)", 150f),
    CUP("Cup", 200f),
    TABLESPOON("Tbsp", 15f),
    GRAM("Grams (g)", 1f);

    companion object {
        fun fromString(name: String): PortionUnit {
            return entries.find { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                ?: KATORI
        }

        fun fromDisplayName(displayName: String): PortionUnit {
            return entries.find { it.displayName.equals(displayName, ignoreCase = true) || it.name.equals(displayName, ignoreCase = true) }
                ?: PIECE
        }
    }
}

enum class PrepStyle(val displayName: String, val calorieMultiplier: Float, val fatMultiplier: Float) {
    HOMEMADE("Homemade (Low Oil)", 0.9f, 0.8f),
    STANDARD("Standard Prep", 1.0f, 1.0f),
    RESTAURANT_RICH("Restaurant Rich (Ghee/Butter)", 1.25f, 1.4f);

    companion object {
        fun fromString(name: String): PrepStyle {
            return entries.find { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                ?: STANDARD
        }
    }
}

enum class AnalysisMethod(val displayName: String, val isLocalAiModel: Boolean, val description: String) {
    CLOUD_VISION_API(
        displayName = "High-Speed Gemini Vision API",
        isLocalAiModel = false,
        description = "Cloud Vision Multimodal Engine. Instant 100% accurate food photo recognition."
    ),
    LOCAL_HEURISTIC_COLOR_FEATURE(
        displayName = "Local Feature Analyzer (Simulated)",
        isLocalAiModel = false,
        description = "Color region feature analysis linked to Indian Nutrition Database. Transparent local heuristic pipeline."
    ),
    LOCAL_QWEN_VLM_PLACEHOLDER(
        displayName = "Local Qwen VLM (Pending Integration)",
        isLocalAiModel = true,
        description = "Local Qwen Vision model pipeline. Requires 6GB+ RAM and NDK runtime on-device."
    ),
    MANUAL_ENTRY(
        displayName = "Manual Meal Entry",
        isLocalAiModel = false,
        description = "Manually selected and configured foods from Indian Nutrition Database."
    )
}

data class NutrientBreakdown(
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float
) {
    operator fun plus(other: NutrientBreakdown): NutrientBreakdown {
        return NutrientBreakdown(
            calories = calories + other.calories,
            proteinG = proteinG + other.proteinG,
            carbsG = carbsG + other.carbsG,
            fatG = fatG + other.fatG
        )
    }
}

data class CalorieRange(
    val minCalories: Int,
    val maxCalories: Int,
    val assumptionsNote: String
)

data class EditableFoodItem(
    val id: String,
    val foodName: String,
    val confidenceScore: Float, // 0.0 to 1.0
    val quantity: Float,
    val unit: PortionUnit,
    val prepStyle: PrepStyle,
    val caloriesPer100g: Int,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val preparationNote: String = "",
    val originalUserText: String? = null,
    val detectedScript: String? = null
) {
    val totalWeightGrams: Float
        get() = quantity * unit.defaultGrams

    val calculatedNutrients: NutrientBreakdown
        get() {
            val weightRatio = totalWeightGrams / 100f
            val baseCal = (caloriesPer100g * weightRatio * prepStyle.calorieMultiplier).toInt()
            val baseProtein = proteinPer100g * weightRatio
            val baseCarbs = carbsPer100g * weightRatio
            val baseFat = fatPer100g * weightRatio * prepStyle.fatMultiplier
            return NutrientBreakdown(
                calories = baseCal.coerceAtLeast(0),
                proteinG = (baseProtein * 10f).toInt() / 10f,
                carbsG = (baseCarbs * 10f).toInt() / 10f,
                fatG = (baseFat * 10f).toInt() / 10f
            )
        }
}

data class MealAnalysisResult(
    val mealName: String,
    val items: List<EditableFoodItem>,
    val analysisMethod: AnalysisMethod,
    val isFoodDetected: Boolean = true,
    val detectionWarning: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalNutrients: NutrientBreakdown
        get() = items.fold(NutrientBreakdown(0, 0f, 0f, 0f)) { acc, item -> acc + item.calculatedNutrients }

    val calorieRange: CalorieRange
        get() {
            val totalCal = totalNutrients.calories
            if (totalCal == 0) {
                return CalorieRange(0, 0, "Zero Calorie Supplement / Pure Hydration (0 kcal).")
            }
            val minCal = (totalCal * 0.85f).toInt()
            val maxCal = (totalCal * 1.20f).toInt()
            val assumptions = "Estimates reflect standard Indian prep. Photo cannot determine exact oil/ghee usage (~85-120% range)."
            return CalorieRange(minCal, maxCal, assumptions)
        }
}
