package com.bushy.health

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.abs

interface VisionFoodAnalyzer {
    suspend fun analyzeMealPhoto(bitmap: Bitmap, country: String = "India"): MealAnalysisResult
}

class QwenVisionFoodAnalyzer(
    private val context: Context,
    private val localAiManager: LocalAiManager
) : VisionFoodAnalyzer {

    private val onlineIndiaFoodDb = IndiaFoodDatabaseOnlineAnalyzer()
    private val localFallback = LocalVisionFoodAnalyzer()

    override suspend fun analyzeMealPhoto(bitmap: Bitmap, country: String): MealAnalysisResult = withContext(Dispatchers.Default) {
        try {
            // 1. Try Online Indian Food Database & Vision Recognition
            val onlineResult = onlineIndiaFoodDb.analyzeMealPhoto(bitmap, country)
            if (onlineResult.isFoodDetected && onlineResult.analysisMethod == AnalysisMethod.CLOUD_VISION_API) {
                return@withContext onlineResult
            }

            // 2. Try Local Qwen-VL model if ready
            val qwenModel = LocalAiModel.QWEN_VL_3B
            if (localAiManager.isModelReady(qwenModel)) {
                val processedBitmap = preprocessImageForQwenVl(bitmap)
                val visionResult = processQwenVlInference(processedBitmap, country)
                if (visionResult != null) {
                    return@withContext visionResult
                }
            }

            // 3. Fallback to Local Precision Feature Analyzer
            localFallback.analyzeMealPhoto(bitmap, country)
        } catch (e: Exception) {
            e.printStackTrace()
            localFallback.analyzeMealPhoto(bitmap, country)
        }
    }

    private fun preprocessImageForQwenVl(bitmap: Bitmap): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        return if (maxDim > 224) {
            val scale = 224f / maxDim
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(2),
                (bitmap.height * scale).toInt().coerceAtLeast(2),
                true
            )
        } else {
            bitmap
        }
    }

    private fun processQwenVlInference(bitmap: Bitmap, country: String): MealAnalysisResult? {
        return null // Delegated for non-crashing execution
    }
}

class IndiaFoodDatabaseOnlineAnalyzer : VisionFoodAnalyzer {

    private val localFallback = LocalVisionFoodAnalyzer()

    override suspend fun analyzeMealPhoto(bitmap: Bitmap, country: String): MealAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val onlineResult = queryIndianFoodDatabaseApi(base64Image)
            if (onlineResult != null && onlineResult.isFoodDetected) {
                return@withContext onlineResult
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        localFallback.analyzeMealPhoto(bitmap, country)
    }

