import Foundation

public enum AvatarType: String, Codable, CaseIterable, Identifiable {
    case male = "MALE"
    case female = "FEMALE"
    
    public var id: String { rawValue }
    
    public var displayName: String {
        switch self {
        case .male: return "Bloub Boy"
        case .female: return "Bloub Girl"
        }
    }
}

public enum AvatarExpression: String, Codable, CaseIterable {
    case neutral = "NEUTRAL"
    case happy = "HAPPY"
    case excited = "EXCITED"
    case workingOut = "WORKING_OUT"
    case celebrating = "CELEBRATING"
    case tired = "TIRED"
    case attentive = "ATTENTIVE"
    case surprised = "SURPRISED"
    case laughing = "LAUGHING"
    case angry = "ANGRY"
    case thinking = "THINKING"
}

public enum ThemeMode: String, Codable, CaseIterable {
    case light = "LIGHT"
    case dark = "DARK"
    case system = "SYSTEM"
}

public enum VisualStyle: String, Codable, CaseIterable {
    case material3 = "MATERIAL3"
    case monochrome = "MONOCHROME"
}

public enum TaskType: String, Codable, CaseIterable {
    case steps = "STEPS"
    case pushups = "PUSHUPS"
    case water = "WATER"
    case general = "GENERAL"
    
    public var iconName: String {
        switch self {
        case .steps: return "figure.walk"
        case .pushups: return "figure.strengthtraining.traditional"
        case .water: return "drop.fill"
        case .general: return "checkmark.seal.fill"
        }
    }
    
    public var unitLabel: String {
        switch self {
        case .steps: return "steps"
        case .pushups: return "reps"
        case .water: return "glasses"
        case .general: return "pts"
        }
    }
}

public struct HealthTask: Identifiable, Codable, Equatable {
    public var id: String
    public var title: String
    public var target: Int
    public var current: Int
    public var type: TaskType
    public var isCompleted: BooleanLiteralType
    
    public init(id: String = UUID().uuidString, title: String, target: Int, current: Int = 0, type: TaskType, isCompleted: Bool = false) {
        self.id = id
        self.title = title
        self.target = target
        self.current = current
        self.type = type
        self.isCompleted = isCompleted
    }
    
    public var progress: Double {
        guard target > 0 else { return 0.0 }
        return min(Double(current) / Double(target), 1.0)
    }
}

public struct LoggedMeal: Identifiable, Codable, Equatable {
    public var id: String
    public var name: String
    public var calories: Int
    public var proteinGrams: Int
    public var carbsGrams: Int
    public var fatGrams: Int
    public var country: String
    public var timestamp: Date
    
    public init(
        id: String = UUID().uuidString,
        name: String,
        calories: Int,
        proteinGrams: Int = 0,
        carbsGrams: Int = 0,
        fatGrams: Int = 0,
        country: String = "India",
        timestamp: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.calories = calories
        self.proteinGrams = proteinGrams
        self.carbsGrams = carbsGrams
        self.fatGrams = fatGrams
        self.country = country
        self.timestamp = timestamp
    }
}

public struct BushyAIMessage: Identifiable, Codable, Equatable {
    public var id: String
    public var text: String
    public var isUser: Bool
    public var citations: [String]
    public var timestamp: Date
    
    public init(
        id: String = UUID().uuidString,
        text: String,
        isUser: Bool,
        citations: [String] = [],
        timestamp: Date = Date()
    ) {
        self.id = id
        self.text = text
        self.isUser = isUser
        self.citations = citations
        self.timestamp = timestamp
    }
}

public struct UserStats: Codable, Equatable {
    public var userName: String
    public var age: Int
    public var height: Int
    public var isSetupComplete: Bool
    public var themeMode: ThemeMode
    public var visualStyle: VisualStyle
    public var steps: Int
    public var pushups: Int
    public var xp: Int
    public var avatarType: AvatarType
    public var expression: AvatarExpression
    public var syncMessage: String?
    public var showChangelog: Bool
    public var tasks: [HealthTask]
    public var bushyAIHistory: [BushyAIMessage]
    public var newlyAddedTaskId: String?
    public var country: String
    public var bonusCalories: Int
    public var loggedMeals: [LoggedMeal]
    public var streakDays: Int
    public var lastActiveDate: String
    
    public init(
        userName: String = "",
        age: Int = 0,
        height: Int = 0,
        isSetupComplete: Bool = false,
        themeMode: ThemeMode = .system,
        visualStyle: VisualStyle = .material3,
        steps: Int = 0,
        pushups: Int = 0,
        xp: Int = 0,
        avatarType: AvatarType = .male,
        expression: AvatarExpression = .neutral,
        syncMessage: String? = nil,
        showChangelog: Bool = false,
        tasks: [HealthTask] = [],
        bushyAIHistory: [BushyAIMessage] = [],
        newlyAddedTaskId: String? = nil,
        country: String = "India",
        bonusCalories: Int = 0,
        loggedMeals: [LoggedMeal] = [],
        streakDays: Int = 1,
        lastActiveDate: String = ""
    ) {
        self.userName = userName
        self.age = age
        self.height = height
        self.isSetupComplete = isSetupComplete
        self.themeMode = themeMode
        self.visualStyle = visualStyle
        self.steps = steps
        self.pushups = pushups
        self.xp = xp
        self.avatarType = avatarType
        self.expression = expression
        self.syncMessage = syncMessage
        self.showChangelog = showChangelog
        self.tasks = tasks
        self.bushyAIHistory = bushyAIHistory
        self.newlyAddedTaskId = newlyAddedTaskId
        self.country = country
        self.bonusCalories = bonusCalories
        self.loggedMeals = loggedMeals
        self.streakDays = streakDays
        self.lastActiveDate = lastActiveDate
    }
    
    // Equivalent of Android: (steps * 0.04).toInt() + bonusCalories
    public var calories: Int {
        Int(Double(steps) * 0.04) + bonusCalories
    }
    
    // Level formula: sqrt(xp / 100) + 1
    public var level: Int {
        Int(Double(xp) / 100.0.squareRoot()) + 1
    }
    
    public var xpInCurrentLevel: Int {
        let currentLevelStartXP = 100 * (level - 1) * (level - 1)
        return max(0, xp - currentLevelStartXP)
    }
    
    public var xpRequiredForNextLevel: Int {
        let currentLevelStartXP = 100 * (level - 1) * (level - 1)
        let nextLevelStartXP = 100 * level * level
        return max(1, nextLevelStartXP - currentLevelStartXP)
    }
    
    public var levelProgress: Double {
        let req = Double(xpRequiredForNextLevel)
        guard req > 0 else { return 0.0 }
        return min(max(Double(xpInCurrentLevel) / req, 0.0), 1.0)
    }
    
    public var totalProteinGrams: Int {
        loggedMeals.reduce(0) { $0 + $1.proteinGrams }
    }
    
    public var totalCarbsGrams: Int {
        loggedMeals.reduce(0) { $0 + $1.carbsGrams }
    }
    
    public var totalFatGrams: Int {
        loggedMeals.reduce(0) { $0 + $1.fatGrams }
    }
}
