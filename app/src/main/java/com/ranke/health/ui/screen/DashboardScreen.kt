package com.ranke.health.ui.screen


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Scale
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ranke.health.core.demoCandles
import com.ranke.health.model.CaloriePlan
import com.ranke.health.model.Candle
import com.ranke.health.model.DashboardState
import com.ranke.health.model.DayMode
import com.ranke.health.model.MealEntry
import com.ranke.health.model.MealPeriod
import com.ranke.health.model.WeightEntry
import com.ranke.health.model.fiveDayMovingAverage
import com.ranke.health.model.weightsToCandles
import com.ranke.health.ui.components.AppModalSheet
import com.ranke.health.ui.components.MealSectionTitle
import com.ranke.health.ui.components.PrimaryModalButton
import com.ranke.health.ui.components.shortChinese
import com.ranke.health.ui.theme.AppCardPadding
import com.ranke.health.ui.theme.AppCardShape
import com.ranke.health.ui.theme.AppControlShape
import com.ranke.health.ui.theme.AppPageTitleSize
import com.ranke.health.ui.theme.AppSectionTitleSize
import com.ranke.health.ui.theme.Background
import com.ranke.health.ui.theme.CardOutline
import com.ranke.health.ui.theme.DashboardSection
import com.ranke.health.ui.theme.DownGreen
import com.ranke.health.ui.theme.Ink
import com.ranke.health.ui.theme.Lime
import com.ranke.health.ui.theme.MonthDayFormatter
import com.ranke.health.ui.theme.MovingAverage
import com.ranke.health.ui.theme.Muted
import com.ranke.health.ui.theme.Outline
import com.ranke.health.ui.theme.Raised
import com.ranke.health.ui.theme.ShortDateFormatter
import com.ranke.health.ui.theme.Surface
import com.ranke.health.ui.theme.TrendRange
import com.ranke.health.ui.theme.UpRed
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun DraggableDashboardBlock(
    modifier: Modifier = Modifier,
    section: DashboardSection,
    onMove: (direction: Int) -> Unit,
    content: @Composable () -> Unit
) {
    val moveThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    var dragDistance by remember(section) { mutableStateOf(0f) }
    var horizontalDistance by remember(section) { mutableStateOf(0f) }
    var isDragging by remember(section) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isDragging) 1.025f else 1f, label = "dashboardDragScale")
    Box(
        modifier.fillMaxWidth()
            .zIndex(if (isDragging) 2f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (isDragging) 18.dp else 0.dp, AppCardShape, ambientColor = Lime, spotColor = Lime)
            .border(if (isDragging) 2.dp else 0.dp, if (isDragging) Lime else Color.Transparent, AppCardShape)
            .pointerInput(section) {
            detectDragGesturesAfterLongPress(
                onDragStart = { isDragging = true; dragDistance = 0f; horizontalDistance = 0f },
                onDragEnd = { isDragging = false; dragDistance = 0f; horizontalDistance = 0f },
                onDragCancel = { isDragging = false; dragDistance = 0f; horizontalDistance = 0f },
                onDrag = { change, dragAmount ->
                    dragDistance += dragAmount.y
                    horizontalDistance += dragAmount.x
                    if (kotlin.math.abs(dragDistance) > kotlin.math.abs(horizontalDistance)) change.consume()
                    if (dragDistance >= moveThreshold && kotlin.math.abs(dragDistance) > kotlin.math.abs(horizontalDistance)) {
                        onMove(1)
                        dragDistance = 0f
                    } else if (dragDistance <= -moveThreshold && kotlin.math.abs(dragDistance) > kotlin.math.abs(horizontalDistance)) {
                        onMove(-1)
                        dragDistance = 0f
                    }
                }
            )
        }
    ) { content() }
}

