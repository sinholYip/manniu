package com.ranke.health.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WeightEntry(val time: Instant, val kilograms: Double, val source: String)

// 枚举不持有显示文案：中文名仍是动作/食物等数据的规范键，但界面文案统一由
// ui/i18n 层按当前语言翻译，避免模型层依赖 Android 资源。
enum class MealPeriod(val targetShare: Double) {
    BREAKFAST(.20),
    LUNCH(.30),
    PRE_WORKOUT(.0),
    DINNER(.35),
    NIGHT_SNACK(.15)
}

fun inferMealPeriod(
    time: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    isTrainingDay: Boolean = false
): MealPeriod = when (time.atZone(zone).hour) {
        in 4..10 -> MealPeriod.BREAKFAST
        in 11..14 -> MealPeriod.LUNCH
        in 15..17 -> if (isTrainingDay) MealPeriod.PRE_WORKOUT else MealPeriod.LUNCH
        in 18..20 -> MealPeriod.DINNER
        else -> MealPeriod.NIGHT_SNACK
    }

data class MealEntry(
    val time: Instant,
    val name: String,
    val kilocalories: Double,
    val source: String,
    val period: MealPeriod,
    val carbohydratesGrams: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val fatGrams: Double = 0.0
)

data class FoodReference(
    val name: String,
    val kilocaloriesPer100g: Double,
    val carbohydratesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val source: String
)

fun caloriesFromMacros(carbohydratesGrams: Double, proteinGrams: Double, fatGrams: Double): Double =
    carbohydratesGrams.coerceAtLeast(0.0) * 4.0 +
        proteinGrams.coerceAtLeast(0.0) * 4.0 +
        fatGrams.coerceAtLeast(0.0) * 9.0

data class Candle(
    val date: LocalDate,
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double
) {
    val isUp: Boolean get() = close >= open
}

fun weightsToCandles(entries: List<WeightEntry>, zone: ZoneId = ZoneId.systemDefault()): List<Candle> {
    val days = entries.sortedBy { it.time }.groupBy { it.time.atZone(zone).toLocalDate() }.toSortedMap()
    var previousClose: Double? = null
    return days.map { (date, values) ->
        val readings = values.map { it.kilograms }
        val open = if (readings.size > 1) readings.first() else previousClose ?: readings.first()
        val close = readings.last()
        Candle(date, open, close, maxOf(readings.max(), open, close), minOf(readings.min(), open, close))
            .also { previousClose = close }
    }
}

fun fiveDayMovingAverage(candles: List<Candle>): Map<LocalDate, Double> = buildMap {
    candles.forEachIndexed { index, candle ->
        if (index >= 4) put(candle.date, candles.subList(index - 4, index + 1).map { it.close }.average())
    }
}

data class DashboardState(
    val loading: Boolean = false,
    val weights: List<WeightEntry> = emptyList(),
    val meals: List<MealEntry> = emptyList(),
    val customFoods: List<FoodReference> = emptyList(),
    val hiddenQuickFoodNames: Set<String> = emptySet(),
    val quickFoodOrder: List<String> = emptyList(),
    val dashboardSectionOrder: List<String> = emptyList(),
    val caloriePlan: CaloriePlan = CaloriePlan(),
    val healthProfile: HealthProfile = HealthProfile(),
    val workoutDays: List<WorkoutDay> = defaultWorkoutWeek(),
    val exercisePrs: List<ExercisePr> = emptyList(),
    val trainingSettings: TrainingSettings = TrainingSettings(),
    val message: String? = null
)

data class TrainingSettings(
    val cycleWeeks: Int = 4,
    val deloadWeek: Int = 4
)

data class ExercisePr(
    val id: Long,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val date: LocalDate
) {
    val estimatedOneRepMax: Double get() = weightKg * (1.0 + reps / 30.0)
}

enum class BiologicalSex { MALE, FEMALE }

enum class BmiStatus { UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE }

data class HealthProfile(
    val heightCm: Double = 176.0,
    val weightKg: Double = 93.0,
    val age: Int = 25,
    val sex: BiologicalSex = BiologicalSex.MALE,
    val strengthTrainingKcal: Double = 350.0,
    val aerobicKcal: Double = 0.0,
    val intakeFactor: Double = .64,
    val trainingCarbsPerKg: Double = 2.5,
    val restCarbsPerKg: Double = 1.9,
    val proteinPerKg: Double = 1.4
) {
    val bmi: Double get() = weightKg / ((heightCm / 100.0) * (heightCm / 100.0))
    val bmiStatus: BmiStatus get() = when {
        bmi < 18.5 -> BmiStatus.UNDERWEIGHT
        bmi < 24.0 -> BmiStatus.NORMAL
        bmi < 28.0 -> BmiStatus.OVERWEIGHT
        else -> BmiStatus.OBESE
    }
    val basalMetabolicRate: Double get() =
        10.0 * weightKg + 6.25 * heightCm - 5.0 * age + if (sex == BiologicalSex.MALE) 5.0 else -161.0
    val sedentaryExpenditure: Double get() = basalMetabolicRate / .7
    val trainingBalanceKcal: Double get() = sedentaryExpenditure + strengthTrainingKcal + aerobicKcal
    val restBalanceKcal: Double get() = sedentaryExpenditure + aerobicKcal
    val trainingIntakeKcal: Double get() = trainingBalanceKcal * intakeFactor
    val restIntakeKcal: Double get() = restBalanceKcal * intakeFactor
    val trainingCarbohydratesGrams: Double get() = weightKg * trainingCarbsPerKg
    val restCarbohydratesGrams: Double get() = weightKg * restCarbsPerKg
    val dailyProteinGrams: Double get() = weightKg * proteinPerKg
}

enum class DayMode { TRAINING, REST }

data class WorkoutExercise(
    val id: Long,
    val name: String,
    val sets: Int,
    val reps: String,
    val weight: String
)

data class WorkoutDay(
    val date: LocalDate,
    val mode: DayMode = DayMode.REST,
    val exercises: List<WorkoutExercise> = emptyList(),
    val cycleWeek: Int? = null,
    val isDeload: Boolean = false,
    val isCompleted: Boolean = false
)

val recommendedWorkoutExerciseNames = listOf(
    "深蹲", "硬拉", "卧推", "推举", "引体向上", "杠铃划船", "臀推", "高位下拉", "抓举"
)

val commonWorkoutExerciseNames = listOf(
    // 举重及其变式
    "抓举", "高抓", "膝上抓", "悬垂抓举", "半抓",
    "挺举", "高翻", "下蹲翻", "翻铃", "膝上高翻", "悬垂高翻", "高翻挺",
    "半挺", "箭步挺", "分腿挺", "下蹲挺", "挺支", "抓支",
    "借力推", "颈后借力推", "实力推", "高拉", "宽速拉", "窄速拉",
    "抓举硬拉", "挺举硬拉", "宽硬拉", "窄硬拉",
    "过顶深蹲", "前蹲", "后蹲", "泽奇蹲",
    "架上挺", "架上借力推", "架上高翻", "架上抓举",
    // 胸部
    "卧推", "平板卧推", "上斜卧推", "下斜卧推", "窄握卧推", "地板卧推", "史密斯卧推",
    "哑铃卧推", "上斜哑铃卧推", "下斜哑铃卧推",
    "哑铃飞鸟", "上斜哑铃飞鸟", "绳索夹胸", "蝴蝶机夹胸",
    "俯卧撑", "上斜俯卧撑", "下斜俯卧撑", "双杠臂屈伸",
    // 背部
    "引体向上", "宽握引体向上", "反手引体向上", "对握引体向上",
    "高位下拉", "反手高位下拉", "颈后高位下拉",
    "杠铃划船", "反手杠铃划船", "哑铃划船", "单臂哑铃划船", "器械划船", "坐姿绳索划船",
    "T杠划船", "潘德雷划船",
    "硬拉", "直腿硬拉", "罗马尼亚硬拉", "相扑硬拉", "小相扑硬拉",
    "山羊挺身", "早安式体前屈", "面拉", "杠铃耸肩", "哑铃耸肩", "农夫行走",
    // 肩部
    "推举", "坐姿哑铃推举", "阿诺德推举", "颈后推举", "器械推肩",
    "侧平举", "前平举", "俯身侧平举", "绳索侧平举",
    "俯身飞鸟", "反向飞鸟", "直立划船", "古巴推举",
    // 手臂
    "二头弯举", "杠铃弯举", "哑铃弯举", "锤式弯举", "牧师凳弯举", "集中弯举", "绳索弯举",
    "绳索下压", "直杆下压", "单臂绳索下压", "仰卧臂屈伸", "颈后臂屈伸", "过顶臂屈伸",
    // 腿部
    "深蹲", "高脚杯深蹲", "哈克深蹲", "箱式深蹲", "暂停深蹲", "史密斯深蹲",
    "腿举", "腿屈伸", "腿弯举", "坐姿腿弯举", "俯卧腿弯举",
    "弓步蹲", "箭步蹲", "行走箭步蹲", "侧弓步", "保加利亚分腿蹲", "泽奇后弓步蹲", "台阶蹬踏",
    "臀推", "臀桥", "站姿提踵", "坐姿提踵",
    // 核心
    "卷腹", "反向卷腹", "仰卧起坐", "负重仰卧起坐", "悬垂举腿",
    "平板支撑", "侧桥", "哥本哈根侧支撑", "俄罗斯转体", "登山跑", "死虫式", "鸟狗式", "健腹轮", "V字两头起",
    // 功能性
    "壶铃摇摆", "土耳其起立", "实心球", "药球下砸", "战绳", "波比跳", "跳箱", "雪橇推", "负重行走"
)

