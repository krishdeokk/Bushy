import SwiftUI
import Combine

@MainActor
public final class GameViewModel: ObservableObject {
    @Published public var uiState: UserStats = UserStats()
    
    private let statsKey = "com.bushy.health.user_stats"
    private var tapTimestamps: [Date] = []
    private var tapCycleIndex = 0
    private let tapCycleExpressions: [AvatarExpression] = [.surprised, .excited, .happy, .neutral]
    
    public init() {
        loadStats()
        updateStreak()
        checkHealthKit()
    }
    
    // MARK: - Persistence
    
    private func loadStats() {
        if let data = UserDefaults.standard.data(forKey: statsKey),
           var saved = try? JSONDecoder().decode(UserStats.self, from: data) {
            saved.visualStyle = .monochrome
            self.uiState = saved
        } else {
            // Default initial state
            self.uiState = UserStats(
                userName: "",
                age: 22,
                height: 175,
                isSetupComplete: false,
                themeMode: .system,
                visualStyle: .monochrome,
                steps: 0,
                pushups: 0,
                xp: 0,
                avatarType: .male,
                expression: .neutral,
                syncMessage: nil,
                showChangelog: false,
                tasks: [
                    HealthTask(id: "starter_steps", title: "Daily Walk", target: 5000, current: 0, type: .steps),
                    HealthTask(id: "starter_pushups", title: "Power Pushups", target: 20, current: 0, type: .pushups),
                    HealthTask(id: "starter_water", title: "Hydrate Hero", target: 8, current: 0, type: .water)
                ],
                bushyAIHistory: [
                    BushyAIMessage(text: "Hey! I'm Bushy ☁️. Let's crush your fitness goals together today!", isUser: false)
                ],
                newlyAddedTaskId: nil,
                country: "India",
                bonusCalories: 0,
                loggedMeals: [],
                streakDays: 1,
                lastActiveDate: todayDateString()
            )
        }
    }
    
    public func saveStats() {
        if let data = try? JSONEncoder().encode(uiState) {
            UserDefaults.standard.set(data, forKey: statsKey)
        }
    }
    
    // MARK: - Streak
    
