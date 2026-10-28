package com.ranke.health.data

import android.content.Context
import com.ranke.health.model.BiologicalSex
import com.ranke.health.model.CaloriePlan
import com.ranke.health.model.DayMode
import com.ranke.health.model.ExercisePr
import com.ranke.health.model.FoodReference
import com.ranke.health.model.HealthProfile
import com.ranke.health.model.MealEntry
import com.ranke.health.model.MealMacroShare
import com.ranke.health.model.MealPeriod
import com.ranke.health.model.TrainingSettings
import com.ranke.health.model.WeightEntry
import com.ranke.health.model.WorkoutDay
import com.ranke.health.model.WorkoutExercise
import com.ranke.health.model.caloriesFromMacros
import com.ranke.health.model.defaultMealMacroShares
import com.ranke.health.model.defaultTrainingMealMacroShares
import com.ranke.health.model.defaultWorkoutWeek
import com.ranke.health.model.inferMealPeriod
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("ranke_records", Context.MODE_PRIVATE)
    private val db = AppDatabase.get(context)
    private val dao = db.healthDao()

    @Volatile private var migrated = false

    // ===== 首次启动：把旧 SharedPreferences JSON 一次性导入 Room =====
    private fun ensureMigrated() {
        if (migrated) return
        synchronized(this) {
            if (migrated) return
            if (!prefs.getBoolean("migrated_to_room_v1", false)) {
                val ok = runCatching { importLegacyPrefsIntoRoom() }.getOrDefault(false)
                if (ok) prefs.edit().putBoolean("migrated_to_room_v1", true).apply()
            }
            migrated = true
        }
    }

    // 返回是否已就绪（Room 已有数据，或本次成功导入）。全部写入放在单个事务里，
    // 任一环节失败即回滚，Room 保持为空，下次启动会重新尝试，避免半迁移丢数据。
    private fun importLegacyPrefsIntoRoom(): Boolean {
        if (dao.weights().isNotEmpty() || dao.meals().isNotEmpty() ||
            dao.workoutDays().isNotEmpty() || dao.exercisePrs().isNotEmpty()
        ) return true

        val weights = parseWeights(JSONArray(prefs.getString("weights", "[]")))
        val meals = parseMeals(JSONArray(prefs.getString("meals", "[]")))
        val days = parseWorkoutDays(JSONArray(prefs.getString("workout_days", "[]")))
        val prs = parseExercisePrs(JSONArray(prefs.getString("exercise_prs", "[]")))

        if (weights.isEmpty() && meals.isEmpty() && days.isEmpty() && prs.isEmpty()) return true

        db.runInTransaction(Runnable {
            if (weights.isNotEmpty()) dao.insertWeights(weights.map { it.toEntity() })
            if (meals.isNotEmpty()) dao.insertMeals(meals.map { it.toEntity() })
            if (days.isNotEmpty()) {
                dao.clearWorkoutDays()
                dao.clearWorkoutExercises()
                dao.insertWorkoutDays(days.map { it.toDayEntity() })
                dao.insertWorkoutExercises(days.flatMap { day ->
                    day.exercises.mapIndexed { index, exercise -> exercise.toEntity(day.date.toString(), index) }
                })
            }
            if (prs.isNotEmpty()) dao.insertExercisePrs(prs.map { it.toEntity() })
        })
        return true
    }

    // ===== 设置类（继续留在 SharedPreferences）=====
    fun savedBackupUri(): String? = prefs.getString("saved_backup_uri", null)

    fun saveBackupUri(uri: String) {
        prefs.edit().putString("saved_backup_uri", uri).apply()
    }

    fun clearBackupUri() {
        prefs.edit().remove("saved_backup_uri").apply()
    }

    // ===== 体重（Room）=====
    fun weights(): List<WeightEntry> {
        ensureMigrated()
        return dao.weights().map { it.toModel() }
    }

    fun addWeight(kg: Double) {
        ensureMigrated()
        dao.insertWeight(WeightEntity(time = Instant.now().toString(), kilograms = kg, source = "man牛"))
    }

    // ===== 饮食（Room）=====
    fun meals(): List<MealEntry> {
        ensureMigrated()
        return dao.meals().map { it.toModel() }
    }

    fun addMeal(name: String, kcal: Double, period: MealPeriod, carbs: Double, protein: Double, fat: Double) {
        ensureMigrated()
        dao.insertMeal(MealEntity(
            time = Instant.now().toString(),
            name = name,
            kilocalories = kcal,
            source = "man牛",
            period = period.name,
            carbohydratesGrams = carbs,
            proteinGrams = protein,
            fatGrams = fat
        ))
    }

    fun updateMeal(original: MealEntry, name: String, kcal: Double, period: MealPeriod, carbs: Double, protein: Double, fat: Double) {
        ensureMigrated()
        dao.updateMeal(
            time = original.time.toString(),
            name = name,
            kilocalories = kcal,
            period = period.name,
            carbs = carbs,
            protein = protein,
            fat = fat
        )
    }

    // ===== 训练日/动作（Room）=====
    fun workoutDays(): List<WorkoutDay> {
        ensureMigrated()
        val days = dao.workoutDays()
        if (days.isEmpty()) return defaultWorkoutWeek()
        val exercisesByDay = dao.workoutExercises().groupBy { it.dayDate }
        return days.map { day -> day.toModel(exercisesByDay[day.date].orEmpty().map { it.toModel() }) }
    }

    fun saveWorkoutDays(days: List<WorkoutDay>) {
        ensureMigrated()
        db.runInTransaction(Runnable {
            replaceWorkoutDays(days)
        })
    }

    // ===== PR（Room）=====
    fun exercisePrs(): List<ExercisePr> {
        ensureMigrated()
        return dao.exercisePrs().map { it.toModel() }
    }

    fun addExercisePr(pr: ExercisePr) {
        ensureMigrated()
        dao.insertExercisePr(pr.toEntity())
    }

    // ===== 自定义食物预设（SharedPreferences）=====
    fun customFoods(): List<FoodReference> = runCatching {
        val json = JSONArray(prefs.getString("food_presets", "[]"))
        buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                val carbs = item.optDouble("carbs", 0.0)
                val protein = item.optDouble("protein", 0.0)
                val fat = item.optDouble("fat", 0.0)
                add(FoodReference(
                    name = item.getString("name"),
                    kilocaloriesPer100g = caloriesFromMacros(carbs, protein, fat),
                    carbohydratesPer100g = carbs,
                    proteinPer100g = protein,
                    fatPer100g = fat,
                    source = "自定义预设"
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun saveFoodPreset(food: FoodReference) {
        val updated = (customFoods().filterNot { it.name.equals(food.name, ignoreCase = true) } + food)
            .sortedBy { it.name }
        prefs.edit().putString("food_presets", JSONArray().apply {
            updated.forEach {
                put(JSONObject()
                    .put("name", it.name)
                    .put("carbs", it.carbohydratesPer100g)
                    .put("protein", it.proteinPer100g)
                    .put("fat", it.fatPer100g))
            }
        }.toString()).apply()
    }

    // ===== 首页偏好（SharedPreferences）=====
    fun hiddenQuickFoodNames(): Set<String> = runCatching {
        val json = JSONArray(prefs.getString("hidden_quick_foods", "[]"))
        buildSet {
            for (index in 0 until json.length()) add(json.getString(index).lowercase())
        }
    }.getOrDefault(emptySet())

    fun hideQuickFood(name: String) {
        val updated = hiddenQuickFoodNames() + name.lowercase()
        prefs.edit().putString("hidden_quick_foods", JSONArray(updated.toList()).toString()).apply()
    }

    fun quickFoodOrder(): List<String> = runCatching {
        val json = JSONArray(prefs.getString("quick_food_order", "[]"))
        buildList { for (index in 0 until json.length()) add(json.getString(index).lowercase()) }
    }.getOrDefault(emptyList())

    fun saveQuickFoodOrder(names: List<String>) {
        val normalized = names.map { it.lowercase() }.distinct()
        prefs.edit().putString("quick_food_order", JSONArray(normalized).toString()).apply()
    }

    fun dashboardSectionOrder(): List<String> = runCatching {
        val json = JSONArray(prefs.getString("dashboard_section_order", "[]"))
        buildList { for (index in 0 until json.length()) add(json.getString(index)) }
    }.getOrDefault(emptyList())

    fun saveDashboardSectionOrder(names: List<String>) {
        prefs.edit().putString("dashboard_section_order", JSONArray(names.distinct()).toString()).apply()
    }

    // ===== 训练设置（SharedPreferences）=====
    fun trainingSettings(): TrainingSettings = runCatching {
        val json = JSONObject(prefs.getString("training_settings", "{}") ?: "{}")
        val cycleWeeks = json.optInt("cycleWeeks", 4).coerceIn(1, 12)
        TrainingSettings(
            cycleWeeks = cycleWeeks,
            deloadWeek = json.optInt("deloadWeek", cycleWeeks).coerceIn(1, cycleWeeks)
        )
    }.getOrDefault(TrainingSettings())

    fun saveTrainingSettings(settings: TrainingSettings) {
        prefs.edit().putString("training_settings", JSONObject()
            .put("cycleWeeks", settings.cycleWeeks)
            .put("deloadWeek", settings.deloadWeek)
            .toString()).apply()
    }

    // ===== 身体档案（SharedPreferences）=====
    fun healthProfile(): HealthProfile = runCatching {
        val json = JSONObject(prefs.getString("health_profile", "{}") ?: "{}")
        HealthProfile(
            heightCm = json.optDouble("heightCm", 176.0),
            weightKg = json.optDouble("weightKg", 93.0),
            age = json.optInt("age", 25),
            sex = runCatching { BiologicalSex.valueOf(json.optString("sex", BiologicalSex.MALE.name)) }.getOrDefault(BiologicalSex.MALE),
            strengthTrainingKcal = json.optDouble("strengthTrainingKcal", 350.0),
            aerobicKcal = json.optDouble("aerobicKcal", 0.0),
            intakeFactor = json.optDouble("intakeFactor", .64),
            trainingCarbsPerKg = json.optDouble("trainingCarbsPerKg", 2.5),
            restCarbsPerKg = json.optDouble("restCarbsPerKg", 1.9),
            proteinPerKg = json.optDouble("proteinPerKg", 1.4)
        )
    }.getOrDefault(HealthProfile())

    fun saveHealthProfile(profile: HealthProfile) {
        prefs.edit().putString("health_profile", JSONObject()
            .put("heightCm", profile.heightCm)
            .put("weightKg", profile.weightKg)
            .put("age", profile.age)
            .put("sex", profile.sex.name)
            .put("strengthTrainingKcal", profile.strengthTrainingKcal)
            .put("aerobicKcal", profile.aerobicKcal)
            .put("intakeFactor", profile.intakeFactor)
            .put("trainingCarbsPerKg", profile.trainingCarbsPerKg)
            .put("restCarbsPerKg", profile.restCarbsPerKg)
            .put("proteinPerKg", profile.proteinPerKg)
            .toString()).apply()
    }

    // ===== 热量计划（SharedPreferences）=====
    fun caloriePlan(): CaloriePlan = runCatching {
        val json = JSONObject(prefs.getString("calorie_plan", "{}") ?: "{}")
        val days = json.optJSONArray("trainingDays")
        val trainingDays = buildSet {
            if (days != null) for (index in 0 until days.length()) {
                runCatching { DayOfWeek.valueOf(days.getString(index)) }.getOrNull()?.let(::add)
            }
        }
        val resolvedDays = if (days == null) CaloriePlan().trainingDays else trainingDays
        fun readShares(key: String, defaults: Map<MealPeriod, MealMacroShare>): Map<MealPeriod, MealMacroShare> {
            val savedShares = json.optJSONObject(key)
            return MealPeriod.entries.associateWith { period ->
                val item = savedShares?.optJSONObject(period.name)
                val fallback = defaults.getValue(period)
                MealMacroShare(
                    item?.optDouble("carbs", fallback.carbohydrates) ?: fallback.carbohydrates,
                    item?.optDouble("protein", fallback.protein) ?: fallback.protein,
                    item?.optDouble("fat", fallback.fat) ?: fallback.fat
                )
            }
        }
        val mealShares = readShares("mealMacroShares", defaultMealMacroShares())
        val trainingMealShares = readShares("trainingMealMacroShares", defaultTrainingMealMacroShares())
        if (json.has("trainingCarbs") && json.has("restCarbs")) {
            CaloriePlan(
                trainingCarbsGrams = json.optDouble("trainingCarbs", 275.0),
                trainingProteinGrams = json.optDouble("trainingProtein", 165.0),
                trainingFatGrams = json.optDouble("trainingFat", 220.0 / 4.5),
                restCarbsGrams = json.optDouble("restCarbs", 225.0),
                restProteinGrams = json.optDouble("restProtein", 135.0),
                restFatGrams = json.optDouble("restFat", 40.0),
                trainingDays = resolvedDays,
                mealMacroShares = mealShares,
                trainingMealMacroShares = trainingMealShares
            )
        } else {
            CaloriePlan.fromLegacyCalories(
                json.optDouble("trainingTarget", 2200.0),
                json.optDouble("restTarget", 1800.0),
                resolvedDays
            )
        }
    }.getOrDefault(CaloriePlan())

    fun saveCaloriePlan(plan: CaloriePlan) {
        val json = JSONObject()
            .put("trainingTarget", plan.trainingTargetKcal)
            .put("restTarget", plan.restTargetKcal)
            .put("trainingCarbs", plan.trainingCarbsGrams)
            .put("trainingProtein", plan.trainingProteinGrams)
            .put("trainingFat", plan.trainingFatGrams)
            .put("restCarbs", plan.restCarbsGrams)
            .put("restProtein", plan.restProteinGrams)
            .put("restFat", plan.restFatGrams)
            .put("mealMacroShares", JSONObject().apply {
                MealPeriod.entries.forEach { period ->
                    val share = plan.mealMacroShares[period] ?: defaultMealMacroShares().getValue(period)
                    put(period.name, JSONObject()
                        .put("carbs", share.carbohydrates)
                        .put("protein", share.protein)
                        .put("fat", share.fat))
                }
            })
            .put("trainingMealMacroShares", JSONObject().apply {
                MealPeriod.entries.forEach { period ->
                    val share = plan.trainingMealMacroShares[period] ?: defaultTrainingMealMacroShares().getValue(period)
                    put(period.name, JSONObject()
                        .put("carbs", share.carbohydrates)
                        .put("protein", share.protein)
                        .put("fat", share.fat))
                }
            })
            .put("trainingDays", JSONArray().apply { plan.trainingDays.sortedBy { it.value }.forEach { put(it.name) } })
        prefs.edit().putString("calorie_plan", json.toString()).apply()
    }

    // ===== 备份导出：结构不变，体重/饮食/训练/PR 改从 Room 读取 =====
    fun exportBackupJson(): String {
        ensureMigrated()
        return JSONObject()
            .put("schemaVersion", 1)
            .put("exportedAt", Instant.now().toString())
            .put("weights", JSONArray().apply {
                weights().forEach {
                    put(JSONObject().put("time", it.time.toString()).put("kg", it.kilograms).put("source", it.source))
                }
            })
            .put("meals", JSONArray().apply {
                meals().forEach {
                    put(JSONObject()
                        .put("time", it.time.toString())
                        .put("name", it.name)
                        .put("kcal", it.kilocalories)
                        .put("source", it.source)
                        .put("period", it.period.name)
                        .put("carbs", it.carbohydratesGrams)
                        .put("protein", it.proteinGrams)
                        .put("fat", it.fatGrams))
                }
            })
            .put("foodPresets", JSONArray(prefs.getString("food_presets", "[]")))
            .put("hiddenQuickFoods", JSONArray(prefs.getString("hidden_quick_foods", "[]")))
            .put("quickFoodOrder", JSONArray(prefs.getString("quick_food_order", "[]")))
            .put("dashboardSectionOrder", JSONArray(prefs.getString("dashboard_section_order", "[]")))
            .put("trainingSettings", JSONObject(prefs.getString("training_settings", "{}") ?: "{}"))
            .put("caloriePlan", JSONObject(prefs.getString("calorie_plan", "{}") ?: "{}"))
            .put("healthProfile", JSONObject(prefs.getString("health_profile", "{}") ?: "{}"))
            .put("workoutDays", JSONArray().apply {
                workoutDays().sortedBy { it.date }.forEach { day ->
                    put(JSONObject()
                        .put("date", day.date.toString())
                        .put("mode", day.mode.name)
                        .apply { if (day.cycleWeek != null) put("cycleWeek", day.cycleWeek) }
                        .put("isDeload", day.isDeload)
                        .put("isCompleted", day.isCompleted)
                        .put("exercises", JSONArray().apply {
                            day.exercises.forEach { exercise ->
                                put(JSONObject()
                                    .put("id", exercise.id)
                                    .put("name", exercise.name)
                                    .put("sets", exercise.sets)
                                    .put("reps", exercise.reps)
                                    .put("weight", exercise.weight))
                            }
                        }))
                }
            })
            .put("exercisePrs", JSONArray().apply {
                exercisePrs().forEach { pr ->
                    put(JSONObject()
                        .put("id", pr.id)
                        .put("exerciseName", pr.exerciseName)
                        .put("weightKg", pr.weightKg)
                        .put("reps", pr.reps)
                        .put("date", pr.date.toString()))
                }
            })
            .toString()
    }

    // ===== 备份恢复：合并语义不变，体重/饮食/训练/PR 写入 Room =====
    fun importBackupJson(content: String): Boolean = runCatching {
        ensureMigrated()
        val root = JSONObject(content)
        require(root.optInt("schemaVersion", 0) == 1) { "Unsupported backup version" }

        // 先完整解析和校验，再执行任何写入。这样损坏文件不会留下“恢复失败但
        // 部分数据已经改变”的状态，也不会把解析失败的训练计划当成空计划。
        val incomingWeights = root.optJSONArray("weights")?.let { incoming ->
            buildList {
                for (i in 0 until incoming.length()) {
                    val item = incoming.getJSONObject(i)
                    add(WeightEntry(
                        time = Instant.parse(item.getString("time")),
                        kilograms = item.getDouble("kg").also { require(it in 20.0..400.0) },
                        source = item.optString("source", "man牛")
                    ))
                }
            }
        }.orEmpty()
        val incomingMeals = root.optJSONArray("meals")?.let { incoming ->
            buildList {
                for (i in 0 until incoming.length()) {
                    val item = incoming.getJSONObject(i)
                    val time = Instant.parse(item.getString("time"))
                    add(MealEntry(
                        time = time,
                        name = item.getString("name").trim().also { require(it.isNotEmpty()) },
                        kilocalories = item.getDouble("kcal").also { require(it >= 0.0) },
                        source = item.optString("source", "man牛"),
                        period = runCatching { MealPeriod.valueOf(item.optString("period")) }
                            .getOrElse { inferMealPeriod(time) },
                        carbohydratesGrams = item.optDouble("carbs", 0.0).also { require(it >= 0.0) },
                        proteinGrams = item.optDouble("protein", 0.0).also { require(it >= 0.0) },
                        fatGrams = item.optDouble("fat", 0.0).also { require(it >= 0.0) }
                    ))
                }
            }
        }.orEmpty()
        val incomingWorkoutDays = root.optJSONArray("workoutDays")?.let(::parseWorkoutDaysStrict)
        val incomingPrs = root.optJSONArray("exercisePrs")?.let(::parseExercisePrsStrict).orEmpty()

        // 在解析阶段同时验证设置 JSON，写入时只使用已经验证过的字符串。
        val incomingFoodPresets = root.optJSONArray("foodPresets") ?: JSONArray()
        val incomingHiddenFoods = root.optJSONArray("hiddenQuickFoods")
        val incomingQuickFoodOrder = root.optJSONArray("quickFoodOrder")
        val incomingDashboardOrder = root.optJSONArray("dashboardSectionOrder")
        val incomingTrainingSettings = root.optJSONObject("trainingSettings")
        val incomingCaloriePlan = root.optJSONObject("caloriePlan")
        val incomingHealthProfile = root.optJSONObject("healthProfile")

        // 体重：按 time|kg 合并
        val existingWeightKeys = weights().map { "${it.time}|${it.kilograms}" }.toMutableSet()
        val newWeights = incomingWeights.filter { existingWeightKeys.add("${it.time}|${it.kilograms}") }

        // 饮食：按 time|name|kcal 合并
        val existingMealKeys = meals().map { "${it.time}|${it.name}|${it.kilocalories}" }.toMutableSet()
        val newMeals = incomingMeals.filter { existingMealKeys.add("${it.time}|${it.name}|${it.kilocalories}") }

        // PR：按 id 合并
        val existingPrIds = exercisePrs().map { it.id }.toMutableSet()
        val newPrs = incomingPrs.filter { existingPrIds.add(it.id) }

        // 设置类：沿用 SharedPreferences 合并
        val editor = prefs.edit().putString(
            "food_presets",
            mergeArrays(
                JSONArray(prefs.getString("food_presets", "[]")),
                incomingFoodPresets
            ) { item -> item.optString("name").lowercase() }.toString()
        )
        incomingHiddenFoods?.let { incoming ->
            val hidden = hiddenQuickFoodNames().toMutableSet()
            for (index in 0 until incoming.length()) hidden += incoming.optString(index).lowercase()
            editor.putString("hidden_quick_foods", JSONArray(hidden.toList()).toString())
        }
        incomingQuickFoodOrder?.let { editor.putString("quick_food_order", it.toString()) }
        incomingDashboardOrder?.let { editor.putString("dashboard_section_order", it.toString()) }
        incomingTrainingSettings?.let { editor.putString("training_settings", it.toString()) }
        incomingCaloriePlan?.let { editor.putString("calorie_plan", it.toString()) }
        incomingHealthProfile?.let { editor.putString("health_profile", it.toString()) }

        // 所有 Room 写入共用一个事务。训练日仅在备份确实包含该字段时替换；
        // 合法的空数组仍可用于清空，解析失败则在到达这里前已经终止。
        db.runInTransaction(Runnable {
            if (newWeights.isNotEmpty()) dao.insertWeights(newWeights.map { it.toEntity() })
            if (newMeals.isNotEmpty()) dao.insertMeals(newMeals.map { it.toEntity() })
            incomingWorkoutDays?.let(::replaceWorkoutDays)
            if (newPrs.isNotEmpty()) dao.insertExercisePrs(newPrs.map { it.toEntity() })
            check(editor.commit()) { "Unable to persist restored settings" }
        })
        true
    }.getOrDefault(false)

    private fun replaceWorkoutDays(days: List<WorkoutDay>) {
        val ordered = days.sortedBy { it.date }
        dao.clearWorkoutDays()
        dao.clearWorkoutExercises()
        dao.insertWorkoutDays(ordered.map { it.toDayEntity() })
        dao.insertWorkoutExercises(ordered.flatMap { day ->
            day.exercises.mapIndexed { index, exercise -> exercise.toEntity(day.date.toString(), index) }
        })
    }

    private fun mergeArrays(current: JSONArray, incoming: JSONArray, key: (JSONObject) -> String): JSONArray {
        val merged = LinkedHashMap<String, JSONObject>()
        for (index in 0 until current.length()) current.optJSONObject(index)?.let { merged[key(it)] = it }
        for (index in 0 until incoming.length()) incoming.optJSONObject(index)?.let { merged[key(it)] = it }
        return JSONArray().apply { merged.values.forEach(::put) }
    }
}

// ===== 旧 JSON 解析（供首次迁移与恢复使用，返回原始列表，空则空列表）=====
private fun parseWeights(json: JSONArray): List<WeightEntry> = runCatching {
    buildList {
        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)
            add(WeightEntry(Instant.parse(item.getString("time")), item.getDouble("kg"), item.optString("source", "man牛")))
        }
    }
}.getOrDefault(emptyList())