@Composable
internal fun Header(plan: CaloriePlan) {
    val today = LocalDate.now()
    val mode = plan.modeFor(today)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row {
                Text("man", color = Ink, fontSize = AppPageTitleSize, fontWeight = FontWeight.Black, modifier = Modifier.alignByBaseline())
                Text("牛", color = Lime, fontSize = AppPageTitleSize, fontWeight = FontWeight.Black, modifier = Modifier.alignByBaseline())
            }
            Text("看见每一天的身体趋势", color = Muted, fontSize = 13.sp)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("${today.format(MonthDayFormatter)} ${today.dayOfWeek.shortChinese()}", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Box(
                Modifier.border(1.dp, Lime, AppControlShape).padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(mode.label, color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun TodayNutritionHero(meals: List<MealEntry>, plan: CaloriePlan) {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val todayMeals = remember(meals, today, zone) {
        meals.filter { it.time.atZone(zone).toLocalDate() == today }
    }
    val consumed = todayMeals.sumOf { it.kilocalories }
    val goal = plan.targetFor(today)
    val (targetCarbs, targetProtein, targetFat) = plan.macroTargetsFor(today)
    val carbs = todayMeals.sumOf { it.carbohydratesGrams }
    val protein = todayMeals.sumOf { it.proteinGrams }
    val fat = todayMeals.sumOf { it.fatGrams }
    val progress = (consumed / goal).coerceIn(0.0, 1.0).toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = AppCardShape,
        border = BorderStroke(1.dp, CardOutline)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("今日摄入", color = Ink, fontSize = AppSectionTitleSize, fontWeight = FontWeight.Bold)
                    Row {
                        Text("%,.0f".format(consumed), color = Lime, fontSize = 42.sp, fontWeight = FontWeight.Black, modifier = Modifier.alignByBaseline())
                        Text(" kcal", color = Muted, fontSize = 13.sp, modifier = Modifier.alignByBaseline())
                    }
                    Text("目标 %,.0f  ·  剩余 %,.0f".format(goal, (goal - consumed).coerceAtLeast(0.0)), color = Muted, fontSize = 11.sp)
                }
                Text("%.0f%%".format(progress * 100), color = Lime, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).background(Raised, CircleShape)) {
                Box(Modifier.fillMaxWidth(progress).height(8.dp).background(Lime, CircleShape))
            }
            MacroProgressRow("碳水", carbs, targetCarbs, Lime)
            MacroProgressRow("蛋白质", protein, targetProtein, DownGreen)
            MacroProgressRow("脂肪", fat, targetFat, UpRed)
        }
    }
}

@Composable
internal fun MacroProgressRow(label: String, consumed: Double, target: Double, color: Color) {
    val progress = (consumed / target.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 12.sp, modifier = Modifier.width(52.dp))
        Box(Modifier.weight(1f).height(6.dp).background(Raised, CircleShape)) {
            Box(Modifier.fillMaxWidth(progress).height(6.dp).background(color, CircleShape))
        }
        Spacer(Modifier.width(10.dp))
        Text("%.0f/%.0fg".format(consumed, target), color = if (progress >= 1f) color else Muted, fontSize = 11.sp, modifier = Modifier.width(70.dp))
    }
}