    private func todayDateString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }
    
    private func updateStreak() {
        let today = todayDateString()
        let lastDate = uiState.lastActiveDate
        
        if lastDate.isEmpty {
            uiState.lastActiveDate = today
            uiState.streakDays = 1
            saveStats()
            return
        }
        
        if lastDate == today {
            // Already updated today
            return
        }
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        if let last = formatter.date(from: lastDate),
           let diff = Calendar.current.dateComponents([.day], from: last, to: Date()).day {
            if diff == 1 {
                uiState.streakDays += 1
            } else if diff > 1 {
                uiState.streakDays = 1
            }
        }
        uiState.lastActiveDate = today
        saveStats()
    }
    
    // MARK: - HealthKit Integration
    
    public func checkHealthKit() {
        HealthManager.shared.requestAuthorization { [weak self] success, _ in
            guard success else { return }
            self?.refreshStats()
        }
    }
    
    public func refreshStats() {
        HealthManager.shared.fetchTodaySteps { [weak self] steps in
            guard let self = self else { return }
            self.uiState.steps = steps
            self.updateTaskProgress(type: .steps, value: steps)
            self.recalculateXP()
            self.saveStats()
        }
    }
    
    // MARK: - Gamification & XP
    
    public func addXP(_ amount: Int) {
        let oldLevel = uiState.level
        uiState.xp += amount
        let newLevel = uiState.level
        
        if newLevel > oldLevel {
            // Level up animation celebration!
            HapticManager.notify(type: .success)
            setExpressionDirectly(.celebrating)
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) { [weak self] in
                self?.setExpressionDirectly(.neutral)
            }
        }
        saveStats()
    }
    
    private func recalculateXP() {
        // Base XP from physical movement: 1 step = 0.05 XP, 1 cal = 0.2 XP
        let movementXP = Int(Double(uiState.steps) * 0.05) + Int(Double(uiState.calories) * 0.2)
        // Pushups: 5 XP each
        let pushupXP = uiState.pushups * 5
        // Tasks completed: 500 XP each
        let completedTasksXP = uiState.tasks.filter { $0.isCompleted }.count * 500
        
        uiState.xp = movementXP + pushupXP + completedTasksXP
    }
    
    // MARK: - Tasks / Quests
    
    public func addNewTask(title: String, target: Int, type: TaskType) {
        let task = HealthTask(
            id: UUID().uuidString,
            title: title,
            target: target,
            current: 0,
            type: type
        )
        uiState.tasks.append(task)
        uiState.newlyAddedTaskId = task.id
        saveStats()
        
        // Brief reaction
        setExpressionDirectly(.excited)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) { [weak self] in
            self?.setExpressionDirectly(.neutral)
        }
    }
    
    public func singleTapIncrement(_ taskId: String) {
        guard let index = uiState.tasks.firstIndex(where: { $0.id == taskId }) else { return }
        var task = uiState.tasks[index]
        guard !task.isCompleted else { return }
        
        task.current += 1
        if task.type == .pushups {
            uiState.pushups += 1
        }
        
        if task.current >= task.target {
            task.isCompleted = true
            addXP(500)
            HapticManager.notify(type: .success)
            setExpressionDirectly(.celebrating)
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
                self?.setExpressionDirectly(.neutral)
            }
        } else {
            addXP(10)
            setExpressionDirectly(.workingOut)
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) { [weak self] in
                self?.setExpressionDirectly(.neutral)
            }
        }
        
        uiState.tasks[index] = task
        saveStats()
    }
    
    public func resetTask(_ taskId: String) {
        guard let index = uiState.tasks.firstIndex(where: { $0.id == taskId }) else { return }
        uiState.tasks[index].current = 0
        uiState.tasks[index].isCompleted = false
        saveStats()
    }
    
    public func deleteTask(_ taskId: String) {
        uiState.tasks.removeAll { $0.id == taskId }
        saveStats()
    }
    
    private func updateTaskProgress(type: TaskType, value: Int) {
        for i in 0..<uiState.tasks.count {
            if uiState.tasks[i].type == type {
                uiState.tasks[i].current = min(value, uiState.tasks[i].target)
                if uiState.tasks[i].current >= uiState.tasks[i].target {
                    if !uiState.tasks[i].isCompleted {
                        uiState.tasks[i].isCompleted = true
                        addXP(500)
                    }
                }
            }
        }
    }
    
    // MARK: - Nutrition & Meals
    
    public func logMeal(
        name: String,
        calories: Int,
        proteinGrams: Int,
        carbsGrams: Int,
        fatGrams: Int,
        country: String = "India"
    ) {
        let meal = LoggedMeal(
            name: name,
            calories: calories,
            proteinGrams: proteinGrams,
            carbsGrams: carbsGrams,
            fatGrams: fatGrams,
            country: country
        )
        uiState.loggedMeals.append(meal)
        uiState.bonusCalories += calories / 4 // Bonus gamified burn quota
        addXP(50)
        setExpressionDirectly(.happy)
        saveStats()
    }
    
    public func logMeal(
        name: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        country: String = "India"
    ) {
        logMeal(
            name: name,
            calories: calories,
            proteinGrams: protein,
            carbsGrams: carbs,
            fatGrams: fat,
            country: country
        )
    }
    
    public func deleteMeal(_ mealId: String) {
        if let meal = uiState.loggedMeals.first(where: { $0.id == mealId }) {
            uiState.bonusCalories = max(0, uiState.bonusCalories - meal.calories / 4)
            uiState.loggedMeals.removeAll { $0.id == mealId }
            saveStats()
        }
    }
    
    // MARK: - Avatar Reactions
    
    public func onAvatarTap() {
        let now = Date()
        tapTimestamps.append(now)
        // Keep only taps in last 1.5 seconds
        tapTimestamps = tapTimestamps.filter { now.timeIntervalSince($0) <= 1.5 }
        
        if tapTimestamps.count >= 5 {
            // Angry reaction!
            tapTimestamps.removeAll()
            setExpressionDirectly(.angry)
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
                self?.setExpressionDirectly(.neutral)
            }
            return
        }
        
        // Cycle expressions
        tapCycleIndex = (tapCycleIndex + 1) % tapCycleExpressions.count
        setExpressionDirectly(tapCycleExpressions[tapCycleIndex])
    }
    
    public func onAvatarDoubleTap() {
        setExpressionDirectly(.laughing)
        HapticManager.impact(style: .heavy)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            self?.setExpressionDirectly(.neutral)
        }
    }
    
    public func setExpressionDirectly(_ expr: AvatarExpression) {
        uiState.expression = expr
    }
    
    public func setAvatar(_ type: AvatarType) {
        uiState.avatarType = type
        saveStats()
    }
    
    public func setVisualStyle(_ style: VisualStyle) {
        uiState.visualStyle = .monochrome
        saveStats()
    }
    
    public func setThemeMode(_ mode: ThemeMode) {
        uiState.themeMode = mode
        saveStats()
    }
    
    public func setCountry(_ country: String) {
        uiState.country = country
        saveStats()
    }
    
    public func completeSetup(name: String, age: Int, height: Int) {
        uiState.userName = name
        uiState.age = age
        uiState.height = height
        uiState.isSetupComplete = true
        saveStats()
    }
    
    public func resetAllStats() {
        UserDefaults.standard.removeObject(forKey: statsKey)
        loadStats()
    }
}