private fun parseMeals(json: JSONArray): List<MealEntry> = runCatching {
    buildList {
        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)
            val time = Instant.parse(item.getString("time"))
            val period = runCatching { MealPeriod.valueOf(item.optString("period")) }.getOrElse { inferMealPeriod(time) }
            add(MealEntry(
                time = time,
                name = item.getString("name"),
                kilocalories = item.getDouble("kcal"),
                source = item.optString("source", "man牛"),
                period = period,
                carbohydratesGrams = item.optDouble("carbs", 0.0),
                proteinGrams = item.optDouble("protein", 0.0),
                fatGrams = item.optDouble("fat", 0.0)
            ))
        }
    }
}.getOrDefault(emptyList())

private fun parseWorkoutDays(json: JSONArray): List<WorkoutDay> = runCatching {
    parseWorkoutDaysStrict(json)
}.getOrDefault(emptyList())

private fun parseWorkoutDaysStrict(json: JSONArray): List<WorkoutDay> =
    buildList {
        for (index in 0 until json.length()) {
            val item = json.getJSONObject(index)
            val exercisesJson = item.optJSONArray("exercises") ?: JSONArray()
            val exercises = buildList {
                for (exerciseIndex in 0 until exercisesJson.length()) {
                    val exercise = exercisesJson.getJSONObject(exerciseIndex)
                    add(WorkoutExercise(
                        id = exercise.optLong("id", System.nanoTime() + exerciseIndex),
                        name = exercise.getString("name").trim().also { require(it.isNotEmpty()) },
                        sets = exercise.optInt("sets", 1).also { require(it in 1..100) },
                        reps = exercise.optString("reps", "1"),
                        weight = exercise.optString("weight", "自定义")
                    ))
                }
            }
            add(WorkoutDay(
                date = LocalDate.parse(item.getString("date")),
                mode = runCatching { DayMode.valueOf(item.optString("mode")) }.getOrDefault(DayMode.REST),
                exercises = exercises,
                cycleWeek = item.optInt("cycleWeek", 0).takeIf { it in 1..12 },
                isDeload = item.optBoolean("isDeload", false),
                isCompleted = item.optBoolean("isCompleted", false)
            ))
        }
    }

