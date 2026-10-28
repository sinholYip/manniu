package com.ranke.health.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.ranke.health.R
import com.ranke.health.model.BiologicalSex
import com.ranke.health.model.BmiStatus
import com.ranke.health.model.DayMode
import com.ranke.health.model.MealPeriod
import com.ranke.health.model.MuscleRegion
import com.ranke.health.model.WorkoutFocus
import com.ranke.health.model.WorkoutFocusKind
import com.ranke.health.ui.theme.TrendRange
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 生效中的界面语言。由 MainActivity 在最外层提供，Dialog 等子组合也能取到，
 * 不受各自 Context 的 configuration 影响。
 */
internal val LocalAppLocale = staticCompositionLocalOf { Locale.getDefault() }

internal fun isEnglish(locale: Locale): Boolean = locale.language == "en"

@Composable
internal fun isEnglishUi(): Boolean = isEnglish(LocalAppLocale.current)

// ===== 枚举文案 =====

@Composable
internal fun MealPeriod.label(): String = stringResource(
    when (this) {
        MealPeriod.BREAKFAST -> R.string.meal_breakfast
        MealPeriod.LUNCH -> R.string.meal_lunch
        MealPeriod.PRE_WORKOUT -> R.string.meal_pre_workout
        MealPeriod.DINNER -> R.string.meal_dinner
        MealPeriod.NIGHT_SNACK -> R.string.meal_night_snack
    }
)

@Composable
internal fun DayMode.label(): String = stringResource(
    if (this == DayMode.TRAINING) R.string.day_training else R.string.day_rest
)

@Composable
internal fun BiologicalSex.label(): String = stringResource(
    if (this == BiologicalSex.MALE) R.string.sex_male else R.string.sex_female
)

@Composable
internal fun BmiStatus.label(): String = stringResource(
    when (this) {
        BmiStatus.UNDERWEIGHT -> R.string.bmi_underweight
        BmiStatus.NORMAL -> R.string.bmi_normal
        BmiStatus.OVERWEIGHT -> R.string.bmi_overweight
        BmiStatus.OBESE -> R.string.bmi_obese
    }
)

@Composable
internal fun MuscleRegion.label(): String = stringResource(
    when (this) {
        MuscleRegion.CHEST -> R.string.region_chest
        MuscleRegion.BACK -> R.string.region_back
        MuscleRegion.SHOULDERS -> R.string.region_shoulders
        MuscleRegion.BICEPS -> R.string.region_biceps
        MuscleRegion.TRICEPS -> R.string.region_triceps
        MuscleRegion.CORE -> R.string.region_core
        MuscleRegion.GLUTES -> R.string.region_glutes
        MuscleRegion.QUADS -> R.string.region_quads
        MuscleRegion.HAMSTRINGS -> R.string.region_hamstrings
        MuscleRegion.CALVES -> R.string.region_calves
        MuscleRegion.LOWER_BACK -> R.string.region_lower_back
        MuscleRegion.ERECTOR_SPINAE -> R.string.region_erector_spinae
    }
)

@Composable
internal fun workoutFocusTitle(focus: WorkoutFocus): String = when (focus.kind) {
    WorkoutFocusKind.PREPARATION -> stringResource(R.string.focus_preparation)
    WorkoutFocusKind.LEG_POWER -> stringResource(R.string.focus_leg_power)
    WorkoutFocusKind.PUSH -> stringResource(R.string.focus_push)
    WorkoutFocusKind.PULL -> stringResource(R.string.focus_pull)
    WorkoutFocusKind.POSTERIOR_CHAIN -> stringResource(R.string.focus_posterior_chain)
    WorkoutFocusKind.REGION -> stringResource(R.string.focus_region)
        .format(focus.primaryRegion?.label().orEmpty())
}

@Composable
internal fun TrendRange.label(): String = stringResource(
    when (this) {
        TrendRange.DAYS_14 -> R.string.range_14
        TrendRange.DAYS_30 -> R.string.range_30
        TrendRange.DAYS_60 -> R.string.range_60
        TrendRange.HALF_YEAR -> R.string.range_half_year
        TrendRange.ALL -> R.string.range_all
    }
)

@Composable
internal fun AppLanguage.label(): String = stringResource(
    when (this) {
        AppLanguage.SYSTEM -> R.string.lang_system
        AppLanguage.CHINESE -> R.string.lang_zh
        AppLanguage.ENGLISH -> R.string.lang_en
    }
)

// ===== 日期与星期 =====
// 三种格式都按“当前应用语言”生成，而不是系统 Locale：
// 应用内切到英文时，即便手机是中文系统，图表坐标轴也应当是英文。

internal fun monthDayFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern(if (isEnglish(locale)) "MMM d" else "M月d日", locale)

internal fun shortDateFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern("M/d", locale)

internal fun prDateFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern(if (isEnglish(locale)) "MMM d, yyyy" else "yyyy.M.d", locale)

internal fun DayOfWeek.shortName(locale: Locale): String = if (isEnglish(locale)) {
    getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
} else when (this) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}

/** 周条上只有约 40dp 宽，中文取“一”，英文用三字母缩写。 */
internal fun DayOfWeek.stripName(locale: Locale): String =
    if (isEnglish(locale)) shortName(locale) else shortName(locale).removePrefix("周")
