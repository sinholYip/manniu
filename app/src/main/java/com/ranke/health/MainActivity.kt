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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val local = LocalStore(this)
        val dataBackup = DataBackup(this, local)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Lime,
                    onPrimary = Background,
                    background = Background,
                    onBackground = Ink,
                    surface = Surface,
                    onSurface = Ink,
                    surfaceVariant = Raised,
                    onSurfaceVariant = Muted,
                    secondaryContainer = Lime,
                    onSecondaryContainer = Background,
                    outline = Outline,
                    error = UpRed
                )
            ) {
                val scope = rememberCoroutineScope()
                val storeMutex = remember { Mutex() }
                var state by remember { mutableStateOf(DashboardState(loading = true)) }

                fun readState(): DashboardState {
                    val workoutDays = local.workoutDays()
                    return DashboardState(
                        weights = local.weights(),
                        meals = local.meals(),
                        customFoods = local.customFoods(),
                        hiddenQuickFoodNames = local.hiddenQuickFoodNames(),
                        quickFoodOrder = local.quickFoodOrder(),
                        dashboardSectionOrder = local.dashboardSectionOrder(),
                        caloriePlan = local.caloriePlan().copy(
                            dayModeOverrides = workoutDays.associate { it.date to it.mode }
                        ),
                        healthProfile = local.healthProfile(),
                        workoutDays = workoutDays,
                        exercisePrs = local.exercisePrs(),
                        trainingSettings = local.trainingSettings()
                    )
                }

                suspend fun refresh() {
                    storeMutex.withLock {
                        state = withContext(Dispatchers.IO) { readState() }
                    }
                }

                fun mutateStore(block: () -> Unit) {
                    scope.launch {
                        storeMutex.withLock {
                            state = withContext(Dispatchers.IO) {
                                block()
                                readState()
                            }
                        }
                    }
                }

                val saveDataLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) scope.launch {
                        runCatching {
                            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        }
                        val success = dataBackup.saveTo(uri)
                        if (success) local.saveBackupUri(uri.toString())
                        Toast.makeText(this@MainActivity, if (success) "数据已保存，之后将直接覆盖此文件" else "保存失败", Toast.LENGTH_SHORT).show()
                    }
                }

                val restoreDataLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) scope.launch {
                        val success = dataBackup.restoreFrom(uri)
                        Toast.makeText(this@MainActivity, if (success) "数据已恢复" else "数据文件无效", Toast.LENGTH_SHORT).show()
                        refresh()
                    }
                }

                LaunchedEffect(Unit) { refresh() }

                RankeApp(
                    state = state,
                    onAddWeight = { kg ->
                        mutateStore { local.addWeight(kg) }
                    },
                    onAddMeal = { name, kcal, period, carbs, protein, fat ->
                        mutateStore { local.addMeal(name, kcal, period, carbs, protein, fat) }
                    },
                    onUpdateMeal = { original, name, kcal, period, carbs, protein, fat ->
                        mutateStore { local.updateMeal(original, name, kcal, period, carbs, protein, fat) }
                    },
                    onSavePlan = { plan ->
                        mutateStore {
                            val previousTrainingDays = local.caloriePlan().trainingDays
                            local.saveCaloriePlan(plan)
                            if (plan.trainingDays != previousTrainingDays) {
                                local.saveWorkoutDays(local.workoutDays().map { day ->
                                    day.copy(mode = if (day.date.dayOfWeek in plan.trainingDays) DayMode.TRAINING else DayMode.REST)
                                })
                            }
                        }
                    },
                    onSaveProfile = { profile ->
                        mutateStore { local.saveHealthProfile(profile) }
                    },
                    onSaveWorkoutDays = { days ->
                        mutateStore { local.saveWorkoutDays(days) }
                    },
                    onAddExercisePr = { pr ->
                        mutateStore { local.addExercisePr(pr) }
                    },
                    onSaveTrainingSettings = { settings ->
                        mutateStore { local.saveTrainingSettings(settings) }
                    },
                    onSaveFoodPreset = { food ->
                        mutateStore { local.saveFoodPreset(food) }
                    },
                    onHideQuickFood = { name ->
                        mutateStore { local.hideQuickFood(name) }
                    },
                    onReorderQuickFoods = { names ->
                        mutateStore { local.saveQuickFoodOrder(names) }
                    },
                    onReorderDashboardSections = { names ->
                        mutateStore { local.saveDashboardSectionOrder(names) }
                    },
                    onSaveData = {
                        val savedUri = local.savedBackupUri()?.let(Uri::parse)
                        if (savedUri == null) {
                            saveDataLauncher.launch("man牛健康数据.json")
                        } else {
                            scope.launch {
                                val success = dataBackup.saveTo(savedUri)
                                if (success) {
                                    Toast.makeText(this@MainActivity, "已覆盖原数据文件", Toast.LENGTH_SHORT).show()
                                } else {
                                    local.clearBackupUri()
                                    Toast.makeText(this@MainActivity, "原文件不可用，请重新选择保存位置", Toast.LENGTH_SHORT).show()
                                    saveDataLauncher.launch("man牛健康数据.json")
                                }
                            }
                        }
                    },
                    onRestoreData = { restoreDataLauncher.launch(arrayOf("application/json", "text/plain")) }
                )
            }
        }
    }
}

