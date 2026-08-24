package com.bushy.health

enum class AvatarType {
    MALE, FEMALE
}

enum class AvatarExpression {
    NEUTRAL, HAPPY, EXCITED, WORKING_OUT, CELEBRATING, TIRED, ATTENTIVE, SURPRISED, LAUGHING, ANGRY, THINKING
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class VisualStyle {
    MATERIAL3, MONOCHROME
}

data class UserStats(
    val userName: String = "",
    val age: Int = 0,
    val height: Int = 0,
    val isSetupComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val visualStyle: VisualStyle = VisualStyle.MATERIAL3,
    val steps: Long = 0,
    val pushups: Int = 0,
    val xp: Long = 0,
    val avatarType: AvatarType = AvatarType.MALE,
    val expression: AvatarExpression = AvatarExpression.NEUTRAL,
    val syncMessage: String? = null,
    val showChangelog: Boolean = false,
    val tasks: List<HealthTask> = emptyList(),
    val bushyAIHistory: List<BushyAIMessage> = emptyList()
) {
    val calories: Int get() = (steps * 0.04).toInt()
    
    val level: Int get() = (Math.sqrt(xp.toDouble() / 100.0).toInt() + 1)
    
    val xpInCurrentLevel: Long get() {
        val currentLevelStartXP = 100L * (level - 1) * (level - 1)
        return xp - currentLevelStartXP
    }
    
    val xpRequiredForNextLevel: Long get() {
        val currentLevelStartXP = 100L * (level - 1) * (level - 1)
        val nextLevelStartXP = 100L * level * level
        return nextLevelStartXP - currentLevelStartXP
    }
    
    val levelProgress: Float get() = xpInCurrentLevel.toFloat() / xpRequiredForNextLevel.toFloat()
}

data class HealthTask(
    val id: String,
    val title: String,
    val target: Int,
    val current: Int,
    val type: TaskType,
    val isCompleted: Boolean = false
)

data class BushyAIMessage(
    val text: String,
    val isUser: Boolean,
    val citations: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class TaskType {
    STEPS, PUSHUPS
}
