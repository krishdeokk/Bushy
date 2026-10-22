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
           let saved = try? JSONDecoder().decode(UserStats.self, from: data) {
            self.uiState = saved
        } else {
            // Default initial state
            self.uiState = UserStats(
                userName: "",
                age: 22,
                height: 175,
                isSetupComplete: false,
                themeMode: .system,
                visualStyle: .material3,
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
    
    // MARK: - HealthKit Sync
    
    public func checkHealthKit() {
        Task {
            let steps = await HealthManager.shared.fetchTodaySteps()
            if steps > 0 {
                await MainActor.run {
                    self.syncSteps(steps)
                }
            }
        }
    }
    
    public func syncSteps(_ steps: Int) {
        let diff = max(0, steps - uiState.steps)
        uiState.steps = steps
        
        // Update starter steps task
        if let index = uiState.tasks.firstIndex(where: { $0.type == .steps }) {
            uiState.tasks[index].current = min(steps, uiState.tasks[index].target)
            if uiState.tasks[index].current >= uiState.tasks[index].target && !uiState.tasks[index].isCompleted {
                uiState.tasks[index].isCompleted = true
                addXP(500)
            }
        }
        
        if diff > 0 {
            addXP(diff / 100 * 10)
        }
        saveStats()
    }
    
    public func refreshStats() {
        Task {
            let granted = await HealthManager.shared.requestPermissions()
            if granted {
                let steps = await HealthManager.shared.fetchTodaySteps()
                await MainActor.run {
                    self.syncSteps(steps)
                    self.uiState.syncMessage = "Synced with Apple Health! 🏃"
                    self.saveStats()
                }
            } else {
                await MainActor.run {
                    self.uiState.syncMessage = "HealthKit permission not granted"
                    self.saveStats()
                }
            }
        }
    }
    
    // MARK: - XP & Progression
    
    public func addXP(_ amount: Int) {
        guard amount > 0 else { return }
        let oldLevel = uiState.level
        uiState.xp += amount
        let newLevel = uiState.level
        
        if newLevel > oldLevel {
            // Level up celebratory moment!
            setExpressionDirectly(.celebrating)
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) { [weak self] in
                self?.setExpressionDirectly(.happy)
            }
        }
        saveStats()
    }
    
    // MARK: - Task Operations
    
    public func singleTapIncrement(_ taskId: String) {
        guard let index = uiState.tasks.firstIndex(where: { $0.id == taskId }) else { return }
        var task = uiState.tasks[index]
        if task.current < task.target {
            task.current += 1
            if task.type == .pushups {
                uiState.pushups += 1
            }
            if task.current >= task.target && !task.isCompleted {
                task.isCompleted = true
                addXP(500) // Quest complete bonus
                setExpressionDirectly(.celebrating)
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
                    self?.setExpressionDirectly(.happy)
                }
            } else {
                addXP(10)
                setExpressionDirectly(.workingOut)
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) { [weak self] in
                    self?.setExpressionDirectly(.happy)
                }
            }
            uiState.tasks[index] = task
            saveStats()
        }
    }
    
    public func incrementTask(_ taskId: String) {
        singleTapIncrement(taskId)
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
    
    public func addNewTask(title: String, target: Int, type: TaskType) {
        let task = HealthTask(title: title, target: target, current: 0, type: type)
        uiState.tasks.append(task)
        uiState.newlyAddedTaskId = task.id
        setExpressionDirectly(.excited)
        saveStats()
    }
    
    public func clearNewlyAddedTask() {
        uiState.newlyAddedTaskId = nil
        saveStats()
    }
    
    // MARK: - Meal Operations
    
    public func logMeal(name: String, calories: Int, protein: Int, carbs: Int, fat: Int) {
        let meal = LoggedMeal(
            name: name,
            calories: calories,
            proteinGrams: protein,
            carbsGrams: carbs,
            fatGrams: fat,
            country: uiState.country
        )
        uiState.loggedMeals.append(meal)
        uiState.bonusCalories += calories / 4 // Bonus gamified burn quota
        addXP(50)
        setExpressionDirectly(.happy)
        saveStats()
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
        let expr = tapCycleExpressions[tapCycleIndex % tapCycleExpressions.count]
        tapCycleIndex += 1
        setExpressionDirectly(expr)
    }
    
    public func onAvatarDoubleTap() {
        setExpressionDirectly(.laughing)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            self?.setExpressionDirectly(.happy)
        }
    }
    
    public func onScreenTouch() {
        if uiState.expression == .tired {
            setExpressionDirectly(.attentive)
        }
    }
    
    public func setExpressionDirectly(_ expr: AvatarExpression) {
        uiState.expression = expr
    }
    
    // MARK: - Settings & Customization
    
    public func setAvatar(_ type: AvatarType) {
        uiState.avatarType = type
        saveStats()
    }
    
    public func setVisualStyle(_ style: VisualStyle) {
        uiState.visualStyle = style
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