@Composable
internal fun RankeApp(
    state: DashboardState,
    onAddWeight: (Double) -> Unit,
    onAddMeal: (String, Double, MealPeriod, Double, Double, Double) -> Unit,
    onUpdateMeal: (MealEntry, String, Double, MealPeriod, Double, Double, Double) -> Unit,
    onSavePlan: (CaloriePlan) -> Unit,
    onSaveProfile: (HealthProfile) -> Unit,
    onSaveWorkoutDays: (List<WorkoutDay>) -> Unit,
    onAddExercisePr: (ExercisePr) -> Unit,
    onSaveTrainingSettings: (TrainingSettings) -> Unit,
    onSaveFoodPreset: (FoodReference) -> Unit,
    onHideQuickFood: (String) -> Unit,
    onReorderQuickFoods: (List<String>) -> Unit,
    onReorderDashboardSections: (List<String>) -> Unit,
    onSaveData: () -> Unit,
    onRestoreData: () -> Unit
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    var editingMeal by remember { mutableStateOf<MealEntry?>(null) }
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var dashboardSections by remember(state.dashboardSectionOrder) {
        val saved = state.dashboardSectionOrder.mapNotNull { name ->
            runCatching { DashboardSection.valueOf(name) }.getOrNull()
        }
        mutableStateOf((saved + DashboardSection.entries).distinct())
    }
    fun openMeal(period: MealPeriod? = null) {
        dialog = period?.let { "meal:${it.name}" } ?: "meal"
    }
    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.HOME,
                    onClick = { selectedTab = AppTab.HOME },
                    icon = { Icon(Icons.Rounded.Home, "饮食计划") },
                    label = { Text("饮食计划") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Background, selectedTextColor = Lime, indicatorColor = Lime, unselectedIconColor = Muted, unselectedTextColor = Muted)
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.TRAINING,
                    onClick = { selectedTab = AppTab.TRAINING },
                    icon = { Icon(Icons.Rounded.FitnessCenter, "训练计划") },
                    label = { Text("训练计划") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Background, selectedTextColor = Lime, indicatorColor = Lime, unselectedIconColor = Muted, unselectedTextColor = Muted)
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.PROFILE,
                    onClick = { selectedTab = AppTab.PROFILE },
                    icon = { Icon(Icons.Rounded.Person, "我") },
                    label = { Text("我") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Background, selectedTextColor = Lime, indicatorColor = Lime, unselectedIconColor = Muted, unselectedTextColor = Muted)
                )
            }
        }
    ) { padding ->
        if (selectedTab == AppTab.PROFILE) {
            ProfileScreen(state.healthProfile, onSaveProfile, onSaveData, onRestoreData, Modifier.padding(padding))
            return@Scaffold
        }
        if (selectedTab == AppTab.TRAINING) {
            TrainingPlanScreen(
                savedDays = state.workoutDays,
                trainingDays = state.caloriePlan.trainingDays,
                onSaveDays = onSaveWorkoutDays,
                exercisePrs = state.exercisePrs,
                onAddExercisePr = onAddExercisePr,
                trainingSettings = state.trainingSettings,
                onSaveTrainingSettings = onSaveTrainingSettings,
                onSaveTrainingDays = { days -> onSavePlan(state.caloriePlan.copy(trainingDays = days)) },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = AppPagePadding,
            verticalArrangement = Arrangement.spacedBy(AppSectionGap)
        ) {
            item { Header(state.caloriePlan) }
            state.message?.let { message ->
                item { Text(message, color = UpRed, fontSize = 13.sp) }
            }
            items(dashboardSections, key = { it.name }) { section ->
                DraggableDashboardBlock(
                    modifier = Modifier.animateItem(
                        placementSpec = spring(dampingRatio = 0.78f, stiffness = 420f)
                    ),
                    section = section,
                    onMove = { direction ->
                        val from = dashboardSections.indexOf(section)
                        val to = (from + direction).coerceIn(0, dashboardSections.lastIndex)
                        if (from >= 0 && from != to) {
                            dashboardSections = dashboardSections.toMutableList().apply { add(to, removeAt(from)) }
                            onReorderDashboardSections(dashboardSections.map { it.name })
                        }
                    }
                ) {
                    when (section) {
                        DashboardSection.NUTRITION -> TodayNutritionHero(state.meals, state.caloriePlan)
                        DashboardSection.WEIGHT_TREND -> WeightChartCard(
                            entries = state.weights,
                            meals = state.meals,
                            isDemo = state.weights.isEmpty(),
                            onAdd = { dialog = "weight" }
                        )
                        DashboardSection.MEALS -> CalorieCard(
                            meals = state.meals,
                            plan = state.caloriePlan,
                            onAdd = { openMeal() },
                            onPeriodClick = { openMeal(it) },
                            onEditPlan = { dialog = "plan" }
                        )
                        DashboardSection.TIP -> DailyTipCard(state.meals, state.caloriePlan)
                    }
                }
            }
            item {
                Button(
                    onClick = { openMeal() },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Background),
                    shape = AppControlShape
                ) {
                    Icon(Icons.Rounded.Restaurant, null)
                    Spacer(Modifier.width(9.dp))
                    Text("记录饮食", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (state.meals.isNotEmpty()) {
                item { Text("最近饮食", color = Ink, fontSize = AppSectionTitleSize, fontWeight = FontWeight.Bold) }
                items(state.meals.take(5), key = { it.time }) { meal ->
                    MealRow(meal, onClick = {
                        editingMeal = meal
                        dialog = "meal-edit"
                    })
                }
            }
        }
    }

    if (dialog == "weight") {
        WeightDialog(entries = state.weights, onDismiss = { dialog = null }) {
            onAddWeight(it)
            dialog = null
        }
    }
    if (dialog?.startsWith("meal") == true) {
        val initialPeriod = dialog?.substringAfter(':', "")?.takeIf { it.isNotBlank() }
            ?.let { runCatching { MealPeriod.valueOf(it) }.getOrNull() }
        MealDialog(
            customFoods = state.customFoods,
            hiddenQuickFoodNames = state.hiddenQuickFoodNames,
            quickFoodOrder = state.quickFoodOrder,
            plan = state.caloriePlan,
            initialPeriod = initialPeriod,
            initialMeal = editingMeal,
            onDismiss = { dialog = null; editingMeal = null },
            onSavePreset = onSaveFoodPreset,
            onHideQuickFood = onHideQuickFood,
            onReorderQuickFoods = onReorderQuickFoods
        ) { name, kcal, period, carbs, protein, fat ->
                editingMeal?.let { onUpdateMeal(it, name, kcal, period, carbs, protein, fat) }
                    ?: onAddMeal(name, kcal, period, carbs, protein, fat)
                dialog = null
                editingMeal = null
            }
    }
    if (dialog == "plan") {
        CaloriePlanDialog(state.caloriePlan, onDismiss = { dialog = null }) { plan ->
            onSavePlan(plan)
            dialog = null
        }
    }
}