@Composable
internal fun SaveCenterCard(
    onSaveData: () -> Unit,
    saveEnabled: Boolean,
    onRestoreData: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Raised, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Save, null, tint = Lime)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("保存中心", color = Ink, fontWeight = FontWeight.Bold, fontSize = AppSectionTitleSize)
                    Text("完整备份包含当前身体设置与全部记录", color = Muted, fontSize = 11.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRestoreData, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(contentColor = Lime)) {
                    Icon(Icons.Rounded.Download, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("恢复数据")
                }
                Button(
                    onClick = onSaveData,
                    enabled = saveEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Background),
                    shape = AppControlShape
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("保存全部数据", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun TodaySummary(state: DashboardState) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val latest = state.weights.maxByOrNull { it.time }?.kilograms
    val todayCalories = remember(state.meals, today, zone) {
        state.meals.filter { it.time.atZone(zone).toLocalDate() == today }.sumOf { it.kilocalories }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(Modifier.weight(1f), "当前体重", latest?.let { "%.1f kg".format(it) } ?: "-- kg", Icons.Rounded.Scale)
        MetricCard(Modifier.weight(1f), "今日摄入", "%.0f kcal".format(todayCalories), Icons.Rounded.Restaurant)
    }
}

@Composable
internal fun MetricCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.padding(17.dp)) {
            Icon(icon, null, tint = Muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(18.dp))
            Text(label, color = Muted, fontSize = 12.sp)
            Text(value, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
internal fun WeightChartCard(entries: List<WeightEntry>, meals: List<MealEntry>, isDemo: Boolean, onAdd: () -> Unit) {
    var selectedRange by remember { mutableStateOf(TrendRange.DAYS_14) }
    val allCandles = remember(entries, isDemo) { if (isDemo) demoCandles() else weightsToCandles(entries) }
    val cutoff = selectedRange.days?.let { LocalDate.now().minusDays(it - 1) }
    val candles = remember(allCandles, cutoff) {
        if (cutoff == null) allCandles else allCandles.filter { !it.date.isBefore(cutoff) }
    }
    val zone = ZoneId.systemDefault()
    val caloriesByDate = remember(meals, candles, isDemo, zone) {
        if (isDemo) {
            val values = listOf(1860.0, 2050.0, 1780.0, 1920.0, 2240.0, 1690.0, 1810.0, 1975.0, 2130.0, 1880.0, 1760.0, 2010.0, 1840.0, 1268.0)
            candles.mapIndexed { index, candle -> candle.date to values.getOrElse(index) { 0.0 } }.toMap()
        } else {
            meals.groupBy { it.time.atZone(zone).toLocalDate() }
                .mapValues { (_, records) -> records.sumOf { it.kilocalories } }
        }
    }
    val change = candles.lastOrNull()?.let { it.close - it.open } ?: 0.0
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.padding(AppCardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("体重趋势", color = Ink, fontWeight = FontWeight.Bold, fontSize = AppSectionTitleSize)
                        if (isDemo) {
                            Spacer(Modifier.width(8.dp))
                            Text("演示", color = Muted, fontSize = 10.sp, modifier = Modifier.background(Background, AppControlShape).padding(7.dp, 3.dp))
                        }
                    }
                    Text("红涨绿待上涨 · ${selectedRange.label}", color = Muted, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("%+.1f kg".format(change), color = if (change >= 0) UpRed else DownGreen, fontWeight = FontWeight.Bold)
                    Text("当日开盘至收盘", color = Muted, fontSize = 10.sp)
                }
                Spacer(Modifier.width(10.dp))
                AddButton(onAdd)
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(TrendRange.entries) { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        label = { Text(range.label, fontSize = 11.sp) }
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            WeightCalorieChart(candles, caloriesByDate, Modifier.fillMaxWidth().height(300.dp))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Legend(UpRed, "上涨")
                Spacer(Modifier.width(18.dp))
                Legend(DownGreen, "待上涨")
                Spacer(Modifier.width(18.dp))
                Legend(MovingAverage, "MA5")
                Spacer(Modifier.width(18.dp))
                Text("柱高 = 每日摄入", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
internal fun AddButton(onClick: () -> Unit) {
    Box(Modifier.size(38.dp).background(Raised, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Add, "添加", tint = Lime)
    }
}

@Composable
internal fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(text, color = Muted, fontSize = 11.sp)
    }
}

@Composable
internal fun WeightCalorieChart(candles: List<Candle>, caloriesByDate: Map<LocalDate, Double>, modifier: Modifier = Modifier) {
    if (candles.isEmpty()) return
    var selectedIndex by remember(candles.map { it.date }) { mutableStateOf<Int?>(null) }
    val labelWidthPx = with(LocalDensity.current) { 42.dp.toPx() }
    Canvas(
        modifier.pointerInput(candles) {
            detectTapGestures { tap ->
                val chartWidth = (size.width.toFloat() - labelWidthPx).coerceAtLeast(1f)
                if (tap.x in 0f..chartWidth) {
                    val slot = chartWidth / candles.size
                    selectedIndex = (tap.x / slot).toInt().coerceIn(0, candles.lastIndex)
                }
            }
        }.pointerInput(candles) {
            fun selectAt(x: Float) {
                val chartWidth = (size.width.toFloat() - labelWidthPx).coerceAtLeast(1f)
                if (x in 0f..chartWidth) {
                    val slot = chartWidth / candles.size
                    selectedIndex = (x / slot).toInt().coerceIn(0, candles.lastIndex)
                }
            }
            var horizontalDistance = 0f
            var verticalDistance = 0f
            detectDragGesturesAfterLongPress(
                onDragStart = { start ->
                    horizontalDistance = 0f
                    verticalDistance = 0f
                    selectAt(start.x)
                },
                onDrag = { change, dragAmount ->
                    horizontalDistance += dragAmount.x
                    verticalDistance += dragAmount.y
                    if (kotlin.math.abs(horizontalDistance) >= kotlin.math.abs(verticalDistance)) {
                        change.consume()
                        selectAt(change.position.x)
                    }
                }
            )
        }
    ) {
        val labelWidth = 42.dp.toPx()
        val bottom = 24.dp.toPx()
        val sectionGap = 22.dp.toPx()
        val volumeHeight = 76.dp.toPx()
        val chartWidth = size.width - labelWidth
        val candleHeight = size.height - bottom - sectionGap - volumeHeight
        val volumeTop = candleHeight + sectionGap
        val volumeBottom = size.height - bottom
        val rawMin = candles.minOf { it.low }
        val rawMax = candles.maxOf { it.high }
        val pad = maxOf((rawMax - rawMin) * .18, .2)
        val min = rawMin - pad
        val max = rawMax + pad
        fun y(value: Double): Float = ((max - value) / (max - min) * candleHeight).toFloat()

        val calorieValues = candles.map { caloriesByDate[it.date] ?: 0.0 }
        val calorieMax = (calorieValues.maxOrNull() ?: 0.0).coerceAtLeast(1.0) * 1.12
        fun volumeY(value: Double): Float = volumeBottom - (value / calorieMax * volumeHeight).toFloat()

        val grid = Color(0xFF2A2A2D)
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(145, 160, 152)
            textSize = 10.sp.toPx()
            isAntiAlias = true
        }
        repeat(4) { index ->
            val gy = candleHeight * index / 3f
            drawLine(grid, Offset(0f, gy), Offset(chartWidth, gy), 1.dp.toPx())
            val value = max - (max - min) * index / 3
            drawContext.canvas.nativeCanvas.drawText("%.1f".format(value), chartWidth + 8.dp.toPx(), gy + 4.dp.toPx(), textPaint)
        }

        val slot = chartWidth / candles.size
        val bodyWidth = (slot * .52f).coerceAtMost(14.dp.toPx()).coerceAtLeast(1.5.dp.toPx())
        candles.forEachIndexed { index, candle ->
            val x = slot * index + slot / 2f
            val color = if (candle.isUp) UpRed else DownGreen
            drawLine(color, Offset(x, y(candle.high)), Offset(x, y(candle.low)), 1.4.dp.toPx())
            val top = y(maxOf(candle.open, candle.close))
            val lower = y(minOf(candle.open, candle.close))
            drawRect(color, Offset(x - bodyWidth / 2f, top), Size(bodyWidth, maxOf(lower - top, 2.dp.toPx())))
        }

        val movingAverages = fiveDayMovingAverage(candles)
        val movingAveragePoints = candles.mapIndexedNotNull { index, candle ->
            movingAverages[candle.date]?.let { average -> Offset(slot * index + slot / 2f, y(average)) }
        }
        movingAveragePoints.zipWithNext().forEach { (start, end) ->
            drawLine(MovingAverage, start, end, 2.dp.toPx())
        }

        drawLine(grid, Offset(0f, volumeTop - 10.dp.toPx()), Offset(chartWidth, volumeTop - 10.dp.toPx()), 1.dp.toPx())
        drawContext.canvas.nativeCanvas.drawText("每日热量", 0f, volumeTop - 14.dp.toPx(), textPaint)
        drawContext.canvas.nativeCanvas.drawText("${calorieMax.toInt()} kcal", chartWidth + 4.dp.toPx(), volumeTop + 4.dp.toPx(), textPaint)
        drawContext.canvas.nativeCanvas.drawText("0", chartWidth + 8.dp.toPx(), volumeBottom, textPaint)

        candles.forEachIndexed { index, candle ->
            val x = slot * index + slot / 2f
            val calories = caloriesByDate[candle.date] ?: 0.0
            val top = volumeY(calories)
            val color = if (candle.isUp) UpRed else DownGreen
            if (calories > 0.0) {
                drawRect(
                    color.copy(alpha = .82f),
                    Offset(x - bodyWidth / 2f, top),
                    Size(bodyWidth, (volumeBottom - top).coerceAtLeast(1.dp.toPx()))
                )
            }
            if (index == 0 || index == candles.lastIndex || index == candles.size / 2) {
                val label = candle.date.format(ShortDateFormatter)
                val measured = textPaint.measureText(label)
                drawContext.canvas.nativeCanvas.drawText(label, (x - measured / 2).coerceIn(0f, chartWidth - measured), size.height - 3.dp.toPx(), textPaint)
            }
        }

        selectedIndex?.takeIf { it in candles.indices }?.let { index ->
            val candle = candles[index]
            val x = slot * index + slot / 2f
            val weightY = y(candle.close)
            val calories = caloriesByDate[candle.date] ?: 0.0
            val calorieY = volumeY(calories)
            val crosshair = Color.White.copy(alpha = .5f)
            val dashed = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))
            drawLine(crosshair, Offset(x, 0f), Offset(x, volumeBottom), 1.dp.toPx(), pathEffect = dashed)
            drawLine(crosshair, Offset(0f, weightY), Offset(chartWidth, weightY), 1.dp.toPx(), pathEffect = dashed)
            drawCircle(Lime, 4.dp.toPx(), Offset(x, weightY))
            if (calories > 0.0) drawCircle(Lime, 4.dp.toPx(), Offset(x, calorieY))

            val tooltipPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(243, 247, 244)
                textSize = 11.sp.toPx()
                isAntiAlias = true
            }
            val dateText = "${candle.date.format(MonthDayFormatter)} ${candle.date.dayOfWeek.shortChinese()}"
            val weightText = "体重 %.1f kg".format(candle.close)
            val calorieText = "热量 %.0f kcal".format(calories)
            val tooltipWidth = 116.dp.toPx()
            val tooltipHeight = 64.dp.toPx()
            val tooltipLeft = if (x + tooltipWidth + 8.dp.toPx() <= chartWidth) x + 8.dp.toPx() else x - tooltipWidth - 8.dp.toPx()
            val tooltipTop = (weightY - tooltipHeight / 2f).coerceIn(4.dp.toPx(), candleHeight - tooltipHeight - 4.dp.toPx())
            drawContext.canvas.nativeCanvas.drawRoundRect(
                android.graphics.RectF(tooltipLeft, tooltipTop, tooltipLeft + tooltipWidth, tooltipTop + tooltipHeight),
                10.dp.toPx(), 10.dp.toPx(),
                android.graphics.Paint().apply { color = android.graphics.Color.rgb(32, 42, 37); isAntiAlias = true }
            )
            drawContext.canvas.nativeCanvas.drawText(dateText, tooltipLeft + 10.dp.toPx(), tooltipTop + 17.dp.toPx(), tooltipPaint)
            tooltipPaint.color = android.graphics.Color.rgb(201, 243, 106)
            tooltipPaint.isFakeBoldText = true
            drawContext.canvas.nativeCanvas.drawText(weightText, tooltipLeft + 10.dp.toPx(), tooltipTop + 36.dp.toPx(), tooltipPaint)
            tooltipPaint.color = android.graphics.Color.rgb(145, 160, 152)
            tooltipPaint.isFakeBoldText = false
            drawContext.canvas.nativeCanvas.drawText(calorieText, tooltipLeft + 10.dp.toPx(), tooltipTop + 54.dp.toPx(), tooltipPaint)
        }
    }
}

