package com.bushy.health

enum class AvatarType {
    MALE, FEMALE
}

data class UserStats(
    val level: Int = 1,
    val xp: Int = 0,
    val steps: Long = 0,
    val pushups: Int = 0,
    val avatarType: AvatarType = AvatarType.MALE
) {
    val nextLevelXp: Int get() = level * 100
}

data class HealthTask(
    val title: String,
    val target: Int,
    val current: Int,
    val type: TaskType
)

enum class TaskType {
    STEPS, PUSHUPS
}
