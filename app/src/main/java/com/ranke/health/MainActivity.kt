package com.ranke.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranke.health.data.DataBackup
import com.ranke.health.data.LocalStore
import com.ranke.health.model.CaloriePlan
import com.ranke.health.model.DashboardState
import com.ranke.health.model.DayMode
import com.ranke.health.model.ExercisePr
import com.ranke.health.model.FoodReference
import com.ranke.health.model.HealthProfile
import com.ranke.health.model.MealEntry
import com.ranke.health.model.MealPeriod
import com.ranke.health.model.TrainingSettings
import com.ranke.health.model.WorkoutDay
import com.ranke.health.ui.components.CaloriePlanDialog
import com.ranke.health.ui.components.MealDialog
import com.ranke.health.ui.i18n.AppLanguage
import com.ranke.health.ui.i18n.LocalAppLocale
import com.ranke.health.ui.i18n.LocaleSettings
import com.ranke.health.ui.screen.CalorieCard
import com.ranke.health.ui.screen.DailyTipCard
import com.ranke.health.ui.screen.DraggableDashboardBlock
import com.ranke.health.ui.screen.Header
import com.ranke.health.ui.screen.MealRow
import com.ranke.health.ui.screen.ProfileScreen
import com.ranke.health.ui.screen.TodayNutritionHero
import com.ranke.health.ui.screen.TrainingPlanScreen
import com.ranke.health.ui.screen.WeightChartCard
import com.ranke.health.ui.screen.WeightDialog
import com.ranke.health.ui.theme.AppControlShape
import com.ranke.health.ui.theme.AppPagePadding
import com.ranke.health.ui.theme.AppSectionGap
import com.ranke.health.ui.theme.AppSectionTitleSize
import com.ranke.health.ui.theme.AppTab
import com.ranke.health.ui.theme.Background
import com.ranke.health.ui.theme.DashboardSection
import com.ranke.health.ui.theme.Ink
import com.ranke.health.ui.theme.Lime
import com.ranke.health.ui.theme.Muted
import com.ranke.health.ui.theme.Outline
import com.ranke.health.ui.theme.Raised
import com.ranke.health.ui.theme.Surface
import com.ranke.health.ui.theme.UpRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // 语言偏好在任何资源加载之前套用，重建后整个应用（含 Compose 资源）即为对应语言。
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleSettings.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val local = LocalStore(this)
        val dataBackup = DataBackup(this, local)
        setContent {
            val appLocale = remember { this@MainActivity.resources.configuration.locales[0] }
            CompositionLocalProvider(LocalAppLocale provides appLocale) {
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
                    var language by remember { mutableStateOf(LocaleSettings.language(this@MainActivity)) }

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
                            val message = if (success) R.string.toast_data_saved else R.string.toast_save_failed
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    val restoreDataLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        if (uri != null) scope.launch {
                            val success = dataBackup.restoreFrom(uri)
                            val message = if (success) R.string.toast_data_restored else R.string.toast_invalid_file
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                            refresh()
                        }
                    }

                    LaunchedEffect(Unit) { refresh() }

                    RankeApp(
                        state = state,
                        language = language,
                        onLanguageChange = { selected ->
                            language = selected
                            LocaleSettings.setLanguage(this@MainActivity, selected)
                            // 资源随配置变化，重建 Activity 让全应用立即换语言。
                            this@MainActivity.recreate()
                        },
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
                            val backupFileName = getString(R.string.backup_file_name)
                            val savedUri = local.savedBackupUri()?.let(Uri::parse)
                            if (savedUri == null) {
                                saveDataLauncher.launch(backupFileName)
                            } else {
                                scope.launch {
                                    val success = dataBackup.saveTo(savedUri)
                                    if (success) {
                                        Toast.makeText(this@MainActivity, R.string.toast_overwritten, Toast.LENGTH_SHORT).show()
                                    } else {
                                        local.clearBackupUri()
                                        Toast.makeText(this@MainActivity, R.string.toast_file_unavailable, Toast.LENGTH_SHORT).show()
                                        saveDataLauncher.launch(backupFileName)
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
}

@Composable
internal fun RankeApp(
    state: DashboardState,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
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
                val nutritionLabel = stringResource(R.string.nav_nutrition)
                val trainingLabel = stringResource(R.string.nav_training)
                val profileLabel = stringResource(R.string.nav_profile)
                NavigationBarItem(
                    selected = selectedTab == AppTab.HOME,
                    onClick = { selectedTab = AppTab.HOME },
                    icon = { Icon(Icons.Rounded.Home, nutritionLabel) },
                    label = { Text(nutritionLabel, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Background, selectedTextColor = Lime, indicatorColor = Lime, unselectedIconColor = Muted, unselectedTextColor = Muted)
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.TRAINING,
                    onClick = { selectedTab = AppTab.TRAINING },
                    icon = { Icon(Icons.Rounded.FitnessCenter, trainingLabel) },
                    label = { Text(trainingLabel, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Background, selectedTextColor = Lime, indicatorColor = Lime, unselectedIconColor = Muted, unselectedTextColor = Muted)
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.PROFILE,
                    onClick = { selectedTab = AppTab.PROFILE },
                    icon = { Icon(Icons.Rounded.Person, profileLabel) },
                    label = { Text(profileLabel, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Background, selectedTextColor = Lime, indicatorColor = Lime, unselectedIconColor = Muted, unselectedTextColor = Muted)
                )
            }
        }
    ) { padding ->
        if (selectedTab == AppTab.PROFILE) {
            ProfileScreen(
                profile = state.healthProfile,
                language = language,
                onLanguageChange = onLanguageChange,
                onSave = onSaveProfile,
                onSaveData = onSaveData,
                onRestoreData = onRestoreData,
                modifier = Modifier.padding(padding)
            )
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
                    Text(stringResource(R.string.log_food), fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            if (state.meals.isNotEmpty()) {
                item { Text(stringResource(R.string.recent_meals), color = Ink, fontSize = AppSectionTitleSize, fontWeight = FontWeight.Bold) }
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