private fun parseExercisePrs(json: JSONArray): List<ExercisePr> = runCatching {
    parseExercisePrsStrict(json)
}.getOrDefault(emptyList())

private fun parseExercisePrsStrict(json: JSONArray): List<ExercisePr> =
    buildList {
        for (index in 0 until json.length()) {
            val item = json.getJSONObject(index)
            add(ExercisePr(
                id = item.optLong("id", index.toLong()),
                exerciseName = item.getString("exerciseName").trim().also { require(it.isNotEmpty()) },
                weightKg = item.getDouble("weightKg").also { require(it > 0.0) },
                reps = item.optInt("reps", 1).coerceAtLeast(1),
                date = LocalDate.parse(item.getString("date"))
            ))
        }
    }

// ===== 模型 <-> 实体映射 =====
private fun WeightEntry.toEntity() = WeightEntity(time = time.toString(), kilograms = kilograms, source = source)
private fun WeightEntity.toModel() = WeightEntry(Instant.parse(time), kilograms, source)

private fun MealEntry.toEntity() = MealEntity(
    time = time.toString(),
    name = name,
    kilocalories = kilocalories,
    source = source,
    period = period.name,
    carbohydratesGrams = carbohydratesGrams,
    proteinGrams = proteinGrams,
    fatGrams = fatGrams
)
private fun MealEntity.toModel() = MealEntry(
    time = Instant.parse(time),
    name = name,
    kilocalories = kilocalories,
    source = source,
    period = runCatching { MealPeriod.valueOf(period) }.getOrElse { inferMealPeriod(Instant.parse(time)) },
    carbohydratesGrams = carbohydratesGrams,
    proteinGrams = proteinGrams,
    fatGrams = fatGrams
)

