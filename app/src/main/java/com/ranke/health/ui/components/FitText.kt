package com.ranke.health.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 单行自适应字号文本。
 *
 * 英文文案普遍比中文长（"Romanian Deadlift" vs "罗马尼亚硬拉"），在固定宽度的
 * 单元格里按原字号排版会被裁切或把容器撑变形。这里在可用宽度内二分搜索最大可容纳
 * 的字号，容器尺寸完全由外部决定，文字只会缩小、不会溢出，也不会改变控件大小。
 *
 * 测量使用继承而来的 LocalTextStyle，与最终渲染的样式保持一致，
 * 避免因字距、行高差异导致测出来能放下、画出来却溢出。
 */
@Composable
internal fun FitText(
    text: String,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Start
) {
    val baseStyle = LocalTextStyle.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val maxWidth = constraints.maxWidth
        val fittedSize = remember(text, maxWidth, maxFontSize, minFontSize, fontWeight, baseStyle) {
            if (maxWidth <= 0 || maxWidth == Constraints.Infinity) {
                maxFontSize
            } else {
                fitFontSize(measurer, text, maxWidth, maxFontSize, minFontSize, baseStyle, fontWeight)
            }
        }
        Text(
            text = text,
            color = color,
            style = baseStyle,
            fontSize = fittedSize,
            fontWeight = fontWeight,
            maxLines = 1,
            // 极端情况（超小屏 + 超大系统字号 + 超长自定义重量）缩到最小字号仍放不下时，
            // 退化为省略号，而不是把字裁掉一半。
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign
        )
    }
}

private fun fitFontSize(
    measurer: TextMeasurer,
    text: String,
    maxWidth: Int,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    baseStyle: TextStyle,
    fontWeight: FontWeight?
): TextUnit {
    fun fits(size: Float): Boolean {
        val layout = measurer.measure(
            text = AnnotatedString(text),
            style = baseStyle.merge(TextStyle(fontSize = size.sp, fontWeight = fontWeight)),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            constraints = Constraints(maxWidth = maxWidth)
        )
        return !layout.hasVisualOverflow && layout.size.width <= maxWidth
    }

    if (fits(maxFontSize.value)) return maxFontSize
    if (!fits(minFontSize.value)) return minFontSize

    var low = minFontSize.value
    var high = maxFontSize.value
    repeat(8) {
        val mid = (low + high) / 2f
        if (fits(mid)) low = mid else high = mid
    }
    return low.sp
}
