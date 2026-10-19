package com.bushy.health

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

class BushyAIViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("bushy_ai_prefs", Context.MODE_PRIVATE)
    private val localAiManager = LocalAiManager(application)
    private val visionFoodAnalyzer: VisionFoodAnalyzer = LocalVisionFoodAnalyzer()

    private val _selectedModel = MutableStateFlow(
        LocalAiModel.fromId(prefs.getString("selected_ai_model", LocalAiModel.SMOLLM_360M.id) ?: LocalAiModel.SMOLLM_360M.id)
    )
    val selectedModel: StateFlow<LocalAiModel> = _selectedModel.asStateFlow()

    val downloadState: StateFlow<Map<String, ModelDownloadState>> = localAiManager.downloadState

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isUserTyping = MutableStateFlow(false)
    val isUserTyping: StateFlow<Boolean> = _isUserTyping.asStateFlow()

    private val _messages = MutableStateFlow<List<BushyAIMessage>>(
        parseMessagesJson(prefs.getString("saved_ai_messages", null))
    )
    val messages: StateFlow<List<BushyAIMessage>> = _messages.asStateFlow()

    // Active Meal Analysis State for Editable Portion UI
    private val _activeMealAnalysisResult = MutableStateFlow<MealAnalysisResult?>(null)
    val activeMealAnalysisResult: StateFlow<MealAnalysisResult?> = _activeMealAnalysisResult.asStateFlow()

    private val _activeMealBitmap = MutableStateFlow<Bitmap?>(null)
    val activeMealBitmap: StateFlow<Bitmap?> = _activeMealBitmap.asStateFlow()

    // Logged Meals History State
    private val _loggedMeals = MutableStateFlow<List<LoggedMeal>>(
        parseLoggedMealsJson(prefs.getString("saved_logged_meals", null))
    )
    val loggedMeals: StateFlow<List<LoggedMeal>> = _loggedMeals.asStateFlow()

    private fun persistMessages(list: List<BushyAIMessage>) {
        prefs.edit().putString("saved_ai_messages", list.takeLast(60).messagesToJsonString()).apply()
    }

    private fun persistLoggedMeals(list: List<LoggedMeal>) {
        val jsonArray = JSONArray()
        list.takeLast(100).forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString("saved_logged_meals", jsonArray.toString()).apply()
    }

    private fun parseLoggedMealsJson(jsonStr: String?): List<LoggedMeal> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<LoggedMeal>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LoggedMeal(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Meal"),
                        calories = obj.optInt("calories", 0),
                        proteinGrams = obj.optInt("proteinGrams", 0),
                        carbsGrams = obj.optInt("carbsGrams", 0),
                        fatGrams = obj.optInt("fatGrams", 0),
                        country = obj.optString("country", "India"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun clearChatHistory() {
        _messages.value = emptyList()
        prefs.edit().remove("saved_ai_messages").apply()
    }

    init {
        val model = _selectedModel.value
        if (model.isLocal) {
            viewModelScope.launch {
                localAiManager.downloadModel(model)
            }
        }
    }

    fun selectModel(model: LocalAiModel) {
        _selectedModel.value = model
        prefs.edit().putString("selected_ai_model", model.id).apply()
        if (model.isLocal) {
            viewModelScope.launch {
                localAiManager.downloadModel(model)
            }
        }
    }

    fun isModelReady(model: LocalAiModel): Boolean {
        return localAiManager.isModelReady(model)
    }

    fun downloadModel(model: LocalAiModel) {
        viewModelScope.launch {
            localAiManager.downloadModel(model)
        }
    }

    fun onUserTyping(isTyping: Boolean) {
        _isUserTyping.value = isTyping
    }

    fun interpretDishText(rawInput: String): DishInterpretationResult {
        return MultilingualDishInterpreter.interpretDishText(rawInput)
    }

    fun tryParseTaskLocal(userText: String): TaskParseResult? = Companion.tryParseTaskLocal(userText)

    fun sendMessage(userText: String, userStats: UserStats, onDeployTask: ((String, Int, TaskType) -> Unit)? = null) {
        if (userText.isBlank()) return

        val userMessage = BushyAIMessage(text = userText, isUser = true)
        val withUser = _messages.updateAndGet { it + userMessage }
        persistMessages(withUser)

        _isUserTyping.value = false

        val localTask = tryParseTaskLocal(userText)
        if (localTask != null) {
            onDeployTask?.invoke(localTask.title, localTask.target, localTask.type)
            val withBot = _messages.updateAndGet { it + BushyAIMessage(text = localTask.replyText, isUser = false) }
            persistMessages(withBot)
            return
        }

        _isGenerating.value = true
        val activeModel = _selectedModel.value

        viewModelScope.launch {
            try {
                val localResponse = localAiManager.generateLocalResponse(
                    prompt = userText,
                    model = activeModel,
                    userStats = userStats,
                    todayLoggedMeals = _loggedMeals.value
                )
                val withResponse = _messages.updateAndGet { it + BushyAIMessage(text = localResponse, isUser = false) }
                persistMessages(withResponse)
            } catch (e: Exception) {
                val withErr = _messages.updateAndGet { it + BushyAIMessage(text = "Error: ${e.localizedMessage}", isUser = false) }
                persistMessages(withErr)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun analyzeMealPhoto(
        bitmap: Bitmap,
        country: String,
        onResult: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int) -> Unit
    ) {
        _isGenerating.value = true
        _activeMealBitmap.value = bitmap

        viewModelScope.launch {
            try {
                val analysisResult = visionFoodAnalyzer.analyzeMealPhoto(bitmap, country)
                _activeMealAnalysisResult.value = analysisResult

                val totals = analysisResult.totalNutrients
                onResult(
                    analysisResult.mealName,
                    totals.calories,
                    totals.proteinG.toInt(),
                    totals.carbsG.toInt(),
                    totals.fatG.toInt()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun saveConfirmedMeal(
        result: MealAnalysisResult,
        onMealLogged: ((String, Int, Int, Int, Int) -> Unit)?
    ) {
        val totals = result.totalNutrients
        onMealLogged?.invoke(
            result.mealName,
            totals.calories,
            totals.proteinG.toInt(),
            totals.carbsG.toInt(),
            totals.fatG.toInt()
        )

        val newLoggedMeal = LoggedMeal(
            id = UUID.randomUUID().toString(),
            name = result.mealName,
            calories = totals.calories,
            proteinGrams = totals.proteinG.toInt(),
            carbsGrams = totals.carbsG.toInt(),
            fatGrams = totals.fatG.toInt(),
            country = "India",
            timestamp = System.currentTimeMillis()
        )

        val updatedLogged = _loggedMeals.updateAndGet { it + newLoggedMeal }
        persistLoggedMeals(updatedLogged)

        val breakdownText = StringBuilder().apply {
            append("🍱 Meal Confirmed & Logged: ${result.mealName}\n\nConfirmed Items:\n")
            result.items.forEach { item ->
                val n = item.calculatedNutrients
                append("• ${item.foodName} (${item.quantity} ${item.unit.displayName}): ${n.calories} kcal [${item.prepStyle.displayName}]\n")
            }
            append("─────────────────────────\n")
            append("Total: ${totals.calories} kcal (${result.calorieRange.minCalories}-${result.calorieRange.maxCalories} range) • ${totals.proteinG}g Protein • ${totals.carbsG}g Carbs • ${totals.fatG}g Fat\nMethod: ${result.analysisMethod.displayName}")
        }.toString()

        val newMessages = _messages.updateAndGet { it + BushyAIMessage(text = breakdownText, isUser = false) }
        persistMessages(newMessages)

        clearActiveMealAnalysis()
    }

    fun clearActiveMealAnalysis() {
        _activeMealAnalysisResult.value = null
        _activeMealBitmap.value = null
    }

    override fun onCleared() {
        super.onCleared()
        localAiManager.close()
    }

    companion object {
        fun tryParseTaskLocal(userText: String): TaskParseResult? {
            val lower = userText.trim().lowercase()

            val isQuestion = lower.endsWith("?") ||
                Regex("""^(what|why|how|when|where|who|can|should|could|is|are|do you|tell me|explain)\b""").containsMatchIn(lower)

            val isExplicitTaskCommand = Regex("""\b(add|create|deploy|set|track|start)\b.*\b(task|mission|goal|workout|routine)\b""").containsMatchIn(lower) ||
                                        Regex("""\b(new|daily)\b.*\b(task|mission|goal)\b""").containsMatchIn(lower)

            if (isQuestion && !isExplicitTaskCommand) return null

            val isExerciseMention = Regex("""\b(pushups?|push up|push-up|pullups?|pull up|pull-up|squats?|crunches?|situps?|sit up|planks?|jumping jacks?|water|drink|steps?|walk|run)\b""").containsMatchIn(lower)

            if (!isExplicitTaskCommand && !isExerciseMention) return null

            val numbers = Regex("""\d+""").findAll(userText).map { it.value.toIntOrNull() ?: 0 }.filter { it > 0 }.toList()
            val detectedNumber = numbers.firstOrNull()
            val hasNumber = detectedNumber != null
            val isImperative = Regex("""^(do|drink|run|walk|hold|perform|track|log|start|add|set|create)\b""").containsMatchIn(lower)
            val isExactExerciseName = Regex("""^(pushups?|push up|push-up|pullups?|pull up|pull-up|squats?|crunches?|situps?|sit up|planks?|jumping jacks?|water|drink|steps?|walk|run)$""").matches(lower)

            if (!isExplicitTaskCommand && !hasNumber && !isImperative && !isExactExerciseName) {
                return null
            }

            val type: TaskType
            val defaultTarget: Int
            val title: String

            when {
                lower.contains("step") || lower.contains("walk") || lower.contains("run") -> {
                    type = TaskType.STEPS
                    defaultTarget = 5000
                    val target = detectedNumber ?: defaultTarget
                    title = if (lower.contains("run")) "$target Km Run" else "$target Daily Steps"
                    return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
                }

                lower.contains("pushup") || lower.contains("push up") || lower.contains("push-up") -> {
                    type = TaskType.PUSHUPS
                    defaultTarget = 30
                    val target = detectedNumber ?: defaultTarget
                    title = "$target Pushups"
                    return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
                }

                lower.contains("pullup") || lower.contains("pull up") || lower.contains("pull-up") -> {
                    type = TaskType.GENERAL
                    defaultTarget = 10
                    val target = detectedNumber ?: defaultTarget
                    title = "$target Pullups"
                    return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
                }

                lower.contains("squat") -> {
                    type = TaskType.GENERAL
                    defaultTarget = 30
                    val target = detectedNumber ?: defaultTarget
                    title = "$target Squats"
                    return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
                }

                lower.contains("crunch") || lower.contains("situp") || lower.contains("sit up") -> {
                    type = TaskType.GENERAL
                    defaultTarget = 25
                    val target = detectedNumber ?: defaultTarget
                    title = "$target Crunches"
                    return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
                }

                lower.contains("jack") -> {
                    type = TaskType.GENERAL
                    defaultTarget = 50
                    val target = detectedNumber ?: defaultTarget
                    title = "$target Jumping Jacks"
                    return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
                }

                lower.contains("plank") -> {
                    type = TaskType.GENERAL
                    defaultTarget = 60
                    val target = detectedNumber ?: defaultTarget
                    title = "Hold Plank for $target Sec"
                    return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
                }

                lower.contains("water") || lower.contains("drink") || lower.contains("glass") -> {
                    type = TaskType.WATER
                    defaultTarget = 8
                    val target = detectedNumber ?: defaultTarget
                    title = "Drink $target Glasses of Water"
                    return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
                }

                isExplicitTaskCommand -> {
                    type = TaskType.GENERAL
                    val target = detectedNumber ?: 10

                    var cleanedTitle = userText
                        .replace(Regex("""(?i)\b(add|create|deploy|set|automate|task|mission|for|me|a|new|to|do|goal)\b"""), "")
                        .replace(Regex("""\d+"""), "")
                        .trim()

                    if (cleanedTitle.isBlank()) cleanedTitle = "Daily Exercise"

                    cleanedTitle = cleanedTitle.lowercase().split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }

                    title = if (detectedNumber != null) "$target $cleanedTitle" else cleanedTitle
                    return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title' (Target: $target). Zero API tokens used!")
                }

                else -> return null
            }
        }
    }
}

data class TaskParseResult(
    val title: String,
    val target: Int,
    val type: TaskType,
    val replyText: String
)
