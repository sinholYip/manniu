package com.ranke.health.ui.screen


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ranke.health.R
import com.ranke.health.model.DayMode
import com.ranke.health.model.ExercisePr
import com.ranke.health.model.MuscleRegion
import com.ranke.health.model.TrainingSettings
import com.ranke.health.model.WorkoutDay
import com.ranke.health.model.WorkoutExercise
import com.ranke.health.model.WorkoutFocus
import com.ranke.health.model.WorkoutStatistics
import com.ranke.health.model.buildWorkoutCycle
import com.ranke.health.model.calculateWorkoutStatistics
import com.ranke.health.model.commonWorkoutExerciseNames
import com.ranke.health.model.estimateWorkoutMinutes
import com.ranke.health.model.recommendedWorkoutExerciseNames
import com.ranke.health.model.spreadsheetWorkoutExerciseNames
import com.ranke.health.model.summarizeWorkoutFocus
import com.ranke.health.model.workoutExerciseMatches
import com.ranke.health.ui.components.AppModalSheet
import com.ranke.health.ui.components.PrimaryModalButton
import com.ranke.health.ui.components.WeekdayChip
import com.ranke.health.ui.components.shortChinese
import com.ranke.health.ui.theme.AppBodySize
import com.ranke.health.ui.theme.AppCaptionSize
import com.ranke.health.ui.theme.AppCardPadding
import com.ranke.health.ui.theme.AppCardShape
import com.ranke.health.ui.theme.AppControlShape
import com.ranke.health.ui.theme.AppPagePadding
import com.ranke.health.ui.theme.AppPageTitleSize
import com.ranke.health.ui.theme.AppSectionGap
import com.ranke.health.ui.theme.AppSectionTitleSize
import com.ranke.health.ui.theme.Background
import com.ranke.health.ui.theme.CardOutline
import com.ranke.health.ui.theme.Ink
import com.ranke.health.ui.theme.Lime
import com.ranke.health.ui.theme.MonthDayFormatter
import com.ranke.health.ui.theme.Muted
import com.ranke.health.ui.theme.Outline
import com.ranke.health.ui.theme.PrDateFormatter
import com.ranke.health.ui.theme.Raised
import com.ranke.health.ui.theme.Surface
import com.ranke.health.ui.theme.UpRed
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun TrainingPlanScreen(
    savedDays: List<WorkoutDay>,
    trainingDays: Set<DayOfWeek>,
    onSaveDays: (List<WorkoutDay>) -> Unit,
    exercisePrs: List<ExercisePr>,
    onAddExercisePr: (ExercisePr) -> Unit,
    trainingSettings: TrainingSettings,
    onSaveTrainingSettings: (TrainingSettings) -> Unit,
    onSaveTrainingDays: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val initialMonday = remember(today) { today.minusDays((today.dayOfWeek.value - 1).toLong()) }
    var weekStart by remember { mutableStateOf(initialMonday) }
    var cycleStart by remember { mutableStateOf(initialMonday) }
    var selectedCycleWeek by remember { mutableStateOf(1) }
    var selectedDate by remember { mutableStateOf(today) }
    val weekDates = (0L..6L).map(weekStart::plusDays)
    fun dayFor(date: LocalDate): WorkoutDay = savedDays.firstOrNull { it.date == date }
        ?: WorkoutDay(date = date, mode = if (date.dayOfWeek in trainingDays) DayMode.TRAINING else DayMode.REST)
    val selectedDay = dayFor(selectedDate)
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<WorkoutExercise?>(null) }
    var addingExercise by remember { mutableStateOf(false) }
    var addingPr by remember { mutableStateOf(false) }
    var cycleExpanded by remember { mutableStateOf(false) }
    var cycleTrainingDays by remember(trainingDays) { mutableStateOf(trainingDays) }
    var cycleWeeksInput by remember(trainingSettings) { mutableStateOf(trainingSettings.cycleWeeks.toString()) }
    var deloadWeekInput by remember(trainingSettings) { mutableStateOf(trainingSettings.deloadWeek.toString()) }
    val cycleWeeks = cycleWeeksInput.toIntOrNull()
    val deloadWeek = deloadWeekInput.toIntOrNull()
    val cycleSettingsValid = cycleWeeks?.let { it in 1..12 } == true && deloadWeek?.let { it in 1..cycleWeeks!! } == true
    val workoutFocus = summarizeWorkoutFocus(selectedDay.exercises)
    val workoutStatistics = calculateWorkoutStatistics(savedDays)

    fun saveDay(updated: WorkoutDay) {
        onSaveDays((savedDays.filterNot { it.date == updated.date } + updated).sortedBy { it.date })
    }

    fun updateCycleWeeks(value: String) {
        val filtered = value.filter(Char::isDigit).take(2)
        cycleWeeksInput = filtered
        val weeks = filtered.toIntOrNull()?.takeIf { it in 1..12 } ?: return
        val adjustedDeload = (deloadWeekInput.toIntOrNull() ?: weeks).coerceIn(1, weeks)
        if (deloadWeekInput != adjustedDeload.toString()) deloadWeekInput = adjustedDeload.toString()
        onSaveTrainingSettings(TrainingSettings(weeks, adjustedDeload))
    }

    fun updateDeloadWeek(value: String) {
        val filtered = value.filter(Char::isDigit).take(2)
        deloadWeekInput = filtered
        val weeks = cycleWeeksInput.toIntOrNull()?.takeIf { it in 1..12 } ?: return
        val deload = filtered.toIntOrNull()?.takeIf { it in 1..weeks } ?: return
        onSaveTrainingSettings(TrainingSettings(weeks, deload))
    }

    LaunchedEffect(today) {
        // 每天首次进入训练计划时同步到当天所在周，并默认选中今天（包括休息日）。
        weekStart = initialMonday
        cycleStart = initialMonday
        selectedCycleWeek = 1
        selectedDate = today
    }

    LaunchedEffect(cycleWeeks, deloadWeek, cycleTrainingDays, cycleStart) {
        if (cycleSettingsValid && cycleTrainingDays.isNotEmpty()) {
            val generated = buildWorkoutCycle(cycleStart, cycleTrainingDays, savedDays, cycleWeeks!!, deloadWeek!!)
            val end = generated.last().date
            onSaveDays((savedDays.filter { it.date < cycleStart || it.date > end } + generated).sortedBy { it.date })
            if (selectedCycleWeek > cycleWeeks) selectedCycleWeek = cycleWeeks
            weekStart = cycleStart.plusWeeks((selectedCycleWeek - 1).toLong())
        }
    }

    LaunchedEffect(selectedDate, selectedDay.mode) {
        if (selectedDay.mode == DayMode.REST) {
            addingExercise = false
            editing = null
        }
    }

    val listState = rememberLazyListState()
    val canScroll by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo
            if (info.totalItemsCount == 0 || visible.isEmpty()) false
            else visible.first().index > 0 || visible.first().offset < info.viewportStartOffset ||
                visible.last().index < info.totalItemsCount - 1 ||
                visible.last().offset + visible.last().size > info.viewportEndOffset
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = AppPagePadding,
        verticalArrangement = Arrangement.spacedBy(AppSectionGap),
        userScrollEnabled = canScroll
    ) {
        item {
            Text("训练计划", color = Ink, fontSize = AppPageTitleSize, fontWeight = FontWeight.Black)
            Text("今天练什么，一眼就知道", color = Muted, fontSize = AppBodySize)
        }
        item {
            TrainingFocusHero(
                date = selectedDate,
                day = selectedDay,
                focus = workoutFocus
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
                Column(Modifier.padding(AppCardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("第 ${selectedDay.cycleWeek ?: selectedCycleWeek} 周", color = Ink, fontSize = AppSectionTitleSize, fontWeight = FontWeight.Bold)
                        Text(" / ${cycleWeeks ?: 4} 周", color = Muted, fontSize = AppBodySize)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { cycleExpanded = !cycleExpanded }) {
                            Text(if (cycleExpanded) "收起设置" else "周期设置", color = Lime, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.Tune, null, tint = Lime, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (cycleExpanded) {
                        Text("设置周期长度与减载周；减载周动作组数降至约 60%", color = Muted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(cycleWeeksInput, ::updateCycleWeeks, label = { Text("周期周数") }, suffix = { Text("周") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(deloadWeekInput, ::updateDeloadWeek, label = { Text("减载周") }, prefix = { Text("第 ") }, suffix = { Text("周") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        if (!cycleSettingsValid) Text("周期为 1–12 周，减载周需位于周期内", color = UpRed, fontSize = 10.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DayOfWeek.entries.forEach { day ->
                                WeekdayChip(
                                    day = day,
                                    selected = day in cycleTrainingDays,
                                    onClick = {
                                        val updated = if (day in cycleTrainingDays) cycleTrainingDays - day else cycleTrainingDays + day
                                        cycleTrainingDays = updated
                                        onSaveTrainingDays(updated)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (cycleSettingsValid) {
                            Text("选择要编辑的周", color = Muted, fontSize = 11.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..cycleWeeks!!).forEach { week ->
                                    FilterChip(
                                        selected = week == selectedCycleWeek,
                                        onClick = {
                                            selectedCycleWeek = week
                                            val next = cycleStart.plusWeeks((week - 1).toLong())
                                            weekStart = next
                                            selectedDate = (0L..6L).map(next::plusDays).firstOrNull { it.dayOfWeek in cycleTrainingDays } ?: next
                                        },
                                        label = { Text("$week", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    val next = weekStart.minusWeeks(1)
                    weekStart = next
                    selectedCycleWeek = (((next.toEpochDay() - cycleStart.toEpochDay()) / 7).toInt() + 1).coerceIn(1, cycleWeeks ?: 4)
                    selectedDate = (0L..6L).map(next::plusDays).firstOrNull { dayFor(it).mode == DayMode.TRAINING } ?: next
                }) { Text("‹ 上周") }
                Text(
                    "${weekStart.format(MonthDayFormatter)} - ${weekStart.plusDays(6).format(MonthDayFormatter)}",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                TextButton(onClick = {
                    val next = weekStart.plusWeeks(1)
                    weekStart = next
                    selectedCycleWeek = (((next.toEpochDay() - cycleStart.toEpochDay()) / 7).toInt() + 1).coerceIn(1, cycleWeeks ?: 4)
                    selectedDate = (0L..6L).map(next::plusDays).firstOrNull { dayFor(it).mode == DayMode.TRAINING } ?: next
                }) { Text("下周 ›") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                weekDates.forEach { date ->
                    val day = dayFor(date)
                    Card(
                        modifier = Modifier.weight(1f)
                            .clickable { selectedDate = date },
                        colors = CardDefaults.cardColors(containerColor = if (date == selectedDate) Lime else Surface),
                        shape = AppControlShape,
                        border = BorderStroke(1.dp, if (date == selectedDate) Lime else Outline)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(date.dayOfWeek.shortChinese().removePrefix("周"), color = if (date == selectedDate) Background else Muted, fontSize = 10.sp)
                            Text(date.dayOfMonth.toString(), color = if (date == selectedDate) Background else Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Text(
                                when {
                                    day.isCompleted -> "✓"
                                    day.mode == DayMode.TRAINING -> "练"
                                    else -> "休"
                                },
                                color = if (date == selectedDate) Background else if (day.isCompleted || day.mode == DayMode.TRAINING) Lime else Muted,
                                fontSize = 10.sp,
                                fontWeight = if (day.isCompleted) FontWeight.Black else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
        if (selectedDay.mode == DayMode.TRAINING) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("动作安排", color = Ink, fontSize = AppSectionTitleSize, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { addingExercise = true }) {
                                Text("添加动作", color = Lime, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Rounded.Add, null, tint = Lime, modifier = Modifier.size(18.dp))
                            }
                        }
                        selectedDay.exercises.forEach { exercise ->
                            val swipeOffset = remember(exercise.id) { Animatable(0f) }
                            val deleteThreshold = with(LocalDensity.current) { 88.dp.toPx() }
                            val moveThreshold = with(LocalDensity.current) { 54.dp.toPx() }
                            val maxSwipe = deleteThreshold * 1.6f
                            var verticalDistance by remember(exercise.id) { mutableStateOf(0f) }
                            var horizontalDistance by remember(exercise.id) { mutableStateOf(0f) }
                            var isDragging by remember(exercise.id) { mutableStateOf(false) }
                            val dragScale by animateFloatAsState(if (isDragging) 1.018f else 1f, label = "exerciseDragScale")
                            fun moveExercise(direction: Int) {
                                val currentIndex = selectedDay.exercises.indexOfFirst { it.id == exercise.id }
                                val targetIndex = (currentIndex + direction).coerceIn(0, selectedDay.exercises.lastIndex)
                                if (currentIndex >= 0 && targetIndex != currentIndex) {
                                    val reordered = selectedDay.exercises.toMutableList()
                                    val moved = reordered.removeAt(currentIndex)
                                    reordered.add(targetIndex, moved)
                                    saveDay(selectedDay.copy(exercises = reordered))
                                }
                            }
                            Box(
                                Modifier.fillMaxWidth()
                                    .zIndex(if (isDragging) 2f else 0f)
                                    .graphicsLayer { scaleX = dragScale; scaleY = dragScale }
                                    .shadow(if (isDragging) 12.dp else 0.dp, AppControlShape, ambientColor = Lime, spotColor = Lime)
                            ) {
                                if (swipeOffset.value > 0f) {
                                    Box(
                                        Modifier.matchParentSize()
                                            .background(
                                                Brush.horizontalGradient(0f to UpRed, 1f to UpRed.copy(alpha = .25f)),
                                                AppControlShape
                                            )
                                            .padding(start = 18.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text("删除", color = Background, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Row(
                                    Modifier.fillMaxWidth()
                                        .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
                                        .background(Surface)
                                        .pointerInput(exercise.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    isDragging = true
                                                    verticalDistance = 0f
                                                    horizontalDistance = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    verticalDistance += dragAmount.y
                                                    horizontalDistance += dragAmount.x
                                                    if (kotlin.math.abs(verticalDistance) > kotlin.math.abs(horizontalDistance)) {
                                                        change.consume()
                                                        if (verticalDistance >= moveThreshold) {
                                                            moveExercise(1)
                                                            verticalDistance = 0f
                                                            horizontalDistance = 0f
                                                        } else if (verticalDistance <= -moveThreshold) {
                                                            moveExercise(-1)
                                                            verticalDistance = 0f
                                                            horizontalDistance = 0f
                                                        }
                                                    } else if (horizontalDistance > 0f) {
                                                        change.consume()
                                                        scope.launch {
                                                            swipeOffset.snapTo(horizontalDistance.coerceIn(0f, maxSwipe))
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    isDragging = false
                                                    scope.launch {
                                                        if (horizontalDistance > kotlin.math.abs(verticalDistance) && swipeOffset.value >= deleteThreshold) {
                                                            saveDay(selectedDay.copy(exercises = selectedDay.exercises.filterNot { it.id == exercise.id }))
                                                        } else {
                                                            swipeOffset.animateTo(0f, spring())
                                                        }
                                                        verticalDistance = 0f
                                                        horizontalDistance = 0f
                                                    }
                                                },
                                                onDragCancel = {
                                                    isDragging = false
                                                    verticalDistance = 0f
                                                    horizontalDistance = 0f
                                                    scope.launch { swipeOffset.animateTo(0f, spring()) }
                                                }
                                            )
                                        }
                                        .clickable { editing = exercise }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    WorkoutExerciseRowContent(exercise)

                                }
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Outline.copy(alpha = .45f)))
                        }
                        Button(
                            onClick = { saveDay(selectedDay.copy(isCompleted = !selectedDay.isCompleted)) },
                            enabled = selectedDay.exercises.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppControlShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedDay.isCompleted) Raised else Lime,
                                contentColor = if (selectedDay.isCompleted) Lime else Background,
                                disabledContainerColor = Raised,
                                disabledContentColor = Muted
                            )
                        ) {
                            Icon(Icons.Rounded.Check, null)
                            Spacer(Modifier.width(7.dp))
                            Text(if (selectedDay.isCompleted) "已完成 · 点击撤销" else "完成训练", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("今天安排恢复", color = Ink, fontSize = AppSectionTitleSize, fontWeight = FontWeight.Bold)
                        Text("休息日不可添加训练动作", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            ExercisePrCard(exercisePrs, onAdd = { addingPr = true })
        }
        item {
            WorkoutStatisticsCard(workoutStatistics)
        }
    }

    if (selectedDay.mode == DayMode.TRAINING && (addingExercise || editing != null)) {
        ExerciseEditorDialog(
            initial = editing,
            existingExerciseNames = savedDays.flatMap { it.exercises }.map { it.name },
            onDismiss = { addingExercise = false; editing = null },
            onSave = { exercise ->
                val updated = if (editing == null) selectedDay.exercises + exercise
                else selectedDay.exercises.map { if (it.id == editing?.id) exercise else it }
                saveDay(selectedDay.copy(exercises = updated))
                addingExercise = false
                editing = null
            }
        )
    }
    if (addingPr) {
        ExercisePrDialog(
            existingExerciseNames = (savedDays.flatMap { it.exercises }.map { it.name } + exercisePrs.map { it.exerciseName }).distinct(),
            onDismiss = { addingPr = false },
            onSave = {
                onAddExercisePr(it)
                addingPr = false
            }
        )
    }
}

@Composable
internal fun WorkoutStatisticsCard(statistics: WorkoutStatistics) {
    val topRegions = statistics.regionSets.take(6)
    val maxSets = topRegions.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = AppCardShape,
        border = BorderStroke(1.dp, CardOutline)
    ) {
        Column(Modifier.fillMaxWidth().padding(AppCardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("训练统计", color = Ink, fontSize = AppSectionTitleSize, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatisticValue("完成训练", statistics.completedSessions.toString(), "次", Modifier.weight(1f))
                StatisticValue("完成周期", statistics.completedCycles.toString(), "周", Modifier.weight(1f))
            }
            Text("部位容量", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (topRegions.isEmpty()) {
                Text("完成一次训练后，这里会按部位累计有效组数", color = Muted, fontSize = AppCaptionSize)
            } else {
                topRegions.forEach { (region, sets) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(region.label, color = Muted, fontSize = 12.sp, modifier = Modifier.width(68.dp))
                        Box(Modifier.weight(1f).height(8.dp).background(Raised, CircleShape)) {
                            Box(
                                Modifier.fillMaxWidth(sets.toFloat() / maxSets)
                                    .height(8.dp)
                                    .background(Lime, CircleShape)
                            )
                        }
                        Text("$sets 组", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
                Text("复合动作会计入所有相关部位", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

// All three columns share the actual card width; no fixed-width cell can push
// the edit icon outside the card. Stack the name on narrow/large-font screens.
@Composable
internal fun WorkoutExerciseRowContent(exercise: WorkoutExercise, modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stacked = maxWidth < 300.dp * fontScale
        if (stacked) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(exercise.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "编辑动作", tint = Muted, modifier = Modifier.size(16.dp))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutExerciseValue("${exercise.sets} 组 × ${exercise.reps} 次", Modifier.weight(1.3f))
                    WorkoutExerciseValue(exercise.weight, Modifier.weight(1f), isWeight = true)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(exercise.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1.1f))
                WorkoutExerciseValue("${exercise.sets} 组 × ${exercise.reps} 次", Modifier.weight(1.3f))
                WorkoutExerciseValue(exercise.weight, Modifier.weight(1f), isWeight = true)
                Icon(Icons.Rounded.ChevronRight, contentDescription = "编辑动作", tint = Muted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun WorkoutExerciseValue(value: String, modifier: Modifier, isWeight: Boolean = false) {
    Box(
        modifier.heightIn(min = 44.dp)
            .border(1.dp, Outline, AppControlShape)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            value,
            color = Ink,
            fontSize = if (isWeight) 14.sp else 12.sp,
            fontWeight = if (isWeight) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun ExercisePrCard(records: List<ExercisePr>, onAdd: () -> Unit) {
    val bestRecords = records.groupBy { it.exerciseName }.values
        .mapNotNull { entries -> entries.maxByOrNull { it.estimatedOneRepMax } }
        .sortedByDescending { it.date }
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = AppCardShape,
        border = BorderStroke(1.dp, CardOutline)
    ) {
        Column(Modifier.fillMaxWidth().padding(AppCardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("动作 PR", color = Ink, fontSize = AppSectionTitleSize, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onAdd) {
                    Text("记录 PR", color = Lime, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.Add, null, tint = Lime, modifier = Modifier.size(18.dp))
                }
            }
            if (bestRecords.isEmpty()) {
                Text("选择动作，记录你的重量与次数纪录", color = Muted, fontSize = AppCaptionSize)
            } else {
                bestRecords.take(6).forEachIndexed { index, pr ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(pr.exerciseName, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(pr.date.format(PrDateFormatter), color = Muted, fontSize = 10.sp)
                        }
                        Text(if (pr.weightKg % 1.0 == 0.0) pr.weightKg.toInt().toString() else "%.1f".format(pr.weightKg), color = Lime, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(" kg × ${pr.reps}", color = Muted, fontSize = 11.sp)
                    }
                    if (index < bestRecords.take(6).lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Outline.copy(alpha = .45f)))
                }
            }
        }
    }
}

@Composable
internal fun ExercisePrDialog(
    existingExerciseNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (ExercisePr) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("1") }
    val choices = remember(existingExerciseNames, search) {
        val all = (existingExerciseNames + recommendedWorkoutExerciseNames + commonWorkoutExerciseNames + spreadsheetWorkoutExerciseNames).distinct()
        if (search.isBlank()) (existingExerciseNames + recommendedWorkoutExerciseNames).distinct().take(9)
        else all.filter { workoutExerciseMatches(it, search) }.take(9)
    }
    val weightValue = weight.replace(',', '.').toDoubleOrNull()
    val repsValue = reps.toIntOrNull()
    val valid = name.isNotBlank() && weightValue?.let { it > 0.0 && it <= 1000.0 } == true && repsValue?.let { it in 1..100 } == true
    AppModalSheet(
        title = "记录动作 PR",
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                search,
                { search = it },
                label = { Text("搜索动作") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (choices.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    choices.chunked(3).forEach { rowChoices ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowChoices.forEach { choice ->
                                FilterChip(
                                    selected = name == choice,
                                    onClick = { name = choice; search = choice },
                                    label = { Text(choice, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowChoices.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            OutlinedTextField(name, { name = it }, label = { Text("动作名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(weight, { weight = it.filter { char -> char.isDigit() || char == '.' || char == ',' } }, label = { Text("重量") }, suffix = { Text("kg") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(reps, { reps = it.filter(Char::isDigit).take(3) }, label = { Text("次数") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Text("同一动作会保留历史记录，并展示估算强度最高的一次", color = Muted, fontSize = 10.sp)
        },
        footer = {
            PrimaryModalButton("保存 PR", valid) {
                onSave(ExercisePr(System.currentTimeMillis(), name.trim(), weightValue!!, repsValue!!, LocalDate.now()))
            }
        }
    )
}

@Composable
internal fun StatisticValue(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Raised, AppControlShape).padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label, color = Muted, fontSize = AppCaptionSize)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Lime, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(" $unit", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

internal fun workoutBackgroundResource(focus: WorkoutFocus): Int = when {
    focus.title == "腿部力量" || focus.title == "后链力量" -> R.drawable.training_bg_leg_power
    focus.primaryRegion == MuscleRegion.CHEST -> R.drawable.training_bg_chest
    focus.primaryRegion == MuscleRegion.BACK -> R.drawable.training_bg_back
    focus.primaryRegion == MuscleRegion.SHOULDERS -> R.drawable.training_bg_shoulders
    focus.primaryRegion == MuscleRegion.BICEPS -> R.drawable.training_bg_biceps
    focus.primaryRegion == MuscleRegion.TRICEPS -> R.drawable.training_bg_triceps
    focus.primaryRegion == MuscleRegion.CORE -> R.drawable.training_bg_core
    focus.primaryRegion == MuscleRegion.GLUTES -> R.drawable.training_bg_glutes
    focus.primaryRegion == MuscleRegion.QUADS -> R.drawable.training_bg_quadriceps
    focus.primaryRegion == MuscleRegion.HAMSTRINGS -> R.drawable.training_bg_hamstrings
    focus.primaryRegion == MuscleRegion.CALVES -> R.drawable.training_bg_calves
    focus.primaryRegion == MuscleRegion.LOWER_BACK -> R.drawable.training_bg_lower_back
    focus.primaryRegion == MuscleRegion.ERECTOR_SPINAE -> R.drawable.training_bg_erector_spinae
    else -> R.drawable.training_bg_core
}

@Composable
internal fun TrainingFocusHero(
    date: LocalDate,
    day: WorkoutDay,
    focus: WorkoutFocus
) {
    val estimatedMinutes = estimateWorkoutMinutes(day.exercises)
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = AppCardShape,
        border = BorderStroke(1.dp, CardOutline)
    ) {
        Box(Modifier.fillMaxWidth().height(248.dp).clip(AppCardShape)) {
            Image(
                painter = painterResource(
                    when {
                        day.mode == DayMode.REST -> R.drawable.training_bg_rest
                        day.exercises.isEmpty() -> R.drawable.training_bg_preparation
                        else -> workoutBackgroundResource(focus)
                    }
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Background,
                        .52f to Background.copy(alpha = .92f),
                        .78f to Background.copy(alpha = .22f),
                        1f to Color.Transparent
                    )
                )
            )
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("今日训练", color = Ink, fontSize = AppSectionTitleSize, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.border(1.dp, Lime, AppControlShape).padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(day.mode.label, color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    if (day.mode == DayMode.TRAINING) focus.title else "好好休息",
                    color = Lime,
                    fontSize = 39.sp,
                    fontWeight = FontWeight.Black
                )
                Text("${date.format(MonthDayFormatter)}  ${date.dayOfWeek.shortChinese()}", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(9.dp))
                Text(
                    if (day.mode == DayMode.TRAINING) "饮食目标 · 训练日" else "饮食目标 · 休息日",
                    color = Lime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                if (day.mode == DayMode.TRAINING) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(day.exercises.size.toString(), color = Lime, fontSize = 42.sp, fontWeight = FontWeight.Black, modifier = Modifier.alignByBaseline())
                        Text(" 动作", color = Muted, fontSize = 12.sp, modifier = Modifier.alignByBaseline())
                        Spacer(Modifier.weight(1f))
                        Text("约 ", color = Muted, fontSize = 12.sp, modifier = Modifier.alignByBaseline())
                        Text(estimatedMinutes.toString(), color = Lime, fontSize = 42.sp, fontWeight = FontWeight.Black, modifier = Modifier.alignByBaseline())
                        Text(" 分钟", color = Muted, fontSize = 12.sp, modifier = Modifier.alignByBaseline())
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExerciseEditorDialog(
    initial: WorkoutExercise?,
    existingExerciseNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (WorkoutExercise) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var sets by remember(initial) { mutableStateOf(initial?.sets?.toString() ?: "4") }
    var reps by remember(initial) { mutableStateOf(initial?.reps ?: "5") }
    var weight by remember(initial) { mutableStateOf(initial?.weight ?: "80%") }
    var search by remember(initial) { mutableStateOf("") }
    val choices = remember(existingExerciseNames, search) {
        if (search.isBlank()) recommendedWorkoutExerciseNames
        else (existingExerciseNames + commonWorkoutExerciseNames + spreadsheetWorkoutExerciseNames)
            .distinct().filter { workoutExerciseMatches(it, search) }.take(9)
    }
    val valid = name.isNotBlank() && sets.toIntOrNull()?.let { it in 1..30 } == true && reps.isNotBlank() && weight.isNotBlank()
    AppModalSheet(
        title = if (initial == null) "添加动作" else "编辑动作",
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("搜索动作") },
                placeholder = { Text("搜索计划内动作或常用动作") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(if (search.isBlank()) "推荐动作" else "搜索结果", color = Muted, fontSize = 11.sp)
            if (choices.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    choices.chunked(3).forEach { rowChoices ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowChoices.forEach { choice ->
                                FilterChip(
                                    selected = name == choice,
                                    onClick = { name = choice; search = choice },
                                    label = { Text(choice, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowChoices.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            } else {
                Text("没有找到动作，可直接输入动作名称", color = Muted, fontSize = 11.sp)
            }
            OutlinedTextField(name, { name = it }, label = { Text("动作名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(sets, { sets = it.filter(Char::isDigit) }, label = { Text("组数") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(reps, { reps = it }, label = { Text("次数") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(weight, { weight = it }, label = { Text("重量 / 强度") }, placeholder = { Text("例如 80kg 或 85%") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        footer = {
            PrimaryModalButton("保存动作", valid) {
                onSave(WorkoutExercise(initial?.id ?: System.currentTimeMillis(), name.trim(), sets.toInt(), reps.trim(), weight.trim()))
            }
        }
    )
}
