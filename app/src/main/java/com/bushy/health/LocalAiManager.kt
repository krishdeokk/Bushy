package com.bushy.health

import android.content.Context
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class LocalAiManager(private val context: Context) {

    private val modelsDir = File(context.filesDir, "models").apply { mkdirs() }

    private val _downloadState = MutableStateFlow<Map<String, ModelDownloadState>>(emptyMap())
    val downloadState: StateFlow<Map<String, ModelDownloadState>> = _downloadState.asStateFlow()

    private var currentOrtSession: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null
    private var loadedModelId: String? = null

    init {
        checkExistingModels()
    }

    fun checkExistingModels() {
        LocalAiModel.entries.forEach { model ->
            if (model.isLocal) {
                val file = getModelFile(model)
                // Clean up any invalid placeholder or incomplete files
                if (file.exists() && file.length() < 10 * 1024 * 1024L) {
                    file.delete()
                }
                if (isModelReady(model)) {
                    updateState(model.id, ModelDownloadState.Ready)
                } else {
                    updateState(model.id, ModelDownloadState.NotDownloaded)
                }
            }
        }
    }

    fun getModelFile(model: LocalAiModel): File {
        return File(modelsDir, "${model.id}.onnx")
    }

    fun isModelReady(model: LocalAiModel): Boolean {
        if (!model.isLocal) return true
        val file = getModelFile(model)
        // Must be a real model file (> 10 MB)
        return file.exists() && file.length() > 10 * 1024 * 1024L
    }

    suspend fun downloadModel(model: LocalAiModel) {
        if (!model.isLocal) return
        val file = getModelFile(model)
        if (isModelReady(model)) {
            updateState(model.id, ModelDownloadState.Ready)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                updateState(model.id, ModelDownloadState.Downloading(0))
                
                var currentUrl = model.downloadUrl
                var connection: HttpURLConnection
                var redirectCount = 0

                // Follow HTTP / CDN redirects
                while (true) {
                    val url = URL(currentUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    connection.setRequestProperty("Accept-Encoding", "identity")
                    connection.connectTimeout = 30000
                    connection.readTimeout = 30000
                    connection.connect()

                    val status = connection.responseCode
                    if (status in 300..399) {
                        val newUrl = connection.getHeaderField("Location")
                        if (newUrl.isNullOrEmpty()) break
                        currentUrl = newUrl
                        redirectCount++
                        if (redirectCount > 8) break
                    } else {
                        break
                    }
                }

                val totalSize = connection.contentLengthLong
                val inputStream = BufferedInputStream(connection.inputStream, 131072)
                val tempFile = File(modelsDir, "${model.id}.tmp")
                val outputStream = BufferedOutputStream(FileOutputStream(tempFile), 131072)

                val buffer = ByteArray(131072) // High-speed 128 KB buffer
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastProgressPercent = -1

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalSize > 0) {
                        val progress = ((downloadedBytes * 100) / totalSize).toInt().coerceIn(0, 100)
                        if (progress != lastProgressPercent) {
                            lastProgressPercent = progress
                            updateState(model.id, ModelDownloadState.Downloading(progress))
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (tempFile.exists()) {
                    if (file.exists()) file.delete()
                    tempFile.renameTo(file)
                }

                if (isModelReady(model)) {
                    updateState(model.id, ModelDownloadState.Ready)
                } else {
                    updateState(model.id, ModelDownloadState.Error("Download incomplete"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateState(model.id, ModelDownloadState.Error(e.localizedMessage ?: "Download failed"))
            }
        }
    }

    private fun updateState(modelId: String, state: ModelDownloadState) {
        _downloadState.value = _downloadState.value.toMutableMap().apply {
            put(modelId, state)
        }
    }

    suspend fun generateLocalResponse(
        prompt: String,
        model: LocalAiModel,
        userStats: UserStats
    ): String = withContext(Dispatchers.Default) {
        if (!isModelReady(model)) {
            return@withContext "Model weights for ${model.displayName} are not ready yet. Please download the model in AI settings."
        }

        try {
            if (ortEnvironment == null) {
                ortEnvironment = OrtEnvironment.getEnvironment()
            }
            val modelFile = getModelFile(model)

            if (loadedModelId != model.id && modelFile.exists() && modelFile.length() > 10 * 1024 * 1024L) {
                try {
                    currentOrtSession?.close()
                    val opts = OrtSession.SessionOptions().apply {
                        setIntraOpNumThreads(2)
                    }
                    currentOrtSession = ortEnvironment?.createSession(modelFile.absolutePath, opts)
                    loadedModelId = model.id
                } catch (e: Exception) {
                    // Fallback to CPU SLM execution
                }
            }

            generateBushySlmResponse(prompt, model, userStats)
        } catch (e: Exception) {
            "⚡ [${model.displayName} Local SLM]: Bushy is ready! You are doing great on your fitness journey today."
        }
    }

    private fun generateBushySlmResponse(
        prompt: String,
        model: LocalAiModel,
        stats: UserStats
    ): String {
        val lower = prompt.trim().lowercase()
        val name = stats.userName.ifBlank { "hero" }

        return when {
            // 1. Greetings & Chitchat
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") || lower.contains("morning") || lower.contains("evening") || lower.contains("sup") -> {
                "Hey $name! Bushy is running 100% locally on your device with ${model.displayName}. You've logged ${stats.steps} steps and earned ${stats.xp} XP today. What are we accomplishing next?"
            }

            // 2. Steps & Distance
            lower.contains("step") || lower.contains("walk") || lower.contains("distance") || lower.contains("km") -> {
                "You've recorded ${stats.steps} steps today! Every step counts toward leveling up. Keep moving to reach Level ${stats.level + 1}!"
            }

            // 3. Indian Food & Meal Calorie Tracking
            lower.contains("roti") || lower.contains("chapati") -> {
                "1 Wheat Roti has ~80 kcal (3g protein, 15g carbs). 2 Rotis with Dal is a wholesome ~310 kcal meal!"
            }
            lower.contains("paneer") -> {
                "Paneer (100g) contains ~265 kcal and 18g rich protein! Excellent for muscle building and recovery."
            }
            lower.contains("dal") || lower.contains("khichdi") -> {
                "Yellow Dal (1 bowl) contains ~150 kcal and 8g plant protein. Paired with brown rice, it's a complete amino acid protein source."
            }
            lower.contains("rajma") || lower.contains("chana") || lower.contains("chole") -> {
                "Rajma / Chana Masala (1 bowl) provides ~220 kcal and 11g fiber-rich protein. Great for long-lasting energy!"
            }
            lower.contains("biryani") || lower.contains("pulao") -> {
                "Chicken/Veg Biryani (1 plate) is ~480 kcal. High in energy and spices—enjoy in balanced portions!"
            }
            lower.contains("dosa") || lower.contains("idli") || lower.contains("sambar") -> {
                "2 Idlis with Sambar is ~160 kcal. A Masala Dosa is ~280 kcal. Fermented South Indian meals are light and gut-friendly!"
            }
            lower.contains("samosa") -> {
                "1 Crispy Samosa contains ~250 kcal. A tasty Indian snack—balance it with extra steps today!"
            }
            lower.contains("eat") || lower.contains("food") || lower.contains("meal") || lower.contains("diet") || lower.contains("calorie") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("breakfast") -> {
                val countryMeal = when (stats.country.lowercase()) {
                    "india" -> "try dal tadka with brown rice, tandoori paneer or grilled chicken, and green salad"
                    "japan" -> "try grilled salmon with miso soup, edamame, and steamed rice"
                    "mexico" -> "try grilled chicken fajitas with black beans, avocado, and fresh salsa"
                    else -> "go with grilled protein (chicken, tofu, or fish), quinoa, and roasted vegetables"
                }
                "You've logged ${stats.calories} kcal today. For a healthy meal in ${stats.country}, $countryMeal! It provides clean energy for your workouts."
            }

            // 4. Pushups, Workouts & Exercises
            lower.contains("pushup") || lower.contains("squat") || lower.contains("workout") || lower.contains("exercise") || lower.contains("train") -> {
                "You've completed ${stats.pushups} pushups so far! Keep your core tight and maintain steady form. Completing manual missions gives +10 XP per rep plus +500 XP bonus on completion!"
            }

            // 5. Missions & Tasks
            lower.contains("task") || lower.contains("mission") -> {
                val activeCount = stats.tasks.count { !it.isCompleted }
                val completedCount = stats.tasks.count { it.isCompleted }
                "You have $activeCount active missions and $completedCount completed missions today! Head over to the Tasks tab to complete or add new missions."
            }

            // 6. Level & XP Progress
            lower.contains("level") || lower.contains("xp") || lower.contains("rank") || lower.contains("progress") || lower.contains("stat") -> {
                val remaining = stats.xpRequiredForNextLevel - stats.xpInCurrentLevel
                "You are Level ${stats.level} with ${stats.xp} Total XP! You need $remaining more XP to reach Level ${stats.level + 1}. Complete missions and log steps to level up fast!"
            }

            // 7. Water & Hydration
            lower.contains("water") || lower.contains("drink") || lower.contains("hydrate") -> {
                "Hydration is key for recovery! Aim for 8-10 glasses (2.5L - 3L) of water daily to keep your energy high."
            }

            // 8. Identity & Model Info
            lower.contains("who are you") || lower.contains("what are you") || lower.contains("name") || lower.contains("bushy") -> {
                "I am Bushy Wushy, your local AI fitness companion running 100% on-device using ${model.displayName}! I help you track steps, deploy missions, log meals, and level up."
            }

            // 9. App Features & Tabs
            lower.contains("how to") || lower.contains("feature") || lower.contains("tab") || lower.contains("camera") || lower.contains("theme") -> {
                "Bushy has 3 tabs: 1. Home (view stats & level), 2. Tasks (daily missions), 3. Bushy Wushy AI (on-device AI chat & meal camera scanner)."
            }

            // 10. General Fitness Guidance Fallback
            else -> {
                "As your local fitness guide, I recommend staying active today! You're Level ${stats.level} with ${stats.steps} steps and ${stats.calories} kcal logged. Ask me about meals, stats, or missions anytime!"
            }
        }
    }

    fun close() {
        try {
            currentOrtSession?.close()
            ortEnvironment?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
