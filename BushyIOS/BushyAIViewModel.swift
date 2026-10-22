import SwiftUI
import Combine

@MainActor
public final class BushyAIViewModel: ObservableObject {
    @Published public var messages: [BushyAIMessage] = []
    @Published public var isGenerating: Bool = false
    @Published public var activeMealAnalysisResult: MealAnalysisResult? = nil
    
    private let chatKey = "com.bushy.health.ai_chat_history"
    
    public init() {
        loadHistory()
    }
    
    private func loadHistory() {
        if let data = UserDefaults.standard.data(forKey: chatKey),
           let saved = try? JSONDecoder().decode([BushyAIMessage].self, from: data) {
            self.messages = saved
        } else {
            self.messages = [
                BushyAIMessage(text: "Namaste! I'm your Bushy AI health companion ☁️. You can ask me for diet advice, workout tips, or tell me 'Give me a quest to do 25 pushups' and I'll create it!", isUser: false)
            ]
        }
    }
    
    public func saveHistory() {
        if let data = try? JSONEncoder().encode(messages) {
            UserDefaults.standard.set(data, forKey: chatKey)
        }
    }
    
    public func sendMessage(
        _ text: String,
        userStats: UserStats,
        onDeployTask: ((String, Int, TaskType) -> Void)? = nil
    ) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        
        // Add user message
        messages.append(BushyAIMessage(text: trimmed, isUser: true))
        saveHistory()
        
        // 1. Check if user prompt is a task request
        if let parsedTask = tryParseTaskLocal(trimmed) {
            isGenerating = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { [weak self] in
                guard let self = self else { return }
                self.isGenerating = false
                let botText = "Quest Accepted! 🎯 I've created your '\(parsedTask.title)' quest with a target of \(parsedTask.target) \(parsedTask.type.unitLabel). Go get that XP!"
                self.messages.append(BushyAIMessage(text: botText, isUser: false))
                self.saveHistory()
                onDeployTask?(parsedTask.title, parsedTask.target, parsedTask.type)
            }
            return
        }
        