val spreadsheetWorkoutExerciseNames = listOf(
    "保加利亚单腿蹲", "单哑铃划船", "反握高位下拉", "反握下拉", "高翻借力推",
    "架上颈后挺", "架上暂停挺", "宽距后蹲", "膝上高拉+高抓", "膝上高抓", "哑铃单腿硬拉"
)

private val workoutExerciseInitials = mapOf(
    "深蹲" to "sd", "前蹲" to "qd", "卧推" to "wt", "上斜卧推" to "sxwt", "硬拉" to "yl",
    "罗马尼亚硬拉" to "lmnyyl", "挺举" to "tj", "引体向上" to "ytxs", "高位下拉" to "gwxl",
    "杠铃划船" to "glhc", "器械划船" to "qxhc", "哑铃划船" to "ylhc", "双杠臂屈伸" to "sgbqs",
    "俯卧撑" to "fwc", "弓步蹲" to "gbd", "保加利亚分腿蹲" to "bjlyftd", "腿举" to "tj",
    "腿弯举" to "twj", "臀推" to "tt", "二头弯举" to "etwj", "绳索下压" to "ssxy",
    "侧平举" to "cpj", "面拉" to "ml", "卷腹" to "jf", "平板支撑" to "pbzc", "侧桥" to "cq",
    "抓举" to "zj", "高翻" to "gf", "实心球" to "sxq", "保加利亚单腿蹲" to "bjlydtd",
    "单哑铃划船" to "dylhc", "反握高位下拉" to "fwgwxl", "反握下拉" to "fwxl",
    "高翻借力推" to "gfjlt", "高翻挺" to "gft", "高拉" to "gl", "高抓" to "gz",
    "哥本哈根侧支撑" to "gbhgczc", "后蹲" to "hd", "壶铃摇摆" to "hlyb", "架上借力推" to "jsjlt",
    "架上颈后挺" to "jsjht", "架上挺" to "jst", "架上暂停挺" to "jsztt", "宽距后蹲" to "kjhd",
    "宽速拉" to "ksl", "宽硬拉" to "kyl", "平板卧推" to "pbwt", "实力推" to "slt", "挺支" to "tz", "膝上高翻" to "xsgf", "膝上高拉+高抓" to "xsglgz",
    "膝上高抓" to "xsgz", "膝上抓" to "xsz", "下蹲翻" to "xdf", "小相扑硬拉" to "xxpyl",
    "悬垂举腿" to "xcjt", "悬垂抓举" to "xczj", "哑铃单腿硬拉" to "yldtyl", "泽奇蹲" to "zqd",
    "泽奇后弓步蹲" to "zqhgbd", "窄速拉" to "zsl", "窄硬拉" to "zyl", "抓支" to "zz",
    "半抓" to "bz", "翻铃" to "fl", "悬垂高翻" to "xcgf", "半挺" to "bt", "箭步挺" to "jbt",
    "分腿挺" to "ftt", "下蹲挺" to "xdt", "借力推" to "jlt", "颈后借力推" to "jhjlt",
    "抓举硬拉" to "zjyl", "挺举硬拉" to "tjyl", "过顶深蹲" to "gdsd", "架上高翻" to "jsgf", "架上抓举" to "jszj",
    "下斜卧推" to "xxwt", "哑铃卧推" to "ylwt", "上斜哑铃卧推" to "sxylwt", "下斜哑铃卧推" to "xxylwt",
    "地板卧推" to "dbwt", "史密斯卧推" to "smswt", "窄握卧推" to "zwwt",
    "哑铃飞鸟" to "ylfn", "上斜哑铃飞鸟" to "sxylfn", "绳索夹胸" to "ssjx", "蝴蝶机夹胸" to "hdjjx",
    "上斜俯卧撑" to "sxfwc", "下斜俯卧撑" to "xxfwc",
    "宽握引体向上" to "kwyt", "反手引体向上" to "fsyt", "对握引体向上" to "dwyt",
    "反手高位下拉" to "fsgwxl", "颈后高位下拉" to "jhgwxl", "反手杠铃划船" to "fsglhc",
    "单臂哑铃划船" to "dbylhc", "坐姿绳索划船" to "zzsshc", "T杠划船" to "tghc", "潘德雷划船" to "pdlhc",
    "直腿硬拉" to "ztyl", "相扑硬拉" to "xpyl", "山羊挺身" to "ysts", "早安式体前屈" to "zastqq",
    "杠铃耸肩" to "glsj", "哑铃耸肩" to "ylsj", "农夫行走" to "nmxz",
    "推举" to "tj", "坐姿哑铃推举" to "zzyltj", "阿诺德推举" to "andtj", "颈后推举" to "jhtj", "器械推肩" to "qxtj",
    "前平举" to "qpj", "俯身侧平举" to "fscpj", "绳索侧平举" to "sscpj",
    "俯身飞鸟" to "fsfn", "反向飞鸟" to "fxfn", "直立划船" to "zlhc", "古巴推举" to "gbtj",
    "杠铃弯举" to "glwj", "哑铃弯举" to "ylwj", "锤式弯举" to "cswj", "牧师凳弯举" to "msdwj",
    "集中弯举" to "jwj", "绳索弯举" to "sswj", "直杆下压" to "zgxy", "单臂绳索下压" to "dbssxy",
    "仰卧臂屈伸" to "ywbqs", "颈后臂屈伸" to "jhbqs", "过顶臂屈伸" to "gdbqs",
    "高脚杯深蹲" to "gjbsd", "哈克深蹲" to "hksd", "箱式深蹲" to "xssd", "暂停深蹲" to "ztsd", "史密斯深蹲" to "smssd",
    "腿屈伸" to "tqs", "坐姿腿弯举" to "zztwj", "俯卧腿弯举" to "fwtwj",
    "箭步蹲" to "jbd", "行走箭步蹲" to "xzjbd", "侧弓步" to "cgb", "台阶蹬踏" to "tjdt",
    "臀桥" to "qt", "站姿提踵" to "zstz", "坐姿提踵" to "zztz",
    "反向卷腹" to "fxjf", "仰卧起坐" to "ywqz", "负重仰卧起坐" to "fzywqz",
    "俄罗斯转体" to "elszt", "登山跑" to "dsp", "死虫式" to "scs", "鸟狗式" to "ngs", "健腹轮" to "jfl", "V字两头起" to "vzltq",
    "土耳其起立" to "teql", "药球下砸" to "yqxz", "战绳" to "zs", "波比跳" to "bbt",
    "跳箱" to "tx", "雪橇推" to "xqt", "负重行走" to "fzxz"
)