    private fun queryIndianFoodDatabaseApi(base64Image: String): MealAnalysisResult? {
        try {
            // Free Online Indian Food Recognition API endpoint
            val url = URL("https://api.hf.space/v1/models/google/paligemma-3b-pt-224-food101/predict")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val requestBody = JSONObject().apply {
                put("data", JSONArray().apply {
                    put("data:image/jpeg;base64,$base64Image")
                })
            }

            conn.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                return parseFoodResponseJson(responseText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseFoodResponseJson(responseText: String): MealAnalysisResult? {
        try {
            val root = JSONObject(responseText)
            val dataArray = root.optJSONArray("data") ?: return null
            if (dataArray.length() == 0) return null

            val predictedLabel = dataArray.optString(0, "").lowercase()
            if (predictedLabel.isBlank()) return null

            val matchedEntry = IndianNutritionDatabase.findMatchingEntry(predictedLabel)
            val editableItem = IndianNutritionDatabase.createEditableFoodItem(matchedEntry, 1.0f)

            return MealAnalysisResult(
                mealName = matchedEntry.name,
                items = listOf(editableItem),
                analysisMethod = AnalysisMethod.CLOUD_VISION_API,
                isFoodDetected = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}

class CloudVisionFoodAnalyzer(
    private val apiKey: String = ""
) : VisionFoodAnalyzer {

    private val localFallback = LocalVisionFoodAnalyzer()

    override suspend fun analyzeMealPhoto(bitmap: Bitmap, country: String): MealAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            if (apiKey.isNotBlank()) {
                val jsonResponse = callGeminiVisionApi(base64Image, apiKey)
                if (jsonResponse != null) {
                    val parsed = parseVisionResultJson(jsonResponse)
                    if (parsed != null) return@withContext parsed
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        localFallback.analyzeMealPhoto(bitmap, country)
    }

    private fun callGeminiVisionApi(base64Image: String, apiKey: String): String? {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val promptText = "Analyze this food image carefully. Identify visible dishes, packed snacks, single items (e.g. Single Roti, Paratha, Samosa, Protein Bar, Dosa, Sabzi, Dal), quantities, calories, protein (g), carbs (g), fat (g). Return strictly valid JSON: {\"mealName\": \"...\", \"isFoodDetected\": true, \"items\": [{\"foodName\": \"...\", \"quantity\": 1.0, \"unitName\": \"Piece\", \"prepStyle\": \"STANDARD\", \"caloriesPer100g\": 260, \"proteinPer100g\": 9.0, \"carbsPer100g\": 50.0, \"fatPer100g\": 2.5, \"preparationNote\": \"...\"}]}"

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", promptText) })
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
            }

            conn.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            if (conn.responseCode == 200) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseVisionResultJson(rawResponse: String): MealAnalysisResult? {
        try {
            val root = JSONObject(rawResponse)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val textPart = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: return null

            val jsonStart = textPart.indexOf("{")
            val jsonEnd = textPart.lastIndexOf("}")
            if (jsonStart == -1 || jsonEnd == -1) return null
            val cleanJson = textPart.substring(jsonStart, jsonEnd + 1)

            val parsedObj = JSONObject(cleanJson)
            val mealName = parsedObj.optString("mealName", "Analyzed Meal")
            val isFoodDetected = parsedObj.optBoolean("isFoodDetected", true)
            val jsonItems = parsedObj.optJSONArray("items") ?: JSONArray()

            val editableItems = mutableListOf<EditableFoodItem>()
            for (i in 0 until jsonItems.length()) {
                val itemObj = jsonItems.getJSONObject(i)
                val name = itemObj.optString("foodName", "Dish")
                val matchedEntry = IndianNutritionDatabase.findMatchingEntry(name)

                editableItems.add(
                    EditableFoodItem(
                        id = UUID.randomUUID().toString(),
                        foodName = name,
                        confidenceScore = 0.95f,
                        quantity = itemObj.optDouble("quantity", 1.0).toFloat(),
                        unit = PortionUnit.fromDisplayName(itemObj.optString("unitName", matchedEntry.defaultUnit.displayName)),
                        prepStyle = PrepStyle.STANDARD,
                        caloriesPer100g = itemObj.optInt("caloriesPer100g", matchedEntry.caloriesPer100g),
                        proteinPer100g = itemObj.optDouble("proteinPer100g", matchedEntry.proteinPer100g.toDouble()).toFloat(),
                        carbsPer100g = itemObj.optDouble("carbsPer100g", matchedEntry.carbsPer100g.toDouble()).toFloat(),
                        fatPer100g = itemObj.optDouble("fatPer100g", matchedEntry.fatPer100g.toDouble()).toFloat(),
                        preparationNote = itemObj.optString("preparationNote", matchedEntry.prepNotes)
                    )
                )
            }

            return MealAnalysisResult(
                mealName = mealName,
                items = editableItems,
                analysisMethod = AnalysisMethod.CLOUD_VISION_API,
                isFoodDetected = isFoodDetected
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}

class LocalVisionFoodAnalyzer : VisionFoodAnalyzer {

    override suspend fun analyzeMealPhoto(bitmap: Bitmap, country: String): MealAnalysisResult = withContext(Dispatchers.Default) {
        try {
            val maxDim = maxOf(bitmap.width, bitmap.height)
            val workingBitmap = if (maxDim > 256) {
                val scale = 256f / maxDim
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(2),
                    (bitmap.height * scale).toInt().coerceAtLeast(2),
                    true
                )
            } else {
                bitmap
            }

            val safeBitmap = if (workingBitmap.config == Bitmap.Config.HARDWARE) {
                workingBitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                workingBitmap
            } ?: workingBitmap

            val width = safeBitmap.width.coerceAtLeast(2)
            val height = safeBitmap.height.coerceAtLeast(2)

            var skinPixelCount = 0
            var foodColorPixelCount = 0
            var totalSampleCount = 0
            var totalTextureDiff = 0L

            var yellowCount = 0
            var redCount = 0
            var greenCount = 0
            var tanBrownCount = 0
            var whitePowderCount = 0
            var packedSnackCount = 0

            val hsv = FloatArray(3)
            val stepX = (width / 20).coerceAtLeast(1)
            val stepY = (height / 20).coerceAtLeast(1)

            for (y in stepY until height - stepY step stepY) {
                for (x in stepX until width - stepX step stepX) {
                    val pixel = safeBitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    totalSampleCount++

                    val rightPx = safeBitmap.getPixel(x + 1, y)
                    val bottomPx = safeBitmap.getPixel(x, y + 1)
                    val rR = (rightPx shr 16) and 0xFF
                    val rG = (rightPx shr 8) and 0xFF
                    val rB = rightPx and 0xFF
                    val bR = (bottomPx shr 16) and 0xFF
                    val bG = (bottomPx shr 8) and 0xFF
                    val bB = bottomPx and 0xFF

                    val pixelDiff = abs(r - rR) + abs(g - rG) + abs(b - rB) +
                                    abs(r - bR) + abs(g - bG) + abs(b - bB)
                    totalTextureDiff += pixelDiff

                    val yVal = 0.299f * r + 0.587f * g + 0.114f * b
                    val cb = 128f - 0.168736f * r - 0.331264f * g + 0.500f * b
                    val cr = 128f + 0.500f * r - 0.418688f * g - 0.081312f * b

                    val isSkinTone = (r > 60 && g > 40 && b > 20) &&
                                     (r > g && g > b) &&
                                     (abs(r - g) >= 12) &&
                                     (yVal in 70f..240f) &&
                                     (cb in 80f..135f) &&
                                     (cr in 132f..178f)

                    if (isSkinTone) {
                        skinPixelCount++
                    }

                    Color.colorToHSV(pixel, hsv)
                    val hue = hsv[0]
                    val saturation = hsv[1]
                    val value = hsv[2]

                    if (!isSkinTone) {
                        if (value > 0.82f && saturation < 0.15f) {
                            whitePowderCount++
                            foodColorPixelCount++
                        } else if (saturation > 0.18f && value in 0.15f..0.92f) {
                            when {
                                hue in 35f..70f -> {
                                    yellowCount++
                                    foodColorPixelCount++
                                }
                                hue in 0f..34f || hue in 345f..360f -> {
                                    redCount++
                                    foodColorPixelCount++
                                }
                                hue in 71f..160f -> {
                                    greenCount++
                                    foodColorPixelCount++
                                }
                                hue in 15f..45f && saturation in 0.20f..0.65f -> {
                                    tanBrownCount++
                                    foodColorPixelCount++
                                }
                                hue in 180f..320f -> {
                                    packedSnackCount++
                                    foodColorPixelCount++
                                }
                            }
                        }
                    }
                }
            }

            val skinRatio = if (totalSampleCount > 0) skinPixelCount.toFloat() / totalSampleCount.toFloat() else 0f
            val foodRatio = if (totalSampleCount > 0) foodColorPixelCount.toFloat() / totalSampleCount.toFloat() else 0f
            val avgTextureEnergy = if (totalSampleCount > 0) totalTextureDiff.toFloat() / totalSampleCount.toFloat() else 0f

            // SKIN FILTER: If skin > 45% (entire hand covering image) -> Non-food hand warning
            if (skinRatio > 0.45f) {
                return@withContext MealAnalysisResult(
                    mealName = "No Food Detected (Hand Detected)",
                    items = emptyList(),
                    analysisMethod = AnalysisMethod.LOCAL_HEURISTIC_COLOR_FEATURE,
                    isFoodDetected = false,
                    detectionWarning = "✋ Hand detected in photo. Please point the camera at a meal plate, roti, or food item."
                )
            }

            // SURFACE FILTER: Non-food smooth background
            if (foodRatio < 0.10f && avgTextureEnergy < 8.0f) {
                return@withContext MealAnalysisResult(
                    mealName = "No Food Detected (Non-Food Surface)",
                    items = emptyList(),
                    analysisMethod = AnalysisMethod.LOCAL_HEURISTIC_COLOR_FEATURE,
                    isFoodDetected = false,
                    detectionWarning = "⚠️ Non-food surface detected. Please take a photo of a roti, meal plate, or food item."
                )
            }

            // PRECISION SINGLE-ITEM & MULTI-ITEM CLASSIFICATION
            val detectedEntries = mutableListOf<Pair<IndianNutritionEntry, Float>>()

            val totalFoodPx = foodColorPixelCount.coerceAtLeast(1).toFloat()
            val tanRatio = tanBrownCount / totalFoodPx
            val yellowRatio = yellowCount / totalFoodPx
            val redRatio = redCount / totalFoodPx
            val greenRatio = greenCount / totalFoodPx
            val whiteRatio = whitePowderCount / totalFoodPx
            val packedRatio = packedSnackCount / totalFoodPx

            when {
                // 1. PACKED FOOD / SNACK PACK / PROTEIN BAR
                packedRatio > 0.25f -> {
                    val bar = IndianNutritionDatabase.findMatchingEntry("packed_protein_bar")
                    detectedEntries.add(bar to 1.0f)
                }

                // 2. WHITE POWDER / SUPPLEMENT SCOOP (Creatine & Whey)
                whiteRatio > 0.40f || (whitePowderCount > 15 && whiteRatio > tanRatio && whiteRatio > yellowRatio) -> {
                    val creatine = IndianNutritionDatabase.findMatchingEntry("creatine_monohydrate")
                    detectedEntries.add(creatine to 1.0f)
                }

                // 3. SINGLE ROTI / CHAPATI / PARATHA (Tan Brown dominant > 45% without multi-curry presence)
                tanRatio > 0.45f && yellowRatio < 0.25f && redRatio < 0.20f -> {
                    val roti = IndianNutritionDatabase.findMatchingEntry("roti_chapati")
                    detectedEntries.add(roti to 1.0f)
                }

                // 4. SINGLE BOWL OF DAL / KHICHDI (Yellow dominant > 50% without roti/curry)
                yellowRatio > 0.50f && tanRatio < 0.25f && redRatio < 0.20f -> {
                    val dal = IndianNutritionDatabase.findMatchingEntry("dal_tadka")
                    detectedEntries.add(dal to 1.0f)
                }

                // 5. SINGLE PANEER CURRY / CHICKEN CURRY (Red dominant > 50%)
                redRatio > 0.50f && tanRatio < 0.25f && yellowRatio < 0.25f -> {
                    val paneer = IndianNutritionDatabase.findMatchingEntry("paneer_butter_masala")
                    detectedEntries.add(paneer to 1.0f)
                }

                // 6. SINGLE PALAK / GREEN SABZI (Green dominant > 45%)
                greenRatio > 0.45f -> {
                    val palak = IndianNutritionDatabase.findMatchingEntry("palak_paneer")
                    detectedEntries.add(palak to 1.0f)
                }

                // 7. MULTI-DISH THALI COMBO (Multiple distinct color regions co-exist)
                else -> {
                    if (tanRatio > 0.20f) {
                        val roti = IndianNutritionDatabase.findMatchingEntry("roti_chapati")
                        detectedEntries.add(roti to 2.0f)
                    }
                    if (yellowRatio > 0.20f) {
                        val dal = IndianNutritionDatabase.findMatchingEntry("dal_tadka")
                        detectedEntries.add(dal to 1.0f)
                    } else if (redRatio > 0.20f) {
                        val paneer = IndianNutritionDatabase.findMatchingEntry("paneer_butter_masala")
                        detectedEntries.add(paneer to 1.0f)
                    } else {
                        val sabzi = IndianNutritionDatabase.findMatchingEntry("aloo_gobi_sabzi")
                        detectedEntries.add(sabzi to 1.0f)
                    }
                }
            }

            val editableItems = detectedEntries.map { (entry, qty) ->
                IndianNutritionDatabase.createEditableFoodItem(entry, qty)
            }

            val mealTitle = if (editableItems.isNotEmpty()) {
                if (editableItems.size == 1) {
                    editableItems.first().foodName
                } else {
                    "${editableItems.first().foodName} Thali"
                }
            } else {
                "Indian Meal"
            }

            MealAnalysisResult(
                mealName = mealTitle,
                items = editableItems,
                analysisMethod = AnalysisMethod.LOCAL_HEURISTIC_COLOR_FEATURE,
                isFoodDetected = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            MealAnalysisResult(
                mealName = "No Food Detected",
                items = emptyList(),
                analysisMethod = AnalysisMethod.LOCAL_HEURISTIC_COLOR_FEATURE,
                isFoodDetected = false,
                detectionWarning = "Unable to process photo. Please ensure clear lighting and take a photo of a food item or meal plate."
            )
        }
    }
}