@Composable
internal fun CalorieCard(
    meals: List<MealEntry>,
    plan: CaloriePlan,
    onAdd: () -> Unit,
    onEditPlan: () -> Unit,
    onPeriodClick: (MealPeriod) -> Unit
) {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val goal = plan.targetFor(today)
    val todayMeals = meals.filter { it.time.atZone(zone).toLocalDate() == today }
    val visiblePeriods = plan.mealPeriodsFor(today)
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.padding(AppCardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${visiblePeriods.size}餐进度", color = Ink, fontSize = AppSectionTitleSize, fontWeight = FontWeight.Bold)
                    Text(if (plan.modeFor(today) == DayMode.TRAINING) "训练日包含练前餐" else "休息日不安排练前餐", color = Muted, fontSize = 11.sp)
                }
                Box(Modifier.size(38.dp).background(Raised, CircleShape).clickable(onClick = onEditPlan), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Tune, "设置营养计划", tint = Muted, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(38.dp).background(Raised, CircleShape).clickable(onClick = onAdd), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, "记录饮食", tint = Lime)
                }
            }
            Spacer(Modifier.height(14.dp))
            visiblePeriods.forEachIndexed { index, period ->
                val kcal = todayMeals.filter { it.period == period }.sumOf { it.kilocalories }
                val mealTarget = plan.calorieTargetFor(today, period)
                val macroTarget = plan.macroTargetsFor(today, period)
                MealProgressRow(period, kcal, mealTarget, macroTarget) { onPeriodClick(period) }
                if (index != visiblePeriods.lastIndex) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Raised))
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
internal fun MealProgressRow(
    period: MealPeriod,
    consumed: Double,
    target: Double,
    macroTarget: Triple<Double, Double, Double>,
    onClick: () -> Unit
) {
    val progress = (consumed / target).coerceIn(0.0, 1.0).toFloat()
    val periodColor = when (period) {
        MealPeriod.BREAKFAST -> Lime
        MealPeriod.LUNCH -> DownGreen
        MealPeriod.PRE_WORKOUT -> Lime
        MealPeriod.DINNER -> Color(0xFFFF9B52)
        MealPeriod.NIGHT_SNACK -> Color(0xFFB89CFF)
    }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(periodColor, CircleShape))
            Spacer(Modifier.width(9.dp))
            Column {
                Text(period.label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("碳 %.0fg  蛋 %.0fg  脂 %.0fg".format(macroTarget.first, macroTarget.second, macroTarget.third), color = Muted, fontSize = 9.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("%.0f / %.0f kcal".format(consumed, target), color = Muted, fontSize = 11.sp)
            Spacer(Modifier.width(5.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Muted, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).background(Raised, CircleShape)) {
            Box(Modifier.fillMaxWidth(progress).height(7.dp).background(periodColor, CircleShape))
        }
    }
}

@Composable
internal fun DailyTipCard(meals: List<MealEntry>, plan: CaloriePlan) {
    val today = LocalDate.now()
    val todayMeals = meals.filter { it.time.atZone(ZoneId.systemDefault()).toLocalDate() == today }
    val consumed = todayMeals.sumOf { it.kilocalories }
    val goal = plan.targetFor(today)
    val tip = when {
        todayMeals.isEmpty() -> "记下第一餐，才能看见今天的营养趋势"
        consumed > goal -> "今日已超出目标，剩余时间选择清淡食物并适量活动"
        todayMeals.sumOf { it.proteinGrams } < plan.macroTargetsFor(today).second * .5 -> "今日蛋白质进度偏低，下一餐可优先选鱼、蛋或瘦肉"
        else -> "晚餐后散步 20 分钟，有助于稳定餐后状态"
    }
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = AppCardShape, border = BorderStroke(1.dp, CardOutline)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Lime.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Lightbulb, null, tint = Lime, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("今日提示", color = Lime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(tip, color = Ink, fontSize = 12.sp)
            }
        }
    }
}