// english = true 时额外匹配英文动作名与英文首字母缩略（例如 "rdl" 可命中罗马尼亚硬拉）。
// 默认 false，纯中文环境与既有单元测试行为不变。
fun workoutExerciseMatches(name: String, query: String, english: Boolean = false): Boolean {
    val normalized = query.trim().lowercase().replace(" ", "")
    if (normalized.isEmpty() || name.contains(query.trim(), ignoreCase = true)) return true
    if (workoutExerciseInitials[name]?.contains(normalized) == true) return true
    if (!english) return false
    val englishName = exerciseEnglishNames[name] ?: return false
    if (englishName.contains(query.trim(), ignoreCase = true)) return true
    val initials = englishName.split(' ', '-', '(', ')', '/')
        .filter { it.isNotBlank() }
        .joinToString("") { it.first().lowercase() }
    return initials.contains(normalized)
}

enum class MuscleRegion {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS,
    CORE, GLUTES, QUADS, HAMSTRINGS, CALVES,
    LOWER_BACK, ERECTOR_SPINAE
}

fun muscleRegionsForExercise(name: String): Set<MuscleRegion> = buildSet {
    when {
        name.contains("腿弯举") -> addAll(setOf(MuscleRegion.HAMSTRINGS, MuscleRegion.GLUTES))
        name.contains("腿屈伸") -> add(MuscleRegion.QUADS)
        name.contains("提踵") -> add(MuscleRegion.CALVES)
        name.contains("蹲") || name.contains("弓步") || name.contains("腿举") || name.contains("蹬踏") || name.contains("跳箱") || name.contains("雪橇") -> addAll(setOf(MuscleRegion.QUADS, MuscleRegion.GLUTES, MuscleRegion.CORE))
        name.contains("硬拉") || name.contains("速拉") || name.contains("早安") || name.contains("挺身") -> addAll(setOf(MuscleRegion.BACK, MuscleRegion.GLUTES, MuscleRegion.HAMSTRINGS, MuscleRegion.LOWER_BACK, MuscleRegion.ERECTOR_SPINAE))
        name.contains("高拉") -> addAll(setOf(MuscleRegion.BACK, MuscleRegion.GLUTES, MuscleRegion.HAMSTRINGS, MuscleRegion.ERECTOR_SPINAE))
        name.contains("下拉") || name.contains("划船") || name.contains("引体") -> addAll(setOf(MuscleRegion.BACK, MuscleRegion.BICEPS))
        name.contains("卧推") || name.contains("俯卧撑") -> addAll(setOf(MuscleRegion.CHEST, MuscleRegion.TRICEPS, MuscleRegion.SHOULDERS))
        name.contains("反向飞鸟") || name.contains("俯身飞鸟") -> add(MuscleRegion.SHOULDERS)
        name.contains("飞鸟") || name.contains("夹胸") -> add(MuscleRegion.CHEST)
        name.contains("挺举") || name.contains("抓举") || name.contains("高抓") || name.contains("高翻") || name.contains("挺支") || name.contains("抓支") ||
            name.contains("半挺") || name.contains("半抓") || name.contains("箭步挺") || name.contains("分腿挺") || name.contains("下蹲挺") || name.contains("翻铃") -> addAll(setOf(MuscleRegion.QUADS, MuscleRegion.GLUTES, MuscleRegion.BACK, MuscleRegion.SHOULDERS, MuscleRegion.CORE, MuscleRegion.ERECTOR_SPINAE))
        name.contains("臀推") || name.contains("臀桥") -> addAll(setOf(MuscleRegion.GLUTES, MuscleRegion.HAMSTRINGS))
        name.contains("推") || name.contains("平举") -> addAll(setOf(MuscleRegion.SHOULDERS, MuscleRegion.TRICEPS))
        name.contains("弯举") -> add(MuscleRegion.BICEPS)
        name.contains("下压") || name.contains("臂屈伸") -> add(MuscleRegion.TRICEPS)
        name.contains("耸肩") -> add(MuscleRegion.BACK)
        name.contains("摇摆") -> addAll(setOf(MuscleRegion.GLUTES, MuscleRegion.HAMSTRINGS, MuscleRegion.CORE))
        name.contains("支撑") || name.contains("侧桥") || name.contains("卷腹") || name.contains("举腿") || name.contains("起坐") -> add(MuscleRegion.CORE)
        else -> add(MuscleRegion.CORE)
    }
}

