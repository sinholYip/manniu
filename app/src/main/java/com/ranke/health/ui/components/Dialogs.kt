package com.ranke.health.ui.components


import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import com.ranke.health.R
import com.ranke.health.core.formatNutrition
import com.ranke.health.core.rebalanceMealMacro
import com.ranke.health.core.scaleMealMacro
import com.ranke.health.data.FoodCatalogRepository
import com.ranke.health.model.CaloriePlan
import com.ranke.health.model.DayMode
import com.ranke.health.model.FoodReference
import com.ranke.health.model.MealEntry
import com.ranke.health.model.MealMacroShare
import com.ranke.health.model.MealPeriod
import com.ranke.health.model.caloriesFromMacros
import com.ranke.health.model.canonicalFoodName
import com.ranke.health.model.defaultMealMacroShares
import com.ranke.health.model.defaultTrainingMealMacroShares
import com.ranke.health.model.foodDisplayName
import com.ranke.health.model.inferMealPeriod
import com.ranke.health.model.sourceDisplayName
import com.ranke.health.ui.i18n.LocalAppLocale
import com.ranke.health.ui.i18n.isEnglishUi
import com.ranke.health.ui.i18n.label
import com.ranke.health.ui.i18n.shortName
import com.ranke.health.ui.theme.Accent
import com.ranke.health.ui.theme.AppControlShape
import com.ranke.health.ui.theme.AppModalBlurRadius
import com.ranke.health.ui.theme.AppModalMaxHeight
import com.ranke.health.ui.theme.AppModalOuterPadding
import com.ranke.health.ui.theme.AppModalShape
import com.ranke.health.ui.theme.Background
import com.ranke.health.ui.theme.CardOutline
import com.ranke.health.ui.theme.Ink
import com.ranke.health.ui.theme.Lime
import com.ranke.health.ui.theme.Muted
import com.ranke.health.ui.theme.Outline
import com.ranke.health.ui.theme.Raised
import com.ranke.health.ui.theme.UpRed
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
internal fun AppModalSheet(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
    footer: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val dialogView = LocalView.current
        val closeLabel = stringResource(R.string.action_close)
        val blurRadiusPx = with(LocalDensity.current) { AppModalBlurRadius.roundToPx() }
        DisposableEffect(dialogView, blurRadiusPx) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window?.attributes = window?.attributes?.apply {
                    dimAmount = 0.24f
                    blurBehindRadius = blurRadiusPx
                }
            } else {
                window?.setDimAmount(0.72f)
            }
            onDispose {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window?.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window?.attributes = window?.attributes?.apply { blurBehindRadius = 0 }
                }
            }
        }
        Box(Modifier.fillMaxSize().padding(AppModalOuterPadding), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = AppModalMaxHeight),
                colors = CardDefaults.cardColors(containerColor = Background),
                shape = AppModalShape,
                border = BorderStroke(1.dp, CardOutline)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(20.dp, 16.dp, 12.dp, 10.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            // 英文标题更长，允许折行，容器宽度不变。
                            Text(title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                            subtitle?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, color = Muted, fontSize = 12.sp, maxLines = 2)
                            }
                        }
                        Box(Modifier.size(38.dp).background(Raised, CircleShape).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, closeLabel, tint = Muted, modifier = Modifier.size(19.dp))
                        }
                    }
                    Column(
                        Modifier.weight(1f).fillMaxWidth()
                            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) { content() }
                    Box(Modifier.fillMaxWidth().background(Background).padding(20.dp, 14.dp, 20.dp, 18.dp)) { footer() }
                }
            }
        }
    }
}

@Composable
internal fun PrimaryModalButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Background),
        shape = AppControlShape
    ) { FitText(label, maxFontSize = 15.sp, minFontSize = 11.sp, color = Background, fontWeight = FontWeight.Bold) }
}