        // 2. Generate contextual response
        isGenerating = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.9) { [weak self] in
            guard let self = self else { return }
            self.isGenerating = false
            let response = self.generateSmartResponse(query: trimmed, stats: userStats)
            self.messages.append(BushyAIMessage(text: response, isUser: false))
            self.saveHistory()
        }
    }
    
    // MARK: - Smart Task Parser
    
    private func tryParseTaskLocal(_ text: String) -> (title: String, target: Int, type: TaskType)? {
        let lower = text.lowercased()
        
        // Extract numbers
        let numbers = text.components(separatedBy: CharacterSet.decimalDigits.inverted).filter { !$0.isEmpty }
        let target = numbers.compactMap { Int($0) }.first ?? 10
        
        if lower.contains("step") || lower.contains("walk") || lower.contains("run") {
            let stepTarget = (target < 500) ? target * 1000 : target
            return ("Step Quest", stepTarget, .steps)
        } else if lower.contains("pushup") || lower.contains("push up") || lower.contains("push-up") {
            return ("Power Pushups", max(target, 10), .pushups)
        } else if lower.contains("water") || lower.contains("drink") || lower.contains("hydrate") {
            return ("Hydrate Quest", max(target, 8), .water)
        } else if lower.contains("squat") || lower.contains("squats") {
            return ("Squat Power", max(target, 20), .general)
        } else if lower.contains("quest") || lower.contains("mission") || lower.contains("target") {
            return ("Daily Challenge", max(target, 15), .general)
        }
        
        return nil
    }
    
    // MARK: - Smart AI Response Engine
    
    private func generateSmartResponse(query: String, stats: UserStats) -> String {
        let q = query.lowercased()
        
        // Food & Diet queries
        if q.contains("breakfast") {
            return """
            🥞 Great Indian breakfast options for high energy & protein:
            1. Poha with roasted peanuts & boiled sprouts (180 kcal, 7g protein)
            2. 2 Besan / Moong Dal Chillas with mint chutney (210 kcal, 12g protein)
            3. 3 Steamed Idlis with hot Sambar (230 kcal, 9g protein)
            4. Paneer bhurji with 1 whole wheat roti (280 kcal, 16g protein)
            """
        }
        
        if q.contains("protein") {
            return """
            💪 Top vegetarian & non-veg Indian protein sources:
            • Paneer (18g per 100g)
            • Soya Chunks (52g per 100g raw)
            • Greek Yogurt / Hung Curd (10g per 100g)
            • Toor / Moong / Chana Dal (24g per 100g raw)
            • Chicken Breast (31g per 100g) & Eggs (6g per whole egg)
            Current logged protein today: \(stats.totalProteinGrams)g. Keep crushing it!
            """
        }
        
        if q.contains("calorie") || q.contains("burn") {
            return "🔥 Today you've burned approximately \(stats.calories) kcal across \(stats.steps) steps! At Level \(stats.level), your metabolism is firing up."
        }
        
        if q.contains("workout") || q.contains("exercise") {
            return """
            ⚡ Quick 15-Minute Home Circuit:
            • 20 Pushups
            • 25 Bodyweight Squats
            • 30-second Plank
            • 30 Jumping Jacks
            Repeat for 3 rounds! Tell me "create a pushup quest" anytime to track it!
            """
        }
        
        // Dish check with MultilingualDishInterpreter
        let interp = MultilingualDishInterpreter.interpretDishText(query)
        if let match = interp.bestMatch {
            return """
            🍛 Nutrition for \(match.name):
            • Calories: \(match.caloriesPer100g) kcal per 100g
            • Protein: \(match.proteinPer100g)g
            • Carbs: \(match.carbsPer100g)g
            • Fats: \(match.fatPer100g)g
            • Default Portion: 1 \(match.defaultUnit.displayName) (~\(Int(match.defaultUnitWeightGrams))g)
            💡 \(match.prepNotes)
            """
        }
        
        return "I'm right here with you! You are currently Level \(stats.level) with a \(stats.streakDays)-day streak 🔥. Ask me about Indian meals, nutrition, or ask me to set a fitness mission for you!"
    }
    
    // MARK: - Meal Analysis & Scanning
    
    public func analyzeDishText(_ text: String) {
        isGenerating = true
        let interp = MultilingualDishInterpreter.interpretDishText(text)
        
        let item: EditableFoodItem
        if let match = interp.bestMatch {
            item = EditableFoodItem(
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
                originalUserText: text
            )
        } else {
            // General food fallback
            item = EditableFoodItem(
                foodName: text.capitalized,
                confidenceScore: 0.60,
                quantity: 1.0,
                unit: .katori,
                prepStyle: .standard,
                caloriesPer100g: 150,
                proteinPer100g: 4.5,
                carbsPer100g: 22.0,
                fatPer100g: 5.0,
                preparationNote: "Estimated based on mixed Indian dish profile",
                originalUserText: text
            )
        }
        
        let result = MealAnalysisResult(
            mealName: item.foodName,
            items: [item],
            analysisMethod: .localHeuristic,
            isFoodDetected: true
        )
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.isGenerating = false
            self?.activeMealAnalysisResult = result
        }
    }
    
    public func confirmAndSaveMeal(
        result: MealAnalysisResult,
        gameViewModel: GameViewModel
    ) {
        let totals = result.totalNutrients
        gameViewModel.logMeal(
            name: result.mealName,
            calories: totals.calories,
            protein: Int(totals.proteinG),
            carbs: Int(totals.carbsG),
            fat: Int(totals.fatG)
        )
        
        let botText = "🍽️ Logged meal: \(result.mealName) (+50 XP)!\nTotal: \(totals.calories) kcal • \(Int(totals.proteinG))g Protein • \(Int(totals.carbsG))g Carbs • \(Int(totals.fatG))g Fat"
        messages.append(BushyAIMessage(text: botText, isUser: false))
        saveHistory()
        activeMealAnalysisResult = nil
    }
    
    public func clearActiveAnalysis() {
        activeMealAnalysisResult = nil
    }
}
