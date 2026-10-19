package com.bushy.health

import java.util.Locale
import kotlin.math.min

enum class LanguageScript(val displayName: String, val languageName: String) {
    DEVANAGARI("Devanagari", "Hindi / Marathi"),
    BENGALI("Bengali", "Bengali / Assamese"),
    TAMIL("Tamil", "Tamil"),
    TELUGU("Telugu", "Telugu"),
    GUJARATI("Gujarati", "Gujarati"),
    KANNADA("Kannada", "Kannada"),
    MALAYALAM("Malayalam", "Malayalam"),
    GURMUKHI("Gurmukhi", "Punjabi"),
    LATIN_ENGLISH("Latin/English", "English & Transliteration");

    companion object {
        fun detectScript(text: String): LanguageScript {
            for (char in text) {
                val code = char.code
                when (code) {
                    in 0x0900..0x097F -> return DEVANAGARI
                    in 0x0980..0x09FF -> return BENGALI
                    in 0x0B80..0x0BFF -> return TAMIL
                    in 0x0C00..0x0C7F -> return TELUGU
                    in 0x0A80..0x0AFF -> return GUJARATI
                    in 0x0C80..0x0CFF -> return KANNADA
                    in 0x0D00..0x0D7F -> return MALAYALAM
                    in 0x0A00..0x0A7F -> return GURMUKHI
                }
            }
            return LATIN_ENGLISH
        }
    }
}

data class DishInterpretationResult(
    val originalInput: String,
    val normalizedName: String,
    val script: LanguageScript,
    val confidenceScore: Float, // 0.0 to 1.0
    val bestMatch: IndianNutritionEntry?,
    val candidateMatches: List<IndianNutritionEntry>,
    val suggestedCorrection: String?,
    val isAmbiguous: Boolean
)

object MultilingualDishInterpreter {

    fun interpretDishText(rawInput: String): DishInterpretationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            val fallback = IndianNutritionDatabase.defaultItems.first()
            return DishInterpretationResult(
                originalInput = rawInput,
                normalizedName = fallback.name,
                script = LanguageScript.LATIN_ENGLISH,
                confidenceScore = 0.0f,
                bestMatch = fallback,
                candidateMatches = emptyList(),
                suggestedCorrection = null,
                isAmbiguous = false
            )
        }

        val script = LanguageScript.detectScript(trimmed)
        val normalizedInput = trimmed.lowercase(Locale.ROOT)
            .replace(Regex("""[^\p{L}\d\s]"""), "")
            .replace(Regex("""\s+"""), " ")

        val allDatabaseItems = IndianNutritionDatabase.items

        // 1. Direct Alias Match
        val directMatches = allDatabaseItems.filter { entry ->
            entry.name.lowercase(Locale.ROOT) == normalizedInput ||
            entry.aliases.any { alias -> alias.lowercase(Locale.ROOT) == normalizedInput }
        }

        if (directMatches.isNotEmpty()) {
            val best = directMatches.first()
            return DishInterpretationResult(
                originalInput = rawInput,
                normalizedName = best.name,
                script = script,
                confidenceScore = 1.0f,
                bestMatch = best,
                candidateMatches = directMatches,
                suggestedCorrection = null,
                isAmbiguous = directMatches.size > 2
            )
        }

        // 2. Substring & Partial Match
        val partialMatches = allDatabaseItems.filter { entry ->
            entry.name.lowercase(Locale.ROOT).contains(normalizedInput) ||
            entry.aliases.any { alias -> alias.lowercase(Locale.ROOT).contains(normalizedInput) || normalizedInput.contains(alias.lowercase(Locale.ROOT)) }
        }

        // Check if query is a generic ambiguous term like "dal", "paneer", "rice", "curry", "dosa"
        val isGenericQuery = normalizedInput in listOf("dal", "daal", "दाल", "paneer", "पनीर", "rice", "चावल", "curry", "dosa", "डोसा", "paratha", "पराठा")

        if (partialMatches.isNotEmpty()) {
            val best = partialMatches.first()
            val correction = if (best.name.lowercase(Locale.ROOT) != normalizedInput) best.name else null
            return DishInterpretationResult(
                originalInput = rawInput,
                normalizedName = best.name,
                script = script,
                confidenceScore = if (isGenericQuery) 0.65f else 0.88f,
                bestMatch = best,
                candidateMatches = partialMatches,
                suggestedCorrection = correction,
                isAmbiguous = isGenericQuery || partialMatches.size > 1
            )
        }

        // 3. Fuzzy Levenshtein Distance Typo Matching
        var bestFuzzyEntry: IndianNutritionEntry? = null
        var minDistance = Int.MAX_VALUE
        var matchedAlias: String? = null

        for (entry in allDatabaseItems) {
            val aliasList = listOf(entry.name) + entry.aliases
            for (alias in aliasList) {
                val distance = calculateLevenshteinDistance(normalizedInput, alias.lowercase(Locale.ROOT))
                if (distance < minDistance && distance <= 3) {
                    minDistance = distance
                    bestFuzzyEntry = entry
                    matchedAlias = alias
                }
            }
        }

        if (bestFuzzyEntry != null && minDistance <= 3) {
            val confidence = (1.0f - (minDistance.toFloat() / normalizedInput.length.coerceAtLeast(1).toFloat())).coerceIn(0.5f, 0.9f)
            return DishInterpretationResult(
                originalInput = rawInput,
                normalizedName = bestFuzzyEntry.name,
                script = script,
                confidenceScore = confidence,
                bestMatch = bestFuzzyEntry,
                candidateMatches = listOf(bestFuzzyEntry),
                suggestedCorrection = bestFuzzyEntry.name,
                isAmbiguous = false
            )
        }

        // 4. No Match Fallback
        return DishInterpretationResult(
            originalInput = rawInput,
            normalizedName = trimmed,
            script = script,
            confidenceScore = 0.0f,
            bestMatch = null,
            candidateMatches = emptyList(),
            suggestedCorrection = null,
            isAmbiguous = false
        )
    }

    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1, // Deletion
                    min(
                        dp[i][j - 1] + 1, // Insertion
                        dp[i - 1][j - 1] + cost // Substitution
                    )
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}
