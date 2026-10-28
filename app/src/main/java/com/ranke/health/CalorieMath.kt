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

internal fun macroValue(values: Triple<String, String, String>, index: Int): Double = when (index) {
    0 -> values.first.toDoubleOrNull() ?: 0.0
    1 -> values.second.toDoubleOrNull() ?: 0.0
    else -> values.third.toDoubleOrNull() ?: 0.0
}

internal fun replaceMacro(values: Triple<String, String, String>, index: Int, value: String) = when (index) {
    0 -> Triple(value, values.second, values.third)
    1 -> Triple(values.first, value, values.third)
    else -> Triple(values.first, values.second, value)
}

internal fun balancedParts(total: Double, weights: List<Double>): List<Double> {
    if (weights.isEmpty()) return emptyList()
    val safeTotal = total.coerceAtLeast(0.0)
    val weightSum = weights.sum()
    val raw = if (weightSum > 0.0) weights.map { safeTotal * it.coerceAtLeast(0.0) / weightSum }
    else List(weights.size) { safeTotal / weights.size }
    val rounded = raw.dropLast(1).map { kotlin.math.round(it * 10.0) / 10.0 }.toMutableList()
    rounded += (safeTotal - rounded.sum()).coerceAtLeast(0.0)
    return rounded
}

internal fun scaleMealMacro(
    source: Map<MealPeriod, Triple<String, String, String>>,
    macroIndex: Int,
    target: Double,
    periods: List<MealPeriod>
): Map<MealPeriod, Triple<String, String, String>> {
    val allocations = balancedParts(target, periods.map { macroValue(source.getValue(it), macroIndex) })
    return source.toMutableMap().apply {
        periods.forEachIndexed { index, period ->
            this[period] = replaceMacro(getValue(period), macroIndex, formatNutrition(allocations[index]))
        }
    }
}

internal fun rebalanceMealMacro(
    source: Map<MealPeriod, Triple<String, String, String>>,
    editedPeriod: MealPeriod,
    macroIndex: Int,
    rawValue: String,
    target: Double,
    periods: List<MealPeriod>,
    lockedPeriods: Set<MealPeriod>
): Map<MealPeriod, Triple<String, String, String>> {
    val parsed = rawValue.toDoubleOrNull() ?: return source +
        (editedPeriod to replaceMacro(source.getValue(editedPeriod), macroIndex, rawValue))
    val lockedOthers = periods.filter { it != editedPeriod && it in lockedPeriods }
    val lockedTotal = lockedOthers.sumOf { macroValue(source.getValue(it), macroIndex) }
    val edited = parsed.coerceIn(0.0, (target - lockedTotal).coerceAtLeast(0.0))
    val adjustable = periods.filterNot { it in lockedPeriods }
    val allocations = balancedParts(target - lockedTotal - edited, adjustable.map { macroValue(source.getValue(it), macroIndex) })
    return source.toMutableMap().apply {
        this[editedPeriod] = replaceMacro(getValue(editedPeriod), macroIndex, formatNutrition(edited))
        adjustable.forEachIndexed { index, period ->
            this[period] = replaceMacro(getValue(period), macroIndex, formatNutrition(allocations[index]))
        }
    }
}


internal fun formatNutrition(value: Double): String =
    if (value % 1.0 < .01) "%.0f".format(value) else "%.1f".format(value)

internal fun demoCandles(): List<Candle> {
    val closes = listOf(72.8, 72.5, 72.7, 72.2, 71.9, 72.1, 71.8, 71.5, 71.7, 71.3, 71.0, 71.2, 70.9, 70.6)
    var open = 73.0
    return closes.mapIndexed { index, close ->
        val spread = .12 + (index % 3) * .04
        Candle(LocalDate.now().minusDays((closes.lastIndex - index).toLong()), open, close, maxOf(open, close) + spread, minOf(open, close) - spread)
            .also { open = close }
    }
}
