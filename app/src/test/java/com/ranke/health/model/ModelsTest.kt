package com.ranke.health.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.DayOfWeek
import java.time.LocalDate
import com.ranke.health.data.FoodCatalogRepository

class ModelsTest {
    @Test
    fun `workout statistics count completed weeks and muscle sets`() {
        val monday = LocalDate.of(2026, 8, 24)
        val completed = listOf(
            WorkoutDay(monday, DayMode.TRAINING, listOf(WorkoutExercise(1, "卧推", 4, "8", "60kg")), isCompleted = true),
            WorkoutDay(monday.plusDays(2), DayMode.TRAINING, listOf(WorkoutExercise(2, "深蹲", 5, "5", "80%")), isCompleted = true),
            WorkoutDay(monday.plusWeeks(1), DayMode.TRAINING, listOf(WorkoutExercise(3, "高位下拉", 3, "10", "自定义")), isCompleted = true),
            WorkoutDay(monday.plusWeeks(1).plusDays(2), DayMode.TRAINING, listOf(WorkoutExercise(4, "硬拉", 3, "5", "100kg")))
        )

        val statistics = calculateWorkoutStatistics(completed)

        assertEquals(3, statistics.completedSessions)
        assertEquals(1, statistics.completedCycles)
        assertEquals(5, statistics.regionSets.toMap()[MuscleRegion.QUADS])
        assertEquals(3, statistics.regionSets.toMap()[MuscleRegion.BACK])
        assertEquals(4, statistics.regionSets.toMap()[MuscleRegion.CHEST])
    }


    @Test
    fun `exercise search supports Chinese and pinyin initials`() {
        assertTrue(workoutExerciseMatches("深蹲", "深"))
        assertTrue(workoutExerciseMatches("深蹲", "sd"))
        assertTrue(workoutExerciseMatches("膝上高抓", "xsgz"))
        assertFalse(workoutExerciseMatches("膝上高抓", "wt"))
    }

    @Test
    fun `workout cycle uses custom weekdays and selected deload week`() {
        val monday = LocalDate.of(2026, 8, 24)
        val template = listOf(WorkoutDay(monday, mode = DayMode.TRAINING, exercises = listOf(
            WorkoutExercise(1, "深蹲", 5, "5", "80%")
        )))

        val cycle = buildWorkoutCycle(monday, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), template, cycleWeeks = 6, deloadWeek = 5)

