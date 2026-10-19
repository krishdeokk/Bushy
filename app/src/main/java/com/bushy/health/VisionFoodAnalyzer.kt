package com.bushy.health

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

interface VisionFoodAnalyzer {
    suspend fun analyzeMealPhoto(bitmap: Bitmap, country: String = "India"): MealAnalysisResult
}

class LocalVisionFoodAnalyzer : VisionFoodAnalyzer {

    override suspend fun analyzeMealPhoto(bitmap: Bitmap, country: String): MealAnalysisResult = withContext(Dispatchers.Default) {
        try {
            // Downscale image to 256x256 max for fast software pixel grid and texture inspection
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

                    // Calculate local spatial texture difference (adjacent pixel energy)
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

                    // 1. HUMAN SKIN DETECTION (YCbCr & RGB Skin Tone Model)
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

                    // 2. FOOD COLOR SPECTRUM CLASSIFICATION (HSV)
                    Color.colorToHSV(pixel, hsv)
                    val hue = hsv[0]        // 0..360
                    val saturation = hsv[1] // 0..1
                    val value = hsv[2]      // 0..1

                    if (!isSkinTone) {
                        // Check for White Powder / Supplement Scoop (High Value, Very Low Saturation)
                        if (value > 0.82f && saturation < 0.15f) {
                            whitePowderCount++
                            foodColorPixelCount++
                        } else if (saturation > 0.18f && value in 0.15f..0.92f) {
                            when {
                                // Yellow / Golden (Dal, Basmati Rice, Dhokla, Khichdi)
                                hue in 35f..70f -> {
                                    yellowCount++
                                    foodColorPixelCount++
                                }
                                // Red / Orange (Paneer Butter Masala, Chicken Curry, Rajma, Chole)
                                hue in 0f..34f || hue in 345f..360f -> {
                                    redCount++
                                    foodColorPixelCount++
                                }
                                // Green (Palak, Saag, Chutney, Fresh Salad, Bhindi)
                                hue in 71f..160f -> {
                                    greenCount++
                                    foodColorPixelCount++
                                }
                                // Tan / Golden Brown (Roti, Paratha, Naan, Dosa, Samosa)
                                hue in 15f..45f && saturation in 0.20f..0.65f -> {
                                    tanBrownCount++
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

            // FILTER 1: HUMAN SKIN DETECTED (Hand / Arm / Body in photo)
            if (skinRatio > 0.25f) {
                return@withContext MealAnalysisResult(
                    mealName = "No Food Detected (Hand/Skin Detected)",
                    items = emptyList(),
                    analysisMethod = AnalysisMethod.LOCAL_HEURISTIC_COLOR_FEATURE,
                    isFoodDetected = false,
                    detectionWarning = "✋ Hand or human skin detected in photo. Please point the camera at a meal plate or food dish, or select items manually below."
                )
            }

            // FILTER 2: NON-FOOD SMOOTH OR NEUTRAL SURFACE (Desk, keyboard, paper, wall, screen)
            if (foodRatio < 0.15f || avgTextureEnergy < 12.0f) {
                return@withContext MealAnalysisResult(
                    mealName = "No Food Detected (Non-Food Surface)",
                    items = emptyList(),
                    analysisMethod = AnalysisMethod.LOCAL_HEURISTIC_COLOR_FEATURE,
                    isFoodDetected = false,
                    detectionWarning = "⚠️ Non-food surface detected (keyboard, desk, paper, or screen). Please take a photo of an Indian meal plate, or add food items manually below."
                )
            }

            // FILTER 3: VERIFIED FOOD PLATE / SUPPLEMENT -> MAP DISHES FROM NUTRITION DATABASE
            val detectedEntries = mutableListOf<Pair<IndianNutritionEntry, Float>>()

            when {
                // White Powder / Supplement Scoop -> Creatine Monohydrate & Whey Protein
                whitePowderCount >= yellowCount && whitePowderCount >= redCount && whitePowderCount > 10 -> {
                    val creatine = IndianNutritionDatabase.findMatchingEntry("creatine_monohydrate")
                    val whey = IndianNutritionDatabase.findMatchingEntry("whey_protein")
                    detectedEntries.add(creatine to 1.0f)
                    detectedEntries.add(whey to 1.0f)
                }

                // Golden / Yellow dominance -> Dal, Rice, Roti
                yellowCount >= redCount && yellowCount >= greenCount && yellowCount > 0 -> {
                    val dal = IndianNutritionDatabase.findMatchingEntry("dal_tadka")
                    val rice = IndianNutritionDatabase.findMatchingEntry("basmati_rice")
                    val roti = IndianNutritionDatabase.findMatchingEntry("roti_chapati")
                    detectedEntries.add(dal to 1.0f)
                    detectedEntries.add(rice to 1.0f)
                    detectedEntries.add(roti to 2.0f)
                }

                // Reddish / Orange dominance -> Paneer Butter Masala / Chicken Curry / Naan
                redCount > yellowCount && redCount >= greenCount -> {
                    val paneer = IndianNutritionDatabase.findMatchingEntry("paneer_butter_masala")
                    val naan = IndianNutritionDatabase.findMatchingEntry("butter_naan")
                    val curd = IndianNutritionDatabase.findMatchingEntry("plain_curd_dahi")
                    detectedEntries.add(paneer to 1.0f)
                    detectedEntries.add(naan to 1.0f)
                    detectedEntries.add(curd to 1.0f)
                }

                // Green dominance -> Palak Paneer / Sabzi / Roti
                greenCount > yellowCount && greenCount > redCount -> {
                    val palak = IndianNutritionDatabase.findMatchingEntry("palak_paneer")
                    val roti = IndianNutritionDatabase.findMatchingEntry("roti_chapati")
                    val sabzi = IndianNutritionDatabase.findMatchingEntry("aloo_gobi_sabzi")
                    detectedEntries.add(palak to 1.0f)
                    detectedEntries.add(roti to 2.0f)
                    detectedEntries.add(sabzi to 1.0f)
                }

                // Tan / Golden Brown -> Roti, Sabzi, Curd
                else -> {
                    val roti = IndianNutritionDatabase.findMatchingEntry("roti_chapati")
                    val sabzi = IndianNutritionDatabase.findMatchingEntry("aloo_gobi_sabzi")
                    val curd = IndianNutritionDatabase.findMatchingEntry("plain_curd_dahi")
                    detectedEntries.add(roti to 2.0f)
                    detectedEntries.add(sabzi to 1.0f)
                    detectedEntries.add(curd to 1.0f)
                }
            }

            val editableItems = detectedEntries.map { (entry, qty) ->
                IndianNutritionDatabase.createEditableFoodItem(entry, qty)
            }

            val mealTitle = if (editableItems.isNotEmpty()) {
                "${editableItems.first().foodName} Thali"
            } else {
                "Indian Meal Thali"
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
                detectionWarning = "Unable to process photo. Please ensure clear lighting and take a photo of an Indian meal plate."
            )
        }
    }
}