// 训练主题用语义枚举表达，标题文案在 UI 层翻译；背景图选择也直接依据 kind，
// 不再靠中文字符串比较。
enum class WorkoutFocusKind { PREPARATION, LEG_POWER, PUSH, PULL, POSTERIOR_CHAIN, REGION }

data class WorkoutFocus(
    val kind: WorkoutFocusKind,
    val primaryRegion: MuscleRegion?,
    val regions: List<MuscleRegion>
)

fun summarizeWorkoutFocus(exercises: List<WorkoutExercise>): WorkoutFocus {
    if (exercises.isEmpty()) return WorkoutFocus(WorkoutFocusKind.PREPARATION, null, emptyList())
    val scores = exercises
        .flatMap { exercise ->
            val weight = exercise.sets.coerceIn(1, 6)
            muscleRegionsForExercise(exercise.name).map { it to weight }
        }
        .groupingBy { it.first }
        .fold(0) { acc, region -> acc + region.second }
    val ranked = scores.entries
        .sortedWith(compareByDescending<Map.Entry<MuscleRegion, Int>> { it.value }.thenBy { it.key.ordinal })
        .map { it.key }
    val active = scores.keys
    val legRegions = setOf(MuscleRegion.QUADS, MuscleRegion.HAMSTRINGS, MuscleRegion.GLUTES, MuscleRegion.CALVES)
    val pushRegions = setOf(MuscleRegion.CHEST, MuscleRegion.SHOULDERS, MuscleRegion.TRICEPS)
    val pullRegions = setOf(MuscleRegion.BACK, MuscleRegion.BICEPS)
    val posteriorRegions = setOf(MuscleRegion.BACK, MuscleRegion.LOWER_BACK, MuscleRegion.ERECTOR_SPINAE, MuscleRegion.GLUTES, MuscleRegion.HAMSTRINGS)
    val kind = when {
        active.intersect(legRegions).size >= 2 -> WorkoutFocusKind.LEG_POWER
        active.containsAll(pushRegions) -> WorkoutFocusKind.PUSH
        active.containsAll(pullRegions) -> WorkoutFocusKind.PULL
        active.intersect(posteriorRegions).size >= 3 -> WorkoutFocusKind.POSTERIOR_CHAIN
        else -> WorkoutFocusKind.REGION
    }
    return WorkoutFocus(kind, ranked.firstOrNull(), ranked)
}

