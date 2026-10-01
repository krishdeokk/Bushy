package com.bushy.health

import org.json.JSONArray
import org.json.JSONObject

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
    val bushyAIHistory: List<BushyAIMessage> = emptyList(),
    val newlyAddedTaskId: String? = null,
    val country: String = "India",
    val bonusCalories: Int = 0,
    val loggedMeals: List<LoggedMeal> = emptyList(),
    val streakDays: Int = 1,
    val lastActiveDate: String = ""
) {
    val calories: Int get() = (steps * 0.04).toInt() + bonusCalories
    
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

    val totalProteinGrams: Int get() = loggedMeals.sumOf { it.proteinGrams }
    val totalCarbsGrams: Int get() = loggedMeals.sumOf { it.carbsGrams }
    val totalFatGrams: Int get() = loggedMeals.sumOf { it.fatGrams }
}

data class HealthTask(
    val id: String,
    val title: String,
    val target: Int,
    val current: Int,
    val type: TaskType,
    val isCompleted: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("target", target)
            put("current", current)
            put("type", type.name)
            put("isCompleted", isCompleted)
        }
    }
}

fun parseHealthTask(json: JSONObject): HealthTask {
    return HealthTask(
        id = json.optString("id", java.util.UUID.randomUUID().toString()),
        title = json.optString("title", "Mission"),
        target = json.optInt("target", 10),
        current = json.optInt("current", 0),
        type = try {
            TaskType.valueOf(json.optString("type", TaskType.GENERAL.name))
        } catch (e: Exception) {
            TaskType.GENERAL
        },
        isCompleted = json.optBoolean("isCompleted", false)
    )
}

fun List<HealthTask>.tasksToJsonString(): String {
    val array = JSONArray()
    forEach { array.put(it.toJson()) }
    return array.toString()
}

fun parseTasksJson(jsonStr: String?): List<HealthTask> {
    if (jsonStr.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<HealthTask>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(parseHealthTask(obj))
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

data class LoggedMeal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val calories: Int,
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatGrams: Int = 0,
    val country: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("calories", calories)
            put("proteinGrams", proteinGrams)
            put("carbsGrams", carbsGrams)
            put("fatGrams", fatGrams)
            put("country", country)
            put("timestamp", timestamp)
        }
    }
}

fun parseLoggedMeal(json: JSONObject): LoggedMeal {
    return LoggedMeal(
        id = json.optString("id", java.util.UUID.randomUUID().toString()),
        name = json.optString("name", "Healthy Meal"),
        calories = json.optInt("calories", 0),
        proteinGrams = json.optInt("proteinGrams", 0),
        carbsGrams = json.optInt("carbsGrams", 0),
        fatGrams = json.optInt("fatGrams", 0),
        country = json.optString("country", ""),
        timestamp = json.optLong("timestamp", System.currentTimeMillis())
    )
}

fun List<LoggedMeal>.mealsToJsonString(): String {
    val array = JSONArray()
    forEach { array.put(it.toJson()) }
    return array.toString()
}

fun parseMealsJson(jsonStr: String?): List<LoggedMeal> {
    if (jsonStr.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<LoggedMeal>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(parseLoggedMeal(obj))
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

data class BushyAIMessage(
    val text: String,
    val isUser: Boolean,
    val citations: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("text", text)
            put("isUser", isUser)
            val arr = JSONArray()
            citations.forEach { arr.put(it) }
            put("citations", arr)
            put("timestamp", timestamp)
        }
    }
}

fun parseBushyAIMessage(json: JSONObject): BushyAIMessage {
    val citationsList = mutableListOf<String>()
    val arr = json.optJSONArray("citations")
    if (arr != null) {
        for (i in 0 until arr.length()) {
            citationsList.add(arr.optString(i))
        }
    }
    return BushyAIMessage(
        text = json.optString("text", ""),
        isUser = json.optBoolean("isUser", false),
        citations = citationsList,
        timestamp = json.optLong("timestamp", System.currentTimeMillis())
    )
}

fun List<BushyAIMessage>.messagesToJsonString(): String {
    val array = JSONArray()
    forEach { array.put(it.toJson()) }
    return array.toString()
}

fun parseMessagesJson(jsonStr: String?): List<BushyAIMessage> {
    if (jsonStr.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<BushyAIMessage>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(parseBushyAIMessage(obj))
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

enum class TaskType {
    STEPS, PUSHUPS, WATER, GENERAL
}

