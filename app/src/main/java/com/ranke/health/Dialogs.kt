package com.ranke.health

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                            Text(title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            subtitle?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, color = Muted, fontSize = 12.sp)
                            }
                        }
                        Box(Modifier.size(38.dp).background(Raised, CircleShape).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, "关闭", tint = Muted, modifier = Modifier.size(19.dp))
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
    ) { Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
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
        title = "每日营养计划",
        subtitle = "训练日与休息日独立设置",
        onDismiss = onDismiss,
        content = {
            Row(
                Modifier.fillMaxWidth().background(Raised, AppControlShape)
                    .border(1.dp, Outline, AppControlShape).padding(4.dp)
            ) {
                listOf("每日目标", "餐次分配").forEachIndexed { index, label ->
                    Box(
                        Modifier.weight(1f).background(if (page == index) Lime else Color.Transparent, AppControlShape)
                            .clickable { page = index }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = if (page == index) Background else Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
            if (page == 0) {
                Text("输入碳蛋脂目标，热量会自动计算。", color = Muted, fontSize = 11.sp)
                MacroPlanEditor(
                    title = "训练日", calories = trainingCalories,
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
                    title = "休息日", calories = restCalories,
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
                Text("选择每周训练日", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                Text("已手动填写的餐次会锁定，剩余克数继续向后分配。", color = Muted, fontSize = 11.sp)
                Text("训练日 · 含练前餐", color = Lime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                if (!trainingSharesValid) Text("训练日分配需为有效克数并保持自动平衡", color = UpRed, fontSize = 10.sp)
                Text("休息日 · 无练前餐", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                if (!restSharesValid) Text("休息日分配需为有效克数并保持自动平衡", color = UpRed, fontSize = 10.sp)
            }
        },
        footer = {
            PrimaryModalButton("保存计划", valuesValid && sharesValid) {
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
            Text(period.label, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MealGramField("碳水", values.first, { onChange(0, it) }, Modifier.weight(1f))
                MealGramField("蛋白", values.second, { onChange(1, it) }, Modifier.weight(1f))
                MealGramField("脂肪", values.third, { onChange(2, it) }, Modifier.weight(1f))
            }
        }
    }
}


@Composable
internal fun MealGramField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 10.sp) },
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
                Text(title, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("%.0f kcal".format(calories), color = Lime, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MacroTargetField("碳水", carbs, onCarbsChange, Modifier.weight(1f))
                MacroTargetField("蛋白", protein, onProteinChange, Modifier.weight(1f))
                MacroTargetField("脂肪", fat, onFatChange, Modifier.weight(1f))
            }
            Text("碳水×4 + 蛋白质×4 + 脂肪×9", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
internal fun MacroTargetField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp) },
        suffix = { Text("g", fontSize = 10.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

internal fun DayOfWeek.shortChinese(): String = when (this) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}

@Composable
internal fun WeekdayChip(
    day: DayOfWeek,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) Lime else Raised)
            .border(1.dp, if (selected) Lime else Outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.shortChinese(),
            color = if (selected) Background else Ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
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
    var name by remember(initialMeal) { mutableStateOf(initialMeal?.name.orEmpty()) }
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
        name = food.name
        carbs = formatNutrition(food.carbohydratesPer100g * factor)
        protein = formatNutrition(food.proteinPer100g * factor)
        fat = formatNutrition(food.fatPer100g * factor)
    }

    fun searchFoods() {
        if (query.isBlank() || searching) return
        scope.launch {
            searching = true
            searchMessage = null
            results = catalog.search(query.trim())
            searching = false
            searchMessage = if (results.isEmpty()) "没有找到，也可以直接在下方手动填写" else "找到 ${results.size} 个结果"
        }
    }

    AppModalSheet(
        title = if (initialMeal == null) "记录饮食" else "修改饮食",
        subtitle = if (initialMeal == null) "选食物、调份量，热量会自动计算" else "修改后热量与营养汇总会同步更新",
        onDismiss = onDismiss,
        content = {
                MealSectionTitle("用餐时段", "已根据当前时间自动选择")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    selectablePeriods.forEach { option ->
                        val selected = period == option
                        Box(
                            modifier = Modifier.weight(1f).height(38.dp)
                                .background(if (selected) Lime else Raised, AppControlShape)
                                .border(1.dp, if (selected) Lime else Outline, AppControlShape)
                                .clickable { period = option },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                option.label,
                                color = if (selected) Background else Ink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                MealSectionTitle("快速选择", "自定义与常用食物")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickFoods, key = { it.name.lowercase() }) { food ->
                        FoodPresetCard(
                            modifier = Modifier.animateItem(
                                placementSpec = spring(dampingRatio = 0.78f, stiffness = 460f)
                            ),
                            food = food,
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

                MealSectionTitle("搜索食物", "同时搜索本地参考值与 Open Food Facts")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            searchMessage = null
                        },
                        placeholder = { Text("例如：米饭、牛奶") },
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
                        else Icon(Icons.Rounded.Search, "搜索", modifier = Modifier.size(19.dp))
                    }
                }
                searchMessage?.let { Text(it, color = if (results.isEmpty()) Muted else Lime, fontSize = 11.sp) }
                results.take(5).forEach { food ->
                    FoodPresetRow(food = food, selected = selectedFood?.name == food.name, onClick = { applyFood(food) })
                }

                MealSectionTitle("份量与营养", selectedFood?.let { "已选 ${it.name}，改份量会自动换算" } ?: "也可以直接手动录入")
                OutlinedTextField(
                    portion,
                    {
                        portion = it
                        val grams = it.toDoubleOrNull()
                        val food = selectedFood
                        if (grams != null && food != null) applyFood(food, grams)
                    },
                    label = { Text("实际食用量（克）") },
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
                            label = { Text("${grams}g", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    name,
                    { name = it; selectedFood = null },
                    label = { Text("食物名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutritionField("碳水 (g)", carbs, { carbs = it; selectedFood = null }, Modifier.weight(1f))
                    NutritionField("蛋白质 (g)", protein, { protein = it; selectedFood = null }, Modifier.weight(1f))
                }
                NutritionField("脂肪 (g)", fat, { fat = it; selectedFood = null }, Modifier.fillMaxWidth())
                Card(colors = CardDefaults.cardColors(containerColor = Raised), shape = AppControlShape, border = BorderStroke(1.dp, CardOutline)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("自动计算热量", color = Muted, fontSize = 11.sp)
                            Text("碳水×4 + 蛋白质×4 + 脂肪×9", color = Muted, fontSize = 10.sp)
                        }
                        Text("%.0f kcal".format(calculatedCalories), color = Lime, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(
                    onClick = {
                        val grams = portion.toDoubleOrNull() ?: return@TextButton
                        val factor = grams / 100.0
                        if (factor <= 0.0) return@TextButton
                        onSavePreset(FoodReference(
                            name = name.trim(),
                            kilocaloriesPer100g = caloriesFromMacros(parsedCarbs / factor, parsedProtein / factor, parsedFat / factor),
                            carbohydratesPer100g = parsedCarbs / factor,
                            proteinPer100g = parsedProtein / factor,
                            fatPer100g = parsedFat / factor,
                            source = "自定义预设"
                        ))
                        presetMessage = "已保存为每 100 克食物预设"
                    },
                    enabled = name.isNotBlank() && (portion.toDoubleOrNull() ?: 0.0) > 0.0 && calculatedCalories > 0.0,
                    modifier = Modifier.fillMaxWidth().background(Raised, AppControlShape)
                        .border(1.dp, Outline, AppControlShape),
                    colors = ButtonDefaults.textButtonColors(contentColor = Ink)
                ) { Text("保存为常用食物", fontWeight = FontWeight.SemiBold) }
                presetMessage?.let { Text(it, color = Lime, fontSize = 11.sp) }
        },
        footer = {
            PrimaryModalButton(
                (if (initialMeal == null) "记录" else "保存修改") + " %.0f kcal".format(calculatedCalories),
                name.isNotBlank() && calculatedCalories > 0.0
            ) {
                    if (calculatedCalories > 0.0) onSave(
                        name.trim(), calculatedCalories, period,
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
                Text(food.name, color = if (selected) Lime else Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("%.0f kcal / 100g".format(calories), color = Ink, fontSize = 11.sp)
                Text(food.source, color = Muted, fontSize = 9.sp, maxLines = 1)
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
            Icon(Icons.Rounded.Close, contentDescription = "从快速选择移除${food.name}", tint = Ink, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
internal fun FoodPresetRow(food: FoodReference, selected: Boolean = false, onClick: () -> Unit) {
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
            Text(food.name, color = if (selected) Lime else Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${food.source} · 碳 %.1f / 蛋 %.1f / 脂 %.1fg".format(
                food.carbohydratesPer100g,
                food.proteinPer100g,
                food.fatPer100g
            ), color = Muted, fontSize = 10.sp)
        }
        Text("%.0f kcal/100g".format(calories), color = Ink, fontSize = 11.sp)
    }
}

@Composable
internal fun NutritionField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