        assertEquals(42, cycle.size)
        assertEquals(12, cycle.count { it.mode == DayMode.TRAINING })
        assertTrue(cycle.slice(28..34).all { it.isDeload })
        assertTrue((cycle.take(28) + cycle.drop(35)).none { it.isDeload })
        assertEquals(3, cycle[28].exercises.single().sets)
    }

    @Test
    fun healthProfileCalculatesMifflinStJeorAndTargets() {
        val profile = HealthProfile(heightCm = 176.0, weightKg = 93.0, age = 25, sex = BiologicalSex.MALE)

        assertEquals(30.0, profile.bmi, 0.1)
        assertEquals(1910.0, profile.basalMetabolicRate, 0.1)
        assertEquals(3078.6, profile.trainingBalanceKcal, 0.1)
        assertEquals(2728.6, profile.restBalanceKcal, 0.1)
        assertEquals(1970.3, profile.trainingIntakeKcal, 0.1)
        assertEquals(1746.3, profile.restIntakeKcal, 0.1)
        assertEquals(232.5, profile.trainingCarbohydratesGrams, 0.1)
        assertEquals(176.7, profile.restCarbohydratesGrams, 0.1)
        assertEquals(130.2, profile.dailyProteinGrams, 0.1)

        val customized = profile.copy(trainingCarbsPerKg = 3.0, restCarbsPerKg = 2.0, proteinPerKg = 1.6)
        assertEquals(279.0, customized.trainingCarbohydratesGrams, 0.1)
        assertEquals(186.0, customized.restCarbohydratesGrams, 0.1)
        assertEquals(148.8, customized.dailyProteinGrams, 0.1)
    }
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun `multiple readings produce daily OHLC candle`() {
        val candle = weightsToCandles(
            listOf(
                WeightEntry(Instant.parse("2026-08-25T00:00:00Z"), 70.2, "test"),
                WeightEntry(Instant.parse("2026-08-25T04:00:00Z"), 70.8, "test"),
                WeightEntry(Instant.parse("2026-08-25T12:00:00Z"), 69.9, "test"),
                WeightEntry(Instant.parse("2026-08-25T14:00:00Z"), 70.5, "test")
            ), zone
        ).single()

        assertEquals(70.2, candle.open, 0.001)
        assertEquals(70.5, candle.close, 0.001)
        assertEquals(70.8, candle.high, 0.001)
        assertEquals(69.9, candle.low, 0.001)
        assertTrue(candle.isUp)
    }

    @Test
    fun `single reading uses previous close as next open`() {
        val candles = weightsToCandles(
            listOf(
                WeightEntry(Instant.parse("2026-08-24T00:00:00Z"), 71.0, "test"),
                WeightEntry(Instant.parse("2026-08-25T00:00:00Z"), 70.4, "test")
            ), zone
        )

        assertEquals(71.0, candles[1].open, 0.001)
        assertEquals(70.4, candles[1].close, 0.001)
        assertFalse(candles[1].isUp)
    }

    @Test
    fun `calorie plan selects target by training schedule`() {
        val plan = CaloriePlan(
            trainingCarbsGrams = 300.0,
            trainingProteinGrams = 180.0,
            trainingFatGrams = 53.333333,
            restCarbsGrams = 237.5,
            restProteinGrams = 142.5,
            restFatGrams = 42.222222,
            trainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        )

        assertEquals(2400.0, plan.targetFor(LocalDate.of(2026, 8, 24)), 0.001)
        assertEquals(DayMode.TRAINING, plan.modeFor(LocalDate.of(2026, 8, 24)))
        assertEquals(1900.0, plan.targetFor(LocalDate.of(2026, 8, 25)), 0.001)
        assertEquals(DayMode.REST, plan.modeFor(LocalDate.of(2026, 8, 25)))
    }

    @Test
    fun `specific workout date overrides recurring diet schedule`() {
        val date = LocalDate.of(2026, 8, 25)
        val plan = CaloriePlan(
            trainingDays = setOf(DayOfWeek.MONDAY),
            dayModeOverrides = mapOf(date to DayMode.TRAINING)
        )

        assertEquals(DayMode.TRAINING, plan.modeFor(date))
        assertTrue(MealPeriod.PRE_WORKOUT in plan.mealPeriodsFor(date))
    }

    @Test
    fun `calories are calculated from macros`() {
        assertEquals(140.04, caloriesFromMacros(31.08, 3.12, 0.36), 0.001)
    }

    @Test
    fun `negative macros never subtract calories`() {
        assertEquals(90.0, caloriesFromMacros(-5.0, 0.0, 10.0), 0.001)
    }

    @Test
    fun `late meals are inferred as night snack`() {
        assertEquals(MealPeriod.NIGHT_SNACK, inferMealPeriod(Instant.parse("2026-08-25T14:00:00Z"), zone))
    }

    @Test
    fun `meal target shares add up to full daily target`() {
        assertEquals(1.0, MealPeriod.entries.sumOf { it.targetShare }, 0.001)
    }

    @Test
    fun `pre workout meal is available only on training days`() {
        val plan = CaloriePlan(trainingDays = setOf(DayOfWeek.MONDAY))
        assertTrue(MealPeriod.PRE_WORKOUT in plan.mealPeriodsFor(LocalDate.of(2026, 8, 24)))
        assertFalse(MealPeriod.PRE_WORKOUT in plan.mealPeriodsFor(LocalDate.of(2026, 8, 25)))
    }

    @Test
    fun `training and rest meal shares each total one`() {
        val training = defaultTrainingMealMacroShares().values
        val rest = defaultMealMacroShares().filterKeys { it != MealPeriod.PRE_WORKOUT }.values
        assertEquals(1.0, training.sumOf { it.carbohydrates }, 0.001)
        assertEquals(1.0, rest.sumOf { it.carbohydrates }, 0.001)
    }

    @Test
    fun `afternoon meal is pre workout only on training day`() {
        val time = Instant.parse("2026-08-24T08:00:00Z")
        assertEquals(MealPeriod.PRE_WORKOUT, inferMealPeriod(time, zone, isTrainingDay = true))
        assertEquals(MealPeriod.LUNCH, inferMealPeriod(time, zone, isTrainingDay = false))
    }

    @Test
    fun `five day moving average starts on fifth candle`() {
        val candles = (1..6).map { day ->
            val value = day.toDouble()
            Candle(LocalDate.of(2026, 8, day), value, value, value, value)
        }
        val averages = fiveDayMovingAverage(candles)

        assertEquals(2, averages.size)
        assertEquals(3.0, averages.getValue(LocalDate.of(2026, 8, 5)), 0.001)
        assertEquals(4.0, averages.getValue(LocalDate.of(2026, 8, 6)), 0.001)
    }

    @Test
    fun `chain restaurant foods are searchable but not quick foods`() {
        val catalog = FoodCatalogRepository()

        assertTrue(catalog.searchLocalFoods("肯德基").size >= 10)
        assertTrue(catalog.searchLocalFoods("KFC").isNotEmpty())
        assertTrue(catalog.searchLocalFoods("麦当劳").size >= 10)
        assertTrue(catalog.quickFoods().none {
            it.name.contains("肯德基") || it.name.contains("KFC") || it.name.contains("麦当劳")
        })
    }
}
