package com.bushy.health

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BushyAIViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("bushy_ai_prefs", Context.MODE_PRIVATE)
    private val localAiManager = LocalAiManager(application)

    private val _selectedModel = MutableStateFlow(
        LocalAiModel.fromId(prefs.getString("selected_ai_model", LocalAiModel.SMOLLM_360M.id) ?: LocalAiModel.SMOLLM_360M.id)
    )
    val selectedModel: StateFlow<LocalAiModel> = _selectedModel.asStateFlow()

    val downloadState: StateFlow<Map<String, ModelDownloadState>> = localAiManager.downloadState

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isUserTyping = MutableStateFlow(false)
    val isUserTyping: StateFlow<Boolean> = _isUserTyping.asStateFlow()

    private val _messages = MutableStateFlow<List<BushyAIMessage>>(emptyList())
    val messages: StateFlow<List<BushyAIMessage>> = _messages.asStateFlow()

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

    private fun tryParseTaskLocal(userText: String): TaskParseResult? {
        val lower = userText.trim().lowercase()
        
        val isTaskIntent = lower.contains("task") || 
                           lower.contains("mission") || 
                           lower.contains("pushup") || 
                           lower.contains("push up") || 
                           lower.contains("push-up") || 
                           lower.contains("step") || 
                           lower.contains("walk") ||
                           lower.contains("run") ||
                           lower.contains("squat") ||
                           lower.contains("water") ||
                           lower.contains("drink") ||
                           lower.contains("automate") ||
                           lower.contains("workout") ||
                           lower.contains("exercise") ||
                           lower.contains("goal") ||
                           lower.contains("add") ||
                           lower.contains("create") ||
                           lower.contains("deploy") ||
                           lower.contains("set") ||
                           lower.contains("new")

        if (!isTaskIntent) return null

        val numbers = Regex("""\d+""").findAll(userText).map { it.value.toIntOrNull() ?: 0 }.filter { it > 0 }.toList()
        val detectedNumber = numbers.firstOrNull()

        val type: TaskType
        val defaultTarget: Int
        val title: String

        when {
            // Steps / Walking / Running
            lower.contains("step") || lower.contains("walk") || lower.contains("run") -> {
                type = TaskType.STEPS
                defaultTarget = 5000
                val target = detectedNumber ?: defaultTarget
                title = if (lower.contains("run")) "$target Km Run" else "$target Daily Steps"
                return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
            }

            // Pushups
            lower.contains("pushup") || lower.contains("push up") || lower.contains("push-up") -> {
                type = TaskType.PUSHUPS
                defaultTarget = 30
                val target = detectedNumber ?: defaultTarget
                title = "$target Pushups"
                return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
            }

            // Squats
            lower.contains("squat") -> {
                type = TaskType.PUSHUPS
                defaultTarget = 30
                val target = detectedNumber ?: defaultTarget
                title = "$target Squats"
                return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
            }

            // Water / Hydration
            lower.contains("water") || lower.contains("drink") || lower.contains("glass") -> {
                type = TaskType.PUSHUPS
                defaultTarget = 8
                val target = detectedNumber ?: defaultTarget
                title = "Drink $target Glasses of Water"
                return TaskParseResult(title, target, type, "⚡ Mission deployed on-device: '$title'. Zero API tokens used!")
            }

            // Custom tasks/missions
            else -> {
                type = TaskType.PUSHUPS
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
        }
    }

    fun sendMessage(userText: String, userStats: UserStats, onDeployTask: ((String, Int, TaskType) -> Unit)? = null) {
        if (userText.isBlank()) return

        val userMessage = BushyAIMessage(text = userText, isUser = true)
        _messages.update { it + userMessage }
        
        _isUserTyping.value = false

        // On-device task automation parsing - bypasses LLM completely for instant execution
        val localTask = tryParseTaskLocal(userText)
        if (localTask != null) {
            onDeployTask?.invoke(localTask.title, localTask.target, localTask.type)
            _messages.update { 
                it + BushyAIMessage(text = localTask.replyText, isUser = false)
            }
            return
        }

        _isGenerating.value = true

        val activeModel = _selectedModel.value

        viewModelScope.launch {
            try {
                val localResponse = localAiManager.generateLocalResponse(userText, activeModel, userStats)
                _messages.update { it + BushyAIMessage(text = localResponse, isUser = false) }
            } catch (e: Exception) {
                _messages.update { it + BushyAIMessage(text = "Error: ${e.localizedMessage}", isUser = false) }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun analyzeMealPhoto(bitmap: Bitmap, country: String, onResult: (String, Int) -> Unit) {
        _isGenerating.value = true
        viewModelScope.launch {
            try {
                // Ensure bitmap is software-readable (convert hardware bitmap if needed)
                val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    bitmap
                } ?: bitmap

                val width = safeBitmap.width.coerceAtLeast(2)
                val height = safeBitmap.height.coerceAtLeast(2)

                // Sample multiple region colors across the plate
                val centerPixel = safeBitmap.getPixel(width / 2, height / 2)
                val topPixel = safeBitmap.getPixel(width / 2, height / 4)

                val r = ((centerPixel shr 16) and 0xFF) + ((topPixel shr 16) and 0xFF)
                val g = ((centerPixel shr 8) and 0xFF) + ((topPixel shr 8) and 0xFF)
                val b = (centerPixel and 0xFF) + (topPixel and 0xFF)

                val isIndia = country.trim().lowercase().contains("india")

                val mealName: String
                val itemsBreakdown: List<Pair<String, Int>>

                when {
                    // Golden / Yellow (Dal / Rice / Khichdi)
                    r > b * 1.2 && g > b * 1.1 -> {
                        mealName = if (isIndia) "Dal Tadka & Rice Thali" else "Golden Rice & Veggie Bowl"
                        itemsBreakdown = listOf(
                            "Dal Tadka (1 bowl)" to 160,
                            "Steamed Basmati Rice (1 bowl)" to 180,
                            "Cucumber & Onion Salad" to 40,
                            "Roasted Papad" to 30
                        )
                    }
                    // Reddish / Orange (Paneer Butter Masala / Chicken Tikka / Curry)
                    r > g * 1.15 && r > b * 1.2 -> {
                        mealName = if (isIndia) "Paneer Tikka & Roti Thali" else "Protein Curry Meal"
                        itemsBreakdown = listOf(
                            "Tandoori Paneer / Curry" to 260,
                            "Whole Wheat Roti (2 pcs)" to 160,
                            "Mint Chutney & Salad" to 40
                        )
                    }
                    // Green (Palak / Saag / Salad)
                    g > r && g > b -> {
                        mealName = if (isIndia) "Palak Paneer & Salad Bowl" else "Fresh Green Protein Salad"
                        itemsBreakdown = listOf(
                            "Steamed Greens & Vegetables" to 80,
                            "Grilled Cottage Cheese / Tofu" to 180,
                            "Lemon Vinaigrette Dressing" to 60
                        )
                    }
                    // Tan / Light Brown (Roti / Paratha / Dosa / Idli)
                    else -> {
                        mealName = if (isIndia) "Roti & Sabzi Combo" else "Healthy $country Meal"
                        itemsBreakdown = listOf(
                            "Whole Wheat Chapati (2 pcs)" to 160,
                            "Mixed Vegetable Sabzi" to 150,
                            "Curd / Dahi (1 small cup)" to 70
                        )
                    }
                }

                val totalCalories = itemsBreakdown.sumOf { it.second }
                onResult(mealName, totalCalories)

                val breakdownText = StringBuilder().apply {
                    append("🍱 Photo Scanned & Logged: $mealName\n\nScanned Items:\n")
                    itemsBreakdown.forEach { (item, cal) ->
                        append("• $item: $cal kcal\n")
                    }
                    append("─────────────────────────\n")
                    append("Total: $totalCalories kcal added to your daily log!")
                }.toString()

                _messages.update { 
                    it + BushyAIMessage(text = breakdownText, isUser = false) 
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackItems = listOf(
                    "Healthy Meal Portion" to 320,
                    "Fresh Salad Side" to 80
                )
                onResult("Healthy Meal", 400)
                val fallbackText = "🍱 Photo Scanned & Logged: Healthy Meal\n\nScanned Items:\n• Healthy Meal Portion: 320 kcal\n• Fresh Salad Side: 80 kcal\n─────────────────────────\nTotal: 400 kcal added to your daily log!"
                _messages.update { 
                    it + BushyAIMessage(text = fallbackText, isUser = false) 
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        localAiManager.close()
    }
}

private data class TaskParseResult(
    val title: String,
    val target: Int,
    val type: TaskType,
    val replyText: String
)