private fun ExercisePr.toEntity() = ExercisePrEntity(id = id, exerciseName = exerciseName, weightKg = weightKg, reps = reps, date = date.toString())
private fun ExercisePrEntity.toModel() = ExercisePr(id = id, exerciseName = exerciseName, weightKg = weightKg, reps = reps, date = LocalDate.parse(date))

private fun WorkoutDay.toDayEntity() = WorkoutDayEntity(date = date.toString(), mode = mode.name, cycleWeek = cycleWeek, isDeload = isDeload, isCompleted = isCompleted)
private fun WorkoutDayEntity.toModel(exercises: List<WorkoutExercise>) = WorkoutDay(
    date = LocalDate.parse(date),
    mode = runCatching { DayMode.valueOf(mode) }.getOrDefault(DayMode.REST),
    exercises = exercises,
    cycleWeek = cycleWeek,
    isDeload = isDeload,
    isCompleted = isCompleted
)
private fun WorkoutExercise.toEntity(dayDate: String, position: Int) = WorkoutExerciseEntity(
    dayDate = dayDate,
    position = position,
    exerciseId = id,
    name = name,
    sets = sets,
    reps = reps,
    weight = weight
)
private fun WorkoutExerciseEntity.toModel() = WorkoutExercise(id = exerciseId, name = name, sets = sets, reps = reps, weight = weight)