@Composable
internal fun CaloriePlanDialog(plan: CaloriePlan, onDismiss: () -> Unit, onSave: (CaloriePlan) -> Unit) {
    var page by remember { mutableStateOf(0) }
    var trainingCarbs by remember(plan) { mutableStateOf(formatNutrition(plan.trainingCarbsGrams)) }
    var trainingProtein by remember(plan) { mutableStateOf(formatNutrition(plan.trainingProteinGrams)) }
    var trainingFat by remember(plan) { mutableStateOf(formatNutrition(plan.trainingFatGrams)) }
    var restCarbs by remember(plan) { mutableStateOf(formatNutrition(plan.restCarbsGrams)) }
    var restProtein by remember(plan) { mutableStateOf(formatNutrition(plan.restProteinGrams)) }
    var restFat by remember(plan) { mutableStateOf(formatNutrition(plan.restFatGrams)) }
    var trainingDays by remember(plan) { mutableStateOf(plan.trainingDays) }
    var restMealShares by remember(plan) { mutableStateOf(
        MealPeriod.entries.associateWith { period ->
            val share = plan.mealMacroShares[period] ?: defaultMealMacroShares().getValue(period)
            Triple(
                formatNutrition(share.carbohydrates * plan.restCarbsGrams),
                formatNutrition(share.protein * plan.restProteinGrams),
                formatNutrition(share.fat * plan.restFatGrams)
            )
        }
    ) }
    var trainingMealShares by remember(plan) { mutableStateOf(
        MealPeriod.entries.associateWith { period ->
            val share = plan.trainingMealMacroShares[period] ?: defaultTrainingMealMacroShares().getValue(period)
            Triple(
                formatNutrition(share.carbohydrates * plan.trainingCarbsGrams),
                formatNutrition(share.protein * plan.trainingProteinGrams),
                formatNutrition(share.fat * plan.trainingFatGrams)
            )
        }
    ) }
    var trainingLockedMacros by remember(plan) { mutableStateOf<Map<Int, Set<MealPeriod>>>(emptyMap()) }
    var restLockedMacros by remember(plan) { mutableStateOf<Map<Int, Set<MealPeriod>>>(emptyMap()) }
    val trainingValues = listOf(trainingCarbs, trainingProtein, trainingFat).map { it.toDoubleOrNull() }
    val restValues = listOf(restCarbs, restProtein, restFat).map { it.toDoubleOrNull() }
    val trainingCalories = caloriesFromMacros(trainingValues[0] ?: 0.0, trainingValues[1] ?: 0.0, trainingValues[2] ?: 0.0)
    val restCalories = caloriesFromMacros(restValues[0] ?: 0.0, restValues[1] ?: 0.0, restValues[2] ?: 0.0)
    val valuesValid = (trainingValues + restValues).all { it != null && it in 0.0..1000.0 } &&
        trainingCalories in 800.0..6000.0 && restCalories in 800.0..6000.0
    fun parseShares(source: Map<MealPeriod, Triple<String, String, String>>) = source.mapValues { (_, values) ->
        Triple(values.first.toDoubleOrNull(), values.second.toDoubleOrNull(), values.third.toDoubleOrNull())
    }
    fun sharesAreValid(
        parsed: Map<MealPeriod, Triple<Double?, Double?, Double?>>,
        periods: List<MealPeriod>,
        targets: Triple<Double, Double, Double>
    ): Boolean {
        val values = periods.map { parsed.getValue(it) }
        val sums = Triple(
            values.sumOf { it.first ?: 0.0 },
            values.sumOf { it.second ?: 0.0 },
            values.sumOf { it.third ?: 0.0 }
        )
        return values.flatMap { listOf(it.first, it.second, it.third) }.all { it != null && it in 0.0..1000.0 } &&
            kotlin.math.abs(sums.first - targets.first) < .11 &&
            kotlin.math.abs(sums.second - targets.second) < .11 &&
            kotlin.math.abs(sums.third - targets.third) < .11
    }
    val parsedRestShares = parseShares(restMealShares)
    val parsedTrainingShares = parseShares(trainingMealShares)
    val restPeriods = MealPeriod.entries.filterNot { it == MealPeriod.PRE_WORKOUT }
    val trainingPeriods = MealPeriod.entries
    val currentTrainingTargets = Triple(trainingValues[0] ?: 0.0, trainingValues[1] ?: 0.0, trainingValues[2] ?: 0.0)
    val currentRestTargets = Triple(restValues[0] ?: 0.0, restValues[1] ?: 0.0, restValues[2] ?: 0.0)
    val restSharesValid = sharesAreValid(parsedRestShares, restPeriods, currentRestTargets)
    val trainingSharesValid = sharesAreValid(parsedTrainingShares, trainingPeriods, currentTrainingTargets)
    val sharesValid = restSharesValid && trainingSharesValid
    fun ratio(value: Double, target: Double): Double = if (target > 0.0) value / target else 0.0
    val resolvedRestShares = if (restSharesValid) parsedRestShares.mapValues { (_, values) ->
        MealMacroShare(
            ratio(values.first!!, currentRestTargets.first),
            ratio(values.second!!, currentRestTargets.second),
            ratio(values.third!!, currentRestTargets.third)
        )
    } else plan.mealMacroShares
    val resolvedTrainingShares = if (trainingSharesValid) parsedTrainingShares.mapValues { (_, values) ->
        MealMacroShare(
            ratio(values.first!!, currentTrainingTargets.first),
            ratio(values.second!!, currentTrainingTargets.second),
            ratio(values.third!!, currentTrainingTargets.third)
        )
    } else plan.trainingMealMacroShares

    AppModalSheet(
        title = stringResource(R.string.plan_title),
        subtitle = stringResource(R.string.plan_sub),
        onDismiss = onDismiss,
        content = {
            Row(
                Modifier.fillMaxWidth().background(Raised, AppControlShape)
                    .border(1.dp, Outline, AppControlShape).padding(4.dp)
            ) {
                listOf(stringResource(R.string.plan_tab_targets), stringResource(R.string.plan_tab_split)).forEachIndexed { index, label ->
                    Box(
                        Modifier.weight(1f).background(if (page == index) Lime else Color.Transparent, AppControlShape)
                            .clickable { page = index }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FitText(
                            text = label,
                            maxFontSize = 13.sp,
                            minFontSize = 10.sp,
                            color = if (page == index) Background else Muted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (page == 0) {
                Text(stringResource(R.string.plan_targets_hint), color = Muted, fontSize = 11.sp)
                MacroPlanEditor(
                    title = stringResource(R.string.day_training), calories = trainingCalories,
                    carbs = trainingCarbs, onCarbsChange = {
                        trainingCarbs = it
                        trainingLockedMacros = trainingLockedMacros - 0
                        it.toDoubleOrNull()?.let { target -> trainingMealShares = scaleMealMacro(trainingMealShares, 0, target, trainingPeriods) }
                    },
                    protein = trainingProtein, onProteinChange = {
                        trainingProtein = it
                        trainingLockedMacros = trainingLockedMacros - 1
                        it.toDoubleOrNull()?.let { target -> trainingMealShares = scaleMealMacro(trainingMealShares, 1, target, trainingPeriods) }
                    },
                    fat = trainingFat, onFatChange = {
                        trainingFat = it
                        trainingLockedMacros = trainingLockedMacros - 2
                        it.toDoubleOrNull()?.let { target -> trainingMealShares = scaleMealMacro(trainingMealShares, 2, target, trainingPeriods) }
                    }
                )
                MacroPlanEditor(
                    title = stringResource(R.string.day_rest), calories = restCalories,
                    carbs = restCarbs, onCarbsChange = {
                        restCarbs = it
                        restLockedMacros = restLockedMacros - 0
                        it.toDoubleOrNull()?.let { target -> restMealShares = scaleMealMacro(restMealShares, 0, target, restPeriods) }
                    },
                    protein = restProtein, onProteinChange = {
                        restProtein = it
                        restLockedMacros = restLockedMacros - 1
                        it.toDoubleOrNull()?.let { target -> restMealShares = scaleMealMacro(restMealShares, 1, target, restPeriods) }
                    },
                    fat = restFat, onFatChange = {
                        restFat = it
                        restLockedMacros = restLockedMacros - 2
                        it.toDoubleOrNull()?.let { target -> restMealShares = scaleMealMacro(restMealShares, 2, target, restPeriods) }
                    }
                )
                Text(stringResource(R.string.plan_pick_days), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        WeekdayChip(
                            day = day,
                            selected = day in trainingDays,
                            onClick = { trainingDays = if (day in trainingDays) trainingDays - day else trainingDays + day },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Text(stringResource(R.string.plan_split_hint), color = Muted, fontSize = 11.sp)
                Text(stringResource(R.string.plan_training_day_pre), color = Lime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                trainingPeriods.forEach { period ->
                    val values = trainingMealShares.getValue(period)
                    MealDistributionEditor(
                        period = period,
                        values = values,
                        onChange = { macroIndex, value ->
                            val locked = trainingLockedMacros[macroIndex].orEmpty() + period
                            trainingLockedMacros = trainingLockedMacros + (macroIndex to locked)
                            trainingMealShares = rebalanceMealMacro(
                                trainingMealShares, period, macroIndex, value,
                                listOf(currentTrainingTargets.first, currentTrainingTargets.second, currentTrainingTargets.third)[macroIndex],
                                trainingPeriods, locked
                            )
                        }
                    )
                }
                if (!trainingSharesValid) Text(stringResource(R.string.plan_training_split_invalid), color = UpRed, fontSize = 10.sp)
                Text(stringResource(R.string.plan_rest_day_no_pre), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                restPeriods.forEach { period ->
                    val values = restMealShares.getValue(period)
                    MealDistributionEditor(
                        period = period,
                        values = values,
                        onChange = { macroIndex, value ->
                            val locked = restLockedMacros[macroIndex].orEmpty() + period
                            restLockedMacros = restLockedMacros + (macroIndex to locked)
                            restMealShares = rebalanceMealMacro(
                                restMealShares, period, macroIndex, value,
                                listOf(currentRestTargets.first, currentRestTargets.second, currentRestTargets.third)[macroIndex],
                                restPeriods, locked
                            )
                        }
                    )
                }
                if (!restSharesValid) Text(stringResource(R.string.plan_rest_split_invalid), color = UpRed, fontSize = 10.sp)
            }
        },
        footer = {
            PrimaryModalButton(stringResource(R.string.plan_save), valuesValid && sharesValid) {
                    if (valuesValid && sharesValid) onSave(CaloriePlan(
                        trainingCarbsGrams = trainingValues[0]!!,
                        trainingProteinGrams = trainingValues[1]!!,
                        trainingFatGrams = trainingValues[2]!!,
                        restCarbsGrams = restValues[0]!!,
                        restProteinGrams = restValues[1]!!,
                        restFatGrams = restValues[2]!!,
                        trainingDays = trainingDays,
                        mealMacroShares = resolvedRestShares,
                        trainingMealMacroShares = resolvedTrainingShares,
                        dayModeOverrides = plan.dayModeOverrides
                    ))
            }
        }
    )
}

@Composable
internal fun MealDistributionEditor(
    period: MealPeriod,
    values: Triple<String, String, String>,
    onChange: (macroIndex: Int, value: String) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Raised), shape = AppControlShape, border = BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(period.label(), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MealGramField(stringResource(R.string.macro_carbs), values.first, { onChange(0, it) }, Modifier.weight(1f))
                MealGramField(stringResource(R.string.macro_protein_short), values.second, { onChange(1, it) }, Modifier.weight(1f))
                MealGramField(stringResource(R.string.macro_fat), values.third, { onChange(2, it) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun MealGramField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 10.sp, maxLines = 1) },
        suffix = { Text("g", fontSize = 10.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
internal fun MacroPlanEditor(
    title: String,
    calories: Double,
    carbs: String,
    onCarbsChange: (String) -> Unit,
    protein: String,
    onProteinChange: (String) -> Unit,
    fat: String,
    onFatChange: (String) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Raised), shape = AppControlShape, border = BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                Text("%.0f kcal".format(calories), color = Lime, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MacroTargetField(stringResource(R.string.macro_carbs), carbs, onCarbsChange, Modifier.weight(1f))
                MacroTargetField(stringResource(R.string.macro_protein_short), protein, onProteinChange, Modifier.weight(1f))
                MacroTargetField(stringResource(R.string.macro_fat), fat, onFatChange, Modifier.weight(1f))
            }
            Text(stringResource(R.string.macro_formula), color = Muted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun MacroTargetField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp, maxLines = 1) },
        suffix = { Text("g", fontSize = 10.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
internal fun WeekdayChip(
    day: DayOfWeek,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val locale = LocalAppLocale.current
    val label = remember(day, locale) { day.shortName(locale) }
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) Lime else Raised)
            .border(1.dp, if (selected) Lime else Outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 单元格仅约 43dp 宽，英文三字母缩写刚好容纳，超长时自动缩小而不是溢出。
        FitText(
            text = label,
            maxFontSize = 10.sp,
            minFontSize = 7.sp,
            color = if (selected) Background else Ink,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
internal fun MealDialog(
    customFoods: List<FoodReference>,
    hiddenQuickFoodNames: Set<String>,
    quickFoodOrder: List<String>,
    plan: CaloriePlan,
    initialPeriod: MealPeriod? = null,
    initialMeal: MealEntry? = null,
    onDismiss: () -> Unit,
    onSavePreset: (FoodReference) -> Unit,
    onHideQuickFood: (String) -> Unit,
    onReorderQuickFoods: (List<String>) -> Unit,
    onSave: (String, Double, MealPeriod, Double, Double, Double) -> Unit
) {
    val english = isEnglishUi()
    var name by remember(initialMeal) { mutableStateOf(initialMeal?.let { foodDisplayName(it.name, english) }.orEmpty()) }
    var carbs by remember(initialMeal) { mutableStateOf(initialMeal?.let { formatNutrition(it.carbohydratesGrams) }.orEmpty()) }
    var protein by remember(initialMeal) { mutableStateOf(initialMeal?.let { formatNutrition(it.proteinGrams) }.orEmpty()) }
    var fat by remember(initialMeal) { mutableStateOf(initialMeal?.let { formatNutrition(it.fatGrams) }.orEmpty()) }
    var portion by remember { mutableStateOf("100") }
    val today = LocalDate.now()
    val availablePeriods = plan.mealPeriodsFor(today)
    val selectablePeriods = remember(availablePeriods, initialMeal) {
        (availablePeriods + listOfNotNull(initialMeal?.period)).distinct()
    }
    val inferredPeriod = inferMealPeriod(Instant.now(), isTrainingDay = plan.modeFor(today) == DayMode.TRAINING)
    var period by remember(initialPeriod, initialMeal, selectablePeriods) {
        mutableStateOf(initialMeal?.period ?: initialPeriod?.takeIf { it in selectablePeriods }
            ?: inferredPeriod.takeIf { it in selectablePeriods } ?: selectablePeriods.first())
    }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<FoodReference>()) }
    var selectedFood by remember { mutableStateOf<FoodReference?>(null) }
    var searching by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf<String?>(null) }
    var presetMessage by remember { mutableStateOf<String?>(null) }
    val catalog = remember { FoodCatalogRepository() }
    val scope = rememberCoroutineScope()
    // 协程与 onClick 都不是 Composable 上下文，带占位符的文案先取好再进回调。
    val foundTemplate = stringResource(R.string.meal_search_found)
    val noneFoundText = stringResource(R.string.meal_search_none)
    val presetSavedText = stringResource(R.string.meal_preset_saved)
    val parsedCarbs = carbs.toDoubleOrNull() ?: 0.0
    val parsedProtein = protein.toDoubleOrNull() ?: 0.0
    val parsedFat = fat.toDoubleOrNull() ?: 0.0
    val calculatedCalories = caloriesFromMacros(parsedCarbs, parsedProtein, parsedFat)
    var quickFoods by remember(customFoods, hiddenQuickFoodNames, quickFoodOrder) {
        val order = quickFoodOrder.withIndex().associate { it.value to it.index }
        mutableStateOf((customFoods + catalog.quickFoods())
            .distinctBy { it.name.lowercase() }
            .filterNot { it.name.lowercase() in hiddenQuickFoodNames }
            .sortedBy { order[it.name.lowercase()] ?: Int.MAX_VALUE }
            .take(8)
        )
    }

    fun applyFood(food: FoodReference, grams: Double = portion.toDoubleOrNull() ?: 100.0) {
        val factor = grams.coerceAtLeast(0.0) / 100.0
        selectedFood = food
        name = foodDisplayName(food.name, english)
        carbs = formatNutrition(food.carbohydratesPer100g * factor)
        protein = formatNutrition(food.proteinPer100g * factor)
        fat = formatNutrition(food.fatPer100g * factor)
    }

    fun searchFoods() {
        if (query.isBlank() || searching) return
        scope.launch {
            searching = true
            searchMessage = null
            results = catalog.search(query.trim(), english)
            searching = false
            searchMessage = if (results.isEmpty()) noneFoundText else foundTemplate.format(results.size)
        }
    }

    AppModalSheet(
        title = stringResource(if (initialMeal == null) R.string.meal_dialog_add else R.string.meal_dialog_edit),
        subtitle = stringResource(if (initialMeal == null) R.string.meal_dialog_add_sub else R.string.meal_dialog_edit_sub),
        onDismiss = onDismiss,
        content = {
                MealSectionTitle(stringResource(R.string.meal_section_period), stringResource(R.string.meal_section_period_sub))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    selectablePeriods.forEach { option ->
                        val selected = period == option
                        Box(
                            modifier = Modifier.weight(1f).height(38.dp)
                                .background(if (selected) Lime else Raised, AppControlShape)
                                .border(1.dp, if (selected) Lime else Outline, AppControlShape)
                                .clickable { period = option }
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 五等分后每格约 60dp："Pre-workout" 靠缩小字号放进同一格，不改变分段控件尺寸。
                            FitText(
                                text = option.label(),
                                maxFontSize = 11.sp,
                                minFontSize = 8.sp,
                                color = if (selected) Background else Ink,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                MealSectionTitle(stringResource(R.string.meal_section_quick), stringResource(R.string.meal_section_quick_sub))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickFoods, key = { it.name.lowercase() }) { food ->
                        FoodPresetCard(
                            modifier = Modifier.animateItem(
                                placementSpec = spring(dampingRatio = 0.78f, stiffness = 460f)
                            ),
                            food = food,
                            displayName = foodDisplayName(food.name, english),
                            sourceLabel = sourceDisplayName(food.source, english),
                            selected = selectedFood?.name == food.name,
                            onClick = { applyFood(food) },
                            onRemove = { onHideQuickFood(food.name) },
                            onMove = { direction ->
                                val from = quickFoods.indexOfFirst { it.name.equals(food.name, ignoreCase = true) }
                                val to = (from + direction).coerceIn(0, quickFoods.lastIndex)
                                if (from >= 0 && from != to) {
                                    quickFoods = quickFoods.toMutableList().apply { add(to, removeAt(from)) }
                                    onReorderQuickFoods(quickFoods.map { it.name })
                                }
                            }
                        )
                    }
                }

                MealSectionTitle(stringResource(R.string.meal_section_search), stringResource(R.string.meal_section_search_sub))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            searchMessage = null
                        },
                        placeholder = { Text(stringResource(R.string.meal_search_hint), maxLines = 1) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { searchFoods() })
                    )
                    Button(
                        onClick = { searchFoods() },
                        enabled = query.isNotBlank() && !searching,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(13.dp)
                    ) {
                        if (searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        else Icon(Icons.Rounded.Search, stringResource(R.string.action_search), modifier = Modifier.size(19.dp))
                    }
                }
                searchMessage?.let { Text(it, color = if (results.isEmpty()) Muted else Lime, fontSize = 11.sp) }
                results.take(5).forEach { food ->
                    FoodPresetRow(
                        food = food,
                        displayName = foodDisplayName(food.name, english),
                        sourceLabel = sourceDisplayName(food.source, english),
                        selected = selectedFood?.name == food.name,
                        onClick = { applyFood(food) }
                    )
                }

                MealSectionTitle(
                    stringResource(R.string.meal_section_portion),
                    selectedFood?.let { stringResource(R.string.meal_portion_selected).format(foodDisplayName(it.name, english)) }
                        ?: stringResource(R.string.meal_portion_manual)
                )
                OutlinedTextField(
                    portion,
                    {
                        portion = it
                        val grams = it.toDoubleOrNull()
                        val food = selectedFood
                        if (grams != null && food != null) applyFood(food, grams)
                    },
                    label = { Text(stringResource(R.string.meal_portion_label), maxLines = 1) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text("g") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(50, 100, 150, 200).forEach { grams ->
                        FilterChip(
                            selected = portion.toDoubleOrNull() == grams.toDouble(),
                            onClick = {
                                portion = grams.toString()
                                selectedFood?.let { applyFood(it, grams.toDouble()) }
                            },
                            label = { Text("${grams}g", fontSize = 11.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    name,
                    { name = it; selectedFood = null },
                    label = { Text(stringResource(R.string.meal_food_name), maxLines = 1) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutritionField(stringResource(R.string.meal_carb_g), carbs, { carbs = it; selectedFood = null }, Modifier.weight(1f))
                    NutritionField(stringResource(R.string.meal_protein_g), protein, { protein = it; selectedFood = null }, Modifier.weight(1f))
                }
                NutritionField(stringResource(R.string.meal_fat_g), fat, { fat = it; selectedFood = null }, Modifier.fillMaxWidth())
                Card(colors = CardDefaults.cardColors(containerColor = Raised), shape = AppControlShape, border = BorderStroke(1.dp, CardOutline)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.meal_auto_kcal), color = Muted, fontSize = 11.sp, maxLines = 1)
                            Text(stringResource(R.string.macro_formula), color = Muted, fontSize = 10.sp, maxLines = 1)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("%.0f kcal".format(calculatedCalories), color = Lime, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
                TextButton(
                    onClick = {
                        val grams = portion.toDoubleOrNull() ?: return@TextButton
                        val factor = grams / 100.0
                        if (factor <= 0.0) return@TextButton
                        onSavePreset(FoodReference(
                            name = canonicalFoodName(name, english),
                            kilocaloriesPer100g = caloriesFromMacros(parsedCarbs / factor, parsedProtein / factor, parsedFat / factor),
                            carbohydratesPer100g = parsedCarbs / factor,
                            proteinPer100g = parsedProtein / factor,
                            fatPer100g = parsedFat / factor,
                            source = "自定义预设"
                        ))
                        presetMessage = presetSavedText
                    },
                    enabled = name.isNotBlank() && (portion.toDoubleOrNull() ?: 0.0) > 0.0 && calculatedCalories > 0.0,
                    modifier = Modifier.fillMaxWidth().background(Raised, AppControlShape)
                        .border(1.dp, Outline, AppControlShape),
                    colors = ButtonDefaults.textButtonColors(contentColor = Ink)
                ) { Text(stringResource(R.string.meal_save_preset), fontWeight = FontWeight.SemiBold, maxLines = 1) }
                presetMessage?.let { Text(it, color = Lime, fontSize = 11.sp) }
        },
        footer = {
            val saveLabel = stringResource(if (initialMeal == null) R.string.meal_save_new else R.string.meal_save_edit)
                .format(calculatedCalories)
            PrimaryModalButton(saveLabel, name.isNotBlank() && calculatedCalories > 0.0) {
                    if (calculatedCalories > 0.0) onSave(
                        canonicalFoodName(name, english), calculatedCalories, period,
                        parsedCarbs, parsedProtein, parsedFat
                    )
            }
        }
    )
}

@Composable
internal fun MealSectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(subtitle, color = Muted, fontSize = 10.sp)
    }
}

@Composable
internal fun FoodPresetCard(
    modifier: Modifier = Modifier,
    food: FoodReference,
    displayName: String,
    sourceLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMove: (direction: Int) -> Unit
) {
    val calories = caloriesFromMacros(food.carbohydratesPer100g, food.proteinPer100g, food.fatPer100g)
    val moveThreshold = with(LocalDensity.current) { 54.dp.toPx() }
    var dragDistance by remember(food.name) { mutableStateOf(0f) }
    var isDragging by remember(food.name) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isDragging) 1.07f else 1f, label = "quickFoodDragScale")
    Box(
        modifier.width(152.dp)
            .zIndex(if (isDragging) 2f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (isDragging) 16.dp else 0.dp, AppControlShape, ambientColor = Lime, spotColor = Lime)
            .border(if (isDragging) 2.dp else 0.dp, if (isDragging) Lime else Color.Transparent, AppControlShape)
            .pointerInput(food.name) {
            detectDragGesturesAfterLongPress(
                onDragStart = { isDragging = true; dragDistance = 0f },
                onDragEnd = { isDragging = false; dragDistance = 0f },
                onDragCancel = { isDragging = false; dragDistance = 0f },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragDistance += dragAmount.x
                    if (dragDistance >= moveThreshold) {
                        onMove(1)
                        dragDistance = 0f
                    } else if (dragDistance <= -moveThreshold) {
                        onMove(-1)
                        dragDistance = 0f
                    }
                }
            )
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = if (selected) Accent else Raised),
            border = BorderStroke(1.dp, if (selected) Lime else Outline),
            shape = AppControlShape
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                // 卡片宽度锁定 152dp：食物名固定占两行，卡片高度不会因中英文长短而参差。
                Text(
                    displayName,
                    color = if (selected) Lime else Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text("%.0f kcal / 100g".format(calories), color = Ink, fontSize = 11.sp, maxLines = 1)
                Text(sourceLabel, color = Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .background(Outline, CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.meal_remove_cd).format(displayName),
                tint = Ink,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
internal fun FoodPresetRow(
    food: FoodReference,
    displayName: String,
    sourceLabel: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val calories = caloriesFromMacros(
        food.carbohydratesPer100g,
        food.proteinPer100g,
        food.fatPer100g
    )
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) Accent else Raised, AppControlShape)
            .border(1.dp, if (selected) Lime else Outline, AppControlShape)
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(displayName, color = if (selected) Lime else Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.food_row_macros).format(
                    sourceLabel,
                    food.carbohydratesPer100g,
                    food.proteinPer100g,
                    food.fatPer100g
                ),
                color = Muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("%.0f kcal/100g".format(calories), color = Ink, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
internal fun NutritionField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp, maxLines = 1) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}
