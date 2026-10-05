package com.bushy.health

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun levelProgression_isCorrect() {
        val statsZero = UserStats(xp = 0)
        assertEquals(1, statsZero.level)
        assertEquals(0L, statsZero.xpInCurrentLevel)
        assertEquals(100L, statsZero.xpRequiredForNextLevel)
        assertEquals(0f, statsZero.levelProgress, 0.001f)

        val statsLvl2 = UserStats(xp = 100)
        assertEquals(2, statsLvl2.level)
        assertEquals(0L, statsLvl2.xpInCurrentLevel)
        assertEquals(300L, statsLvl2.xpRequiredForNextLevel)

        val statsLvl3 = UserStats(xp = 400)
        assertEquals(3, statsLvl3.level)
    }

    @Test
    fun caloriesCalculation_isCorrect() {
        val stats = UserStats(steps = 2500, bonusCalories = 150)
        // 2500 * 0.04 = 100 + 150 = 250
        assertEquals(250, stats.calories)
    }

    @Test
    fun macrosSum_isCorrect() {
        val meals = listOf(
            LoggedMeal(name = "Dal Rice", calories = 340, proteinGrams = 16, carbsGrams = 68, fatGrams = 8),
            LoggedMeal(name = "Paneer Tikka", calories = 460, proteinGrams = 24, carbsGrams = 48, fatGrams = 18)
        )
        val stats = UserStats(loggedMeals = meals)
        assertEquals(40, stats.totalProteinGrams)
        assertEquals(116, stats.totalCarbsGrams)
        assertEquals(26, stats.totalFatGrams)
    }

    @Test
    fun taskParser_exercisesAndGoals_parsedAccurately() {
        val pushupTask = BushyAIViewModel.tryParseTaskLocal("Add 25 pushups")
        assertNotNull(pushupTask)
        assertEquals(TaskType.PUSHUPS, pushupTask!!.type)
        assertEquals(25, pushupTask.target)
        assertEquals("25 Pushups", pushupTask.title)

        val waterTask = BushyAIViewModel.tryParseTaskLocal("Drink 10 glasses of water")
        assertNotNull(waterTask)
        assertEquals(TaskType.WATER, waterTask!!.type)
        assertEquals(10, waterTask.target)

        val stepTask = BushyAIViewModel.tryParseTaskLocal("Walk 8000 steps")
        assertNotNull(stepTask)
        assertEquals(TaskType.STEPS, stepTask!!.type)
        assertEquals(8000, stepTask.target)

        val plankTask = BushyAIViewModel.tryParseTaskLocal("Hold plank for 45 sec")
        assertNotNull(plankTask)
        assertEquals(TaskType.GENERAL, plankTask!!.type)
        assertEquals(45, plankTask.target)

        val customTask = BushyAIViewModel.tryParseTaskLocal("Add mission Meditate")
        assertNotNull(customTask)
        assertEquals(TaskType.GENERAL, customTask!!.type)
    }

    @Test
    fun taskParser_questionsAndCasualChat_ignored() {
        assertNull(BushyAIViewModel.tryParseTaskLocal("Can I drink water after workout?"))
        assertNull(BushyAIViewModel.tryParseTaskLocal("What are pushups?"))
        assertNull(BushyAIViewModel.tryParseTaskLocal("How many steps should I walk?"))
        assertNull(BushyAIViewModel.tryParseTaskLocal("What's new?"))
        assertNull(BushyAIViewModel.tryParseTaskLocal("I love doing pushups in the morning"))
        assertNull(BushyAIViewModel.tryParseTaskLocal("The water is cold"))
        assertNull(BushyAIViewModel.tryParseTaskLocal("Tell me about protein"))
    }
}