data class WorkoutStatistics(
    val completedSessions: Int,
    val completedCycles: Int,
    val regionSets: List<Pair<MuscleRegion, Int>>
)

fun calculateWorkoutStatistics(days: List<WorkoutDay>): WorkoutStatistics {
    val completedDays = days.filter { it.mode == DayMode.TRAINING && it.isCompleted }
    val completedCycles = days
        .filter { it.mode == DayMode.TRAINING }
        .groupBy { day -> day.date.minusDays((day.date.dayOfWeek.value - 1).toLong()) }
        .count { (_, trainingDays) -> trainingDays.isNotEmpty() && trainingDays.all { it.isCompleted } }
    val regionSets = completedDays
        .flatMap { day ->
            day.exercises.flatMap { exercise ->
                muscleRegionsForExercise(exercise.name).map { region -> region to exercise.sets.coerceAtLeast(0) }
            }
        }
        .groupingBy { it.first }
        .fold(0) { total, entry -> total + entry.second }
        .entries
        .sortedWith(compareByDescending<Map.Entry<MuscleRegion, Int>> { it.value }.thenBy { it.key.ordinal })
        .map { it.key to it.value }
    return WorkoutStatistics(completedDays.size, completedCycles, regionSets)
}

fun buildWorkoutCycle(
    anchor: LocalDate,
    trainingDays: Set<DayOfWeek>,
    templates: List<WorkoutDay>,
    cycleWeeks: Int = 4,
    deloadWeek: Int = cycleWeeks
): List<WorkoutDay> {
    require(cycleWeeks in 1..12) { "周期周数需在 1 到 12 周之间" }
    require(deloadWeek in 1..cycleWeeks) { "减载周必须在周期内" }
    val monday = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
    val templateByWeekday = templates
        .filter { it.mode == DayMode.TRAINING && it.exercises.isNotEmpty() }
        .groupBy { it.date.dayOfWeek }
        .mapValues { (_, days) -> days.minByOrNull { kotlin.math.abs(it.date.toEpochDay() - monday.toEpochDay()) }!! }
    val defaults = defaultWorkoutWeek(monday).associateBy { it.date.dayOfWeek }

    return (0 until cycleWeeks * 7).map { offset ->
        val date = monday.plusDays(offset.toLong())
        val week = offset / 7 + 1
        val training = date.dayOfWeek in trainingDays
        val source = templateByWeekday[date.dayOfWeek] ?: defaults[date.dayOfWeek]
        WorkoutDay(
            date = date,
            mode = if (training) DayMode.TRAINING else DayMode.REST,
            exercises = if (training) source?.exercises.orEmpty().mapIndexed { index, exercise ->
                exercise.copy(
                    id = date.toEpochDay() * 100 + index,
                    sets = if (week == deloadWeek) maxOf(1, kotlin.math.ceil(exercise.sets * .6).toInt()) else exercise.sets
                )
            } else emptyList(),
            cycleWeek = week,
            isDeload = week == deloadWeek,
            isCompleted = templates.firstOrNull { it.date == date }?.isCompleted == true
        )
    }
}

