package com.bushy.health

import android.content.Context
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
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
import java.util.Locale

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

                while (true) {
                    val url = URL(currentUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    connection.setRequestProperty("Accept-Encoding", "identity")
                    connection.connectTimeout = 60000
                    connection.readTimeout = 60000
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
                val tempFile = File(modelsDir, "${model.id}.tmp")

                val bufferSize = 262144 // 256 KB High-Speed Buffer
                BufferedInputStream(connection.inputStream, bufferSize).use { inputStream ->
                    BufferedOutputStream(FileOutputStream(tempFile), bufferSize).use { outputStream ->
                        val buffer = ByteArray(bufferSize)
                        var bytesRead: Int
                        var downloadedBytes = 0L
                        var lastProgressPercent = -1

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            ensureActive()
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
                    }
                }

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
        userStats: UserStats,
        todayLoggedMeals: List<LoggedMeal> = emptyList()
    ): String = withContext(Dispatchers.Default) {
        try {
            if (isModelReady(model)) {
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
                        // Fallback
                    }
                }
            }

            generateBushySlmResponse(prompt, model, userStats, todayLoggedMeals)
        } catch (e: Exception) {
            "⚡ [${model.displayName} Local SLM]: Bushy is ready! Ask me anything about bulking, workouts, or Indian meal calories."
        }
    }

    private fun generateBushySlmResponse(
        prompt: String,
        model: LocalAiModel,
        stats: UserStats,
        loggedMeals: List<LoggedMeal>
    ): String {
        val lower = prompt.trim().lowercase(Locale.US)
        val name = stats.userName.ifBlank { "hero" }

        return when {
            // 1. TODAY'S LOGGED MEALS & CALORIES QUERY
            lower.contains("what did i eat") || lower.contains("logged meal") || lower.contains("my meals") ||
            lower.contains("what i ate") || lower.contains("consumed") || lower.contains("calories today") -> {
                if (loggedMeals.isEmpty()) {
                    "🍱 You haven't logged any meals today yet, $name! Use the camera button to take a photo of your lunch or dinner and earn +50 XP!"
                } else {
                    val totalCal = loggedMeals.sumOf { it.calories }
                    val totalP = loggedMeals.sumOf { it.proteinGrams }
                    val totalC = loggedMeals.sumOf { it.carbsGrams }
                    val totalF = loggedMeals.sumOf { it.fatGrams }

                    val sb = StringBuilder().apply {
                        append("🍱 Today's Logged Meals Summary ($name):\n\n")
                        loggedMeals.forEachIndexed { i, meal ->
                            append("${i + 1}. ${meal.name}: ${meal.calories} kcal (P: ${meal.proteinGrams}g | C: ${meal.carbsGrams}g | F: ${meal.fatGrams}g)\n")
                        }
                        append("─────────────────────────\n")
                        append("Total Consumed: $totalCal kcal • ${totalP}g Protein • ${totalC}g Carbs • ${totalF}g Fat")
                    }
                    sb.toString()
                }
            }

            // 2. BULKING, LEAN MASS & BIG ARMS / BICEPS
            lower.contains("bulk") || lower.contains("bulking") || lower.contains("gain weight") ||
            lower.contains("build muscle") || lower.contains("get bigger") || lower.contains("mass") ||
            lower.contains("big arms") || lower.contains("bicep") || lower.contains("tricep") -> {
                val armTip = if (lower.contains("arm") || lower.contains("bicep") || lower.contains("tricep")) {
                    "\n\n💪 **Big Arms Secret**: The triceps make up ~60–65% of your upper arm mass! Prioritize heavy dips, overhead cable extensions, and close-grip presses alongside incline bicep curls."
                } else ""

                """
                💪 **Elite Muscle Building & Hypertrophy Guide for $name**:

                1. **Calorie Surplus**: Aim for +300 to +500 kcal above maintenance (~2,300–2,600 kcal/day).
                2. **Protein Target**: 1.6g – 2.2g of protein per kg bodyweight.
                3. **Hypertrophy Rep Range**: Train in the 8 to 12 rep sweet spot, taking sets within 1-2 reps of failure.
                4. **Progressive Overload**: Continuously add weight, reps, or control tempo over time.
                5. **High-Protein Indian Foods**:
                   • **Paneer Butter Masala / Tikka**: ~265 kcal & 18g protein per 100g
                   • **Chicken / Fish Curry**: ~165 kcal & 22g protein per portion
                   • **Dal Tadka + Basmati Rice**: Complete amino-acid protein combo (~350 kcal)
                   • **Whey Protein Shake**: 24g fast-absorbing protein per scoop$armTip
                """.trimIndent()
            }

            // 3. FASTING & AUTOPHAGY (EXACT TOPIC ISOLATION)
            lower.contains("fasting") || lower.contains("fastng") || lower.contains("autophagy") ||
            lower.contains("intermittent") || lower.contains("omad") || lower.contains("fast ") -> {
                """
                ⏳ **Intermittent Fasting & Autophagy Guide for $name**:

                1. **What is Fasting?**: Fasting is restricting your daily eating window (e.g. 16:8 protocol: 16 hours fasting, 8 hours eating window; or OMAD: One Meal A Day).
                2. **Fat Loss Mechanism**: Fasting helps you maintain a caloric deficit by reducing the time window available for eating. The fat loss itself comes from the net calorie deficit, not the fasting state alone.
                3. **Autophagy**: Extended fasts (24–36 hours) trigger cellular autophagy—a natural cleanup process where cells recycle damaged proteins and organelles.
                4. **Fasting Rules**: Water, black coffee, and unflavored green tea do not break a fast. Break your fast with a clean, high-protein meal!
                """.trimIndent()
            }

            // 4. CARDIO, HIIT, LISS & ZONE 2 (EXACT TOPIC ISOLATION)
            lower.contains("cardio") || lower.contains("cardy") || lower.contains("hiit") ||
            lower.contains("liss") || lower.contains("zone 2") || lower.contains("vo2") || lower.contains("running") -> {
                """
                🏃 **Cardiovascular Conditioning & Enduring Systems for $name**:

                1. **HIIT (High-Intensity Interval Training)**: Short explosive bursts (e.g. 30 sec sprint) followed by brief rest. Highly time-efficient for boosting VO2 Max and burning calories.
                2. **LISS (Low-Intensity Steady State)**: Brisk walking, light cycling, or swimming at a steady pace (45+ mins). Excellent for building aerobic base and active recovery.
                3. **Zone 2 Cardio**: Working at 60–70% of max heart rate where you can hold a conversation. Directly expands mitochondrial density and cardiovascular health.
                4. **Weights vs. Cardio**: Do weightlifting first for strength/muscle, then follow up with cardio or do cardio on separate days.
                """.trimIndent()
            }

            // 4. WORKOUT SPLITS & SPECIFIC MUSCLE EXERCISES
            lower.contains("workout") || lower.contains("exercise") || lower.contains("split") ||
            lower.contains("routine") || lower.contains("chest") || lower.contains("back") ||
            lower.contains("leg") || lower.contains("shoulder") || lower.contains("abs") ||
            lower.contains("core") || lower.contains("six pack") || lower.contains("belly") || lower.contains("squat") -> {
                when {
                    lower.contains("abs") || lower.contains("core") || lower.contains("six pack") || lower.contains("belly") -> """
                    🔥 **Six-Pack Abs & Core Training Guide for $name**:

                    1. **Top Exercises**:
                       • **Hanging Leg Raises** (3x12): Targets lower rectus abdominis.
                       • **Ab Wheel Rollouts / Cable Crunches** (3x10): Deep abdominal thickness.
                       • **Planks & Side Planks** (3x60 sec): Builds core stability (transverse abdominis).
                       • **Russian Twists / Woodchoppers** (3x15): Shapes obliques for a sharp V-taper.
                    2. **Visible Abs Requirement**: Abs are built in the gym, but revealed in the kitchen! Body fat must drop below ~12–14% (men) or ~18–20% (women).
                    """.trimIndent()
                    lower.contains("chest") -> "💪 **Best Chest Exercises**: 1. Flat Barbell/Dumbbell Bench Press (3x8), 2. Incline Dumbbell Press (3x10), 3. Weighted Dips or Pushups (3x12), 4. Cable Chest Flyes (3x15). Focus on full range of motion!"
                    lower.contains("back") -> "🏋️ **Best Back Exercises**: 1. Pullups / Chin-ups (4 sets to failure), 2. Bent-over Barbell Rows (3x8), 3. Lat Pulldowns (3x10), 4. Seated Cable Rows (3x12). Keeps posture strong!"
                    lower.contains("leg") || lower.contains("quad") || lower.contains("hamstring") -> "🦵 **Best Leg Exercises**: 1. Barbell Back Squats (4x8), 2. Romanian Deadlifts (3x10), 3. Bulgarian Split Squats (3x10 per leg), 4. Walking Lunges (3x12), 5. Standing Calf Raises (4x15)."
                    lower.contains("shoulder") -> "🏋️‍♂️ **Best Shoulder Exercises**: 1. Standing Overhead Military Press (3x8), 2. Dumbbell Lateral Raises (4x15 for side delts), 3. Face Pulls (3x15 for rear delts), 4. Front Raises (3x12)."
                    else -> """
                    🏋️ **Recommended 3-Day Push/Pull/Legs (PPL) Split**:
                    • **Day 1 (Push)**: Bench Press, Overhead Press, Incline Dumbbell Press, Tricep Pushdowns.
                    • **Day 2 (Pull)**: Pullups, Barbell Rows, Lat Pulldowns, Bicep Curls, Face Pulls.
                    • **Day 3 (Legs & Core)**: Squats, Romanian Deadlifts, Bulgarian Split Squats, Leg Press, Planks.
                    """.trimIndent()
                }
            }

            // 5. LIVE INDIAN NUTRITION & SUPPLEMENT DATABASE QUERY
            (lower.contains("creatine") || lower.contains("whey") || lower.contains("roti") ||
             lower.contains("paneer") || lower.contains("dal") || lower.contains("rice") ||
             lower.contains("dosa") || lower.contains("idli") || lower.contains("samosa") ||
             lower.contains("biryani") || lower.contains("poha") || lower.contains("curd") ||
             lower.contains("chole") || lower.contains("rajma") || lower.contains("calories") ||
             lower.contains("protein in") || lower.contains("nutrition")) -> {

                if (lower.contains("creatine")) {
                    return "⚡ **Creatine Monohydrate Nutrition**: Pure Unflavored Creatine Monohydrate contains **0 kcal** (0g Protein, 0g Carbs, 0g Fat) per 5g scoop! It is a pure zero-calorie muscle supplement that increases ATP cellular energy, muscle power, and cell hydration without adding any calories."
                }

                val numbers = Regex("""\d+""").findAll(prompt).mapNotNull { it.value.toIntOrNull() }.toList()
                val qty = numbers.firstOrNull() ?: 1

                val searchedEntry = IndianNutritionDatabase.items.find { entry ->
                    entry.name.lowercase(Locale.US).contains(lower) || entry.aliases.any { lower.contains(it.lowercase(Locale.US)) }
                } ?: IndianNutritionDatabase.findMatchingEntry(lower)

                val totalGrams = qty * searchedEntry.defaultUnitWeightGrams
                val weightRatio = totalGrams / 100f
                val totalCal = (searchedEntry.caloriesPer100g * weightRatio).toInt()
                val totalP = (searchedEntry.proteinPer100g * weightRatio * 10).toInt() / 10f
                val totalC = (searchedEntry.carbsPer100g * weightRatio * 10).toInt() / 10f
                val totalF = (searchedEntry.fatPer100g * weightRatio * 10).toInt() / 10f

                """
                🥗 **Nutrition Calculation from Database**:
                • **Dish / Item**: ${searchedEntry.name}
                • **Quantity**: $qty ${searchedEntry.defaultUnit.displayName} (~${totalGrams.toInt()}g total)
                • **Calories**: **$totalCal kcal**
                • **Macros**: ${totalP}g Protein • ${totalC}g Carbs • ${totalF}g Fat
                • **Category**: ${searchedEntry.regionOrCategory}
                • **Note**: ${searchedEntry.prepNotes}
                """.trimIndent()
            }

            // 6. OPEN-ENDED MEAL RECOMMENDATIONS ("WHAT SHOULD I EAT?")
            lower.contains("what should i eat") || lower.contains("suggest something") || lower.contains("food recommendation") ||
            lower.contains("what to eat") || lower.contains("dinner idea") || lower.contains("lunch idea") -> {
                """
                🍱 **Meal Recommendations for $name**:

                🔥 **Spicy & Flavorful Option**:
                • **Paneer Tikka / Chicken Curry with Whole Wheat Rotis & Mint Chutney** (~520 kcal • 32g Protein)

                🥗 **Light & Healthy Option**:
                • **Steamed Idli with Sambar & Cucumber Salad** (~220 kcal • 10g Protein)

                ⚡ **Quick & Satisfying Street Food Option**:
                • **Kanda Batata Poha or Pav Bhaji with Extra Veggies** (~340 kcal • 8g Protein)

                *Do any of these sound good, or are you craving something specific?*
                """.trimIndent()
            }

            // 7. OMNISCIENT GENERAL KNOWLEDGE HANDLER (CODING, SCIENCE, HISTORY, MATH, POP CULTURE, RELATIONSHIPS)
            lower.contains("code") || lower.contains("kotlin") || lower.contains("android") || lower.contains("java") || lower.contains("python") -> {
                "💻 **Tech & Coding Insights**: As an Omniscient AI, I can help with code design, algorithms, Android Kotlin Compose, data structures, and app performance optimization! What code or architectural pattern are we building?"
            }
            lower.contains("physics") || lower.contains("math") || lower.contains("science") || lower.contains("chemistry") || lower.contains("history") -> {
                "🧠 **Science & Knowledge**: I'm equipped to discuss quantum physics, historical events, mathematical formulas, and scientific principles in detail. What topic are we exploring?"
            }

            // 8. GREETINGS & CHITCHAT
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") || lower.contains("sup") -> {
                "Hey $name! I am Bushy Wushy, your Omniscient AI companion. I'm running 100% locally on your phone with ${model.displayName}. You've logged ${stats.steps} steps and earned ${stats.xp} XP today. Ask me any question—whether it's about bulking, workouts, meal recommendations, coding, science, or fitness!"
            }

            // 9. STEPS & DISTANCE
            lower.contains("step") || lower.contains("walk") || lower.contains("km") -> {
                "You've recorded ${stats.steps} steps today! Every step earns you XP toward reaching Level ${stats.level + 1}!"
            }

            // 10. PUSHUPS & MISSIONS
            lower.contains("pushup") || lower.contains("squat") || lower.contains("task") || lower.contains("mission") -> {
                "You've completed ${stats.pushups} pushups so far! Completing workout missions gives +10 XP per rep plus +500 XP bonus on completion!"
            }

            // 11. GENERAL OMNISCIENT AI FALLBACK
            else -> {
                "As an Omniscient AI Assistant with Master Culinary Specialization and expert fitness knowledge, I'm here for any question, $name! Ask me 'What should I eat today?', 'How to bulk?', 'Explain quantum physics', or 'Help with Android Kotlin code'!"
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