@Composable
internal fun MealRow(meal: MealEntry, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Surface, AppCardShape)
            .border(1.dp, Outline, AppCardShape).clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).background(Background, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Restaurant, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(meal.name, color = Ink, fontWeight = FontWeight.SemiBold)
            Text("${meal.period.label} · ${meal.source}", color = Muted, fontSize = 11.sp)
            Text(
                "碳水 %.1fg  蛋白 %.1fg  脂肪 %.1fg".format(meal.carbohydratesGrams, meal.proteinGrams, meal.fatGrams),
                color = Muted,
                fontSize = 10.sp
            )
        }
        Text("%.0f kcal".format(meal.kilocalories), color = Ink, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun WeightDialog(entries: List<WeightEntry>, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var value by remember { mutableStateOf("") }
    val parsed = value.toDoubleOrNull()
    AppModalSheet(
        title = "记录体重",
        subtitle = "今天的体重",
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink),
                suffix = { Text("kg", color = Muted, fontSize = 18.sp) },
                placeholder = { Text("70.0", color = Muted.copy(alpha = .45f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("支持 20–400 kg，保留一位小数即可", color = Muted, fontSize = 11.sp)
            if (entries.isNotEmpty()) {
                MealSectionTitle("最近记录", "用于确认本次输入是否合理")
                entries.sortedByDescending { it.time }.take(5).forEach { entry ->
                    val date = entry.time.atZone(ZoneId.systemDefault()).toLocalDate()
                    Row(
                        Modifier.fillMaxWidth().background(Raised, AppControlShape)
                            .border(1.dp, Outline, AppControlShape).padding(13.dp, 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${date.format(MonthDayFormatter)} ${date.dayOfWeek.shortChinese()}", color = Ink, modifier = Modifier.weight(1f))
                        Text("%.1f kg".format(entry.kilograms), color = Lime, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        footer = {
            PrimaryModalButton("保存体重", parsed != null && parsed in 20.0..400.0) { parsed?.let(onSave) }
        }
    )
}