fun defaultWorkoutWeek(anchor: LocalDate = LocalDate.now()): List<WorkoutDay> {
    val monday = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
    val templates = mapOf(
        DayOfWeek.MONDAY to listOf(
            WorkoutExercise(101, "抓举", 5, "2", "80%"),
            WorkoutExercise(102, "前蹲", 4, "3", "85%"),
            WorkoutExercise(103, "高位下拉", 4, "10", "自定义")
        ),
        DayOfWeek.TUESDAY to listOf(
            WorkoutExercise(201, "高翻", 5, "2", "80%"),
            WorkoutExercise(202, "窄拉", 3, "3", "85%"),
            WorkoutExercise(203, "实心球", 5, "7", "自定义")
        ),
        DayOfWeek.THURSDAY to listOf(
            WorkoutExercise(401, "深蹲", 4, "4", "85%"),
            WorkoutExercise(402, "器械反向划船", 4, "10", "自定义"),
            WorkoutExercise(403, "侧桥", 3, "30秒", "自重")
        ),
        DayOfWeek.SATURDAY to listOf(
            WorkoutExercise(601, "挺举", 5, "1", "90%"),
            WorkoutExercise(602, "罗马尼亚硬拉", 4, "7", "75%"),
            WorkoutExercise(603, "单腿硬拉", 4, "10", "自定义")
        )
    )
    return (0L..6L).map { offset ->
        val date = monday.plusDays(offset)
        val exercises = templates[date.dayOfWeek].orEmpty()
        WorkoutDay(date, mode = if (exercises.isEmpty()) DayMode.REST else DayMode.TRAINING, exercises = exercises)
    }
}

private fun restSecondsForWeight(weight: String): Int {
    val trimmed = weight.trim()
    if (trimmed.endsWith("%")) {
        val percent = trimmed.removeSuffix("%").toDoubleOrNull()
        if (percent != null) return (60 + (percent * 1.2).toInt()).coerceIn(60, 240)
    }
    val kg = trimmed.lowercase().removeSuffix("kg").trim().toDoubleOrNull()
    if (kg != null) return (75 + (kg * 0.6).toInt()).coerceIn(75, 240)
    return 75
}

fun estimateWorkoutMinutes(exercises: List<WorkoutExercise>): Int {
    if (exercises.isEmpty()) return 0
    val workSecondsPerSet = 40
    val totalSeconds = exercises.sumOf { exercise ->
        120 + exercise.sets * (workSecondsPerSet + restSecondsForWeight(exercise.weight))
    }
    return (8 * 60 + totalSeconds + 30) / 60
}

data class MealMacroShare(
    val carbohydrates: Double,
    val protein: Double,
    val fat: Double
)

private fun shares(vararg values: Pair<MealPeriod, Double>): Map<MealPeriod, MealMacroShare> =
    values.associate { (period, share) -> period to MealMacroShare(share, share, share) }

