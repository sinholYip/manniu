package com.ranke.health.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter

internal val Ink = Color(0xFFF7F7F5)
internal val Muted = Color(0xFF8F8F93)
internal val Surface = Color(0xFF0D0D0F)
internal val Background = Color(0xFF050505)
internal val Accent = Color(0xFF20220C)
internal val Raised = Color(0xFF151517)
internal val Outline = Color(0xFF3A3A3D)
internal val CardOutline = Color(0xFF2A2A2C)
internal val UpRed = Color(0xFFFF5C61)
internal val DownGreen = Color(0xFF63D49B)
internal val Lime = Color(0xFFD7FF2F)
internal val MovingAverage = Color(0xFFD7FF2F)
internal val MonthDayFormatter = DateTimeFormatter.ofPattern("M月d日")
internal val ShortDateFormatter = DateTimeFormatter.ofPattern("M/d")
internal val PrDateFormatter = DateTimeFormatter.ofPattern("yyyy.M.d")
internal val AppCardShape = RoundedCornerShape(20.dp)
internal val AppControlShape = RoundedCornerShape(12.dp)
internal val AppModalShape = RoundedCornerShape(24.dp)
internal val AppModalOuterPadding = 12.dp
internal val AppModalMaxHeight = 680.dp
internal val AppModalBlurRadius = 40.dp
internal val AppPagePadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 22.dp, 20.dp, 36.dp)
internal val AppSectionGap = 16.dp
internal val AppCardPadding = 18.dp
internal val AppPageTitleSize = 30.sp
internal val AppSectionTitleSize = 18.sp
internal val AppBodySize = 13.sp
internal val AppCaptionSize = 11.sp

internal enum class TrendRange(val label: String, val days: Long?) {
    DAYS_14("14天", 14),
    DAYS_30("30天", 30),
    DAYS_60("60天", 60),
    HALF_YEAR("半年", 183),
    ALL("至今", null)
}

internal enum class DashboardSection {
    NUTRITION, WEIGHT_TREND, MEALS, TIP
}

internal enum class AppTab(val label: String) { HOME("饮食计划"), TRAINING("训练计划"), PROFILE("我") }
