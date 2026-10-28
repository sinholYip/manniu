package com.ranke.health.core

import com.ranke.health.model.Candle
import com.ranke.health.model.MealPeriod
import java.time.LocalDate

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