fun defaultMealMacroShares(): Map<MealPeriod, MealMacroShare> = shares(
    MealPeriod.BREAKFAST to .20,
    MealPeriod.LUNCH to .30,
    MealPeriod.PRE_WORKOUT to .0,
    MealPeriod.DINNER to .35,
    MealPeriod.NIGHT_SNACK to .15
)

fun defaultTrainingMealMacroShares(): Map<MealPeriod, MealMacroShare> = shares(
    MealPeriod.BREAKFAST to .20,
    MealPeriod.LUNCH to .25,
    MealPeriod.PRE_WORKOUT to .10,
    MealPeriod.DINNER to .30,
    MealPeriod.NIGHT_SNACK to .15
)

data class CaloriePlan(
    val trainingCarbsGrams: Double = 275.0,
    val trainingProteinGrams: Double = 165.0,
    val trainingFatGrams: Double = 220.0 / 4.5,
    val restCarbsGrams: Double = 225.0,
    val restProteinGrams: Double = 135.0,
    val restFatGrams: Double = 40.0,
    val trainingDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
    val mealMacroShares: Map<MealPeriod, MealMacroShare> = defaultMealMacroShares(),
    val trainingMealMacroShares: Map<MealPeriod, MealMacroShare> = defaultTrainingMealMacroShares(),
    val dayModeOverrides: Map<LocalDate, DayMode> = emptyMap()
) {
    val trainingTargetKcal: Double get() = caloriesFromMacros(trainingCarbsGrams, trainingProteinGrams, trainingFatGrams)
    val restTargetKcal: Double get() = caloriesFromMacros(restCarbsGrams, restProteinGrams, restFatGrams)

    fun modeFor(date: LocalDate): DayMode = dayModeOverrides[date]
        ?: if (date.dayOfWeek in trainingDays) DayMode.TRAINING else DayMode.REST
    fun targetFor(date: LocalDate): Double = if (modeFor(date) == DayMode.TRAINING) trainingTargetKcal else restTargetKcal
    fun mealPeriodsFor(date: LocalDate): List<MealPeriod> = if (modeFor(date) == DayMode.TRAINING) {
        MealPeriod.entries
    } else {
        MealPeriod.entries.filterNot { it == MealPeriod.PRE_WORKOUT }
    }
    fun macroTargetsFor(date: LocalDate): Triple<Double, Double, Double> =
        if (modeFor(date) == DayMode.TRAINING) Triple(trainingCarbsGrams, trainingProteinGrams, trainingFatGrams)
        else Triple(restCarbsGrams, restProteinGrams, restFatGrams)

    fun macroTargetsFor(date: LocalDate, period: MealPeriod): Triple<Double, Double, Double> {
        val daily = macroTargetsFor(date)
        if (period !in mealPeriodsFor(date)) return Triple(0.0, 0.0, 0.0)
        val shares = if (modeFor(date) == DayMode.TRAINING) trainingMealMacroShares else mealMacroShares
        val defaults = if (modeFor(date) == DayMode.TRAINING) defaultTrainingMealMacroShares() else defaultMealMacroShares()
        val share = shares[period] ?: defaults.getValue(period)
        return Triple(daily.first * share.carbohydrates, daily.second * share.protein, daily.third * share.fat)
    }

    fun calorieTargetFor(date: LocalDate, period: MealPeriod): Double {
        val target = macroTargetsFor(date, period)
        return caloriesFromMacros(target.first, target.second, target.third)
    }

    companion object {
        fun fromLegacyCalories(trainingKcal: Double, restKcal: Double, trainingDays: Set<DayOfWeek>): CaloriePlan = CaloriePlan(
            trainingCarbsGrams = trainingKcal * .5 / 4.0,
            trainingProteinGrams = trainingKcal * .3 / 4.0,
            trainingFatGrams = trainingKcal * .2 / 9.0,
            restCarbsGrams = restKcal * .5 / 4.0,
            restProteinGrams = restKcal * .3 / 4.0,
            restFatGrams = restKcal * .2 / 9.0,
            trainingDays = trainingDays,
            mealMacroShares = defaultMealMacroShares(),
            trainingMealMacroShares = defaultTrainingMealMacroShares()
        )
    }
}
