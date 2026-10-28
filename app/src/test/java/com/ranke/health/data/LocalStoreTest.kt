package com.ranke.health.data

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Locale
import com.ranke.health.R
import com.ranke.health.model.WorkoutDay
import com.ranke.health.model.DayMode
import com.ranke.health.model.WorkoutExercise
import com.ranke.health.model.MealPeriod
import com.ranke.health.model.exerciseDisplayName
import com.ranke.health.ui.i18n.LocalAppLocale
import com.ranke.health.ui.screen.WorkoutExerciseRowContent
import com.ranke.health.ui.screen.workoutRowStacks

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalStoreTest {
    @Test
    fun `invalid backup leaves existing Room and preference data unchanged`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("manniu-health.db")
        context.getSharedPreferences("ranke_records", Context.MODE_PRIVATE).edit().clear().commit()
        val store = LocalStore(context)
        val originalDay = WorkoutDay(
            date = LocalDate.of(2026, 8, 29),
            mode = DayMode.TRAINING,
            exercises = listOf(WorkoutExercise(1, "深蹲", 5, "5", "80kg"))
        )
        withContext(Dispatchers.IO) {
            store.addWeight(80.0)
            store.saveWorkoutDays(listOf(originalDay))
        }
        store.saveQuickFoodOrder(listOf("鸡胸肉", "米饭"))

        val damagedBackup = """
            {
              "schemaVersion": 1,
              "weights": [{"time":"2026-08-29T00:00:00Z","kg":81.0}],
              "quickFoodOrder": ["不应写入"],
              "workoutDays": [{"date":"not-a-date","mode":"TRAINING","exercises":[]}]
            }
        """.trimIndent()

        assertFalse(withContext(Dispatchers.IO) { store.importBackupJson(damagedBackup) })
        assertEquals(listOf(80.0), withContext(Dispatchers.IO) { store.weights().map { it.kilograms } })
        assertEquals(listOf(originalDay), withContext(Dispatchers.IO) { store.workoutDays() })
        assertEquals(listOf("鸡胸肉", "米饭"), store.quickFoodOrder())
        val backup = withContext(Dispatchers.IO) {
            store.addMeal("米饭", 232.0, MealPeriod.LUNCH, 51.0, 4.0, 1.0)
            store.exportBackupJson()
        }

        assertTrue(withContext(Dispatchers.IO) { store.importBackupJson(backup) })
        assertTrue(withContext(Dispatchers.IO) { store.importBackupJson(backup) })
        assertEquals(1, withContext(Dispatchers.IO) { store.weights().size })
        assertEquals(1, withContext(Dispatchers.IO) { store.meals().size })
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h1600dp-xhdpi")
class WorkoutExerciseLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    // 中文名短、英文名长（Romanian Deadlift / Bulgarian Split Squat），两种语言都要保证
    // 文字落在单元格内、不横向溢出不纵向溢出，编辑图标也不会被挤出卡片。
    @Test
    fun `exercise cells and edit icon stay inside card in both languages`() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        var screenWidth by mutableStateOf(360)
        var fontScale by mutableStateOf(1f)
        var screenDensity by mutableStateOf(1f)
        var locale by mutableStateOf(Locale.SIMPLIFIED_CHINESE)
        var exercise by mutableStateOf(WorkoutExercise(1, "高抓（膝上）", 5, "3", "48 kg (60%)"))
        compose.setContent {
            val configuration = remember(locale) {
                Configuration(appContext.resources.configuration).apply { setLocale(locale) }
            }
            val localizedContext = remember(configuration) { appContext.createConfigurationContext(configuration) }
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalContext provides localizedContext,
                LocalAppLocale provides locale,
                LocalDensity provides Density(screenDensity, fontScale)
            ) {
                MaterialTheme {
                    Box(Modifier.width(screenWidth.dp).padding(horizontal = 20.dp)) {
                        Card(Modifier.fillMaxWidth()) {
                            WorkoutExerciseRowContent(exercise, Modifier.padding(16.dp).testTag("exercise-row"))
                        }
                    }
                }
            }
        }
        val examples = listOf(
            WorkoutExercise(1, "高抓（膝上）", 5, "3", "48 kg (60%)"),
            WorkoutExercise(2, "抓举支撑", 3, "3", "52 kg (65%)"),
            WorkoutExercise(3, "前蹲", 3, "5", "78 kg (65%)"),
            WorkoutExercise(4, "高位下拉", 3, "12", "自定义"),
            WorkoutExercise(5, "保加利亚分腿蹲", 3, "20秒", "自定义"),
            WorkoutExercise(6, "髋屈肌拉伸＋胸椎旋转", 2, "1", "123.5kg（85%）"),
            WorkoutExercise(7, "罗马尼亚硬拉", 4, "10", "102.5 kg (75%)"),
            WorkoutExercise(8, "哥本哈根侧支撑", 3, "30秒", "自重")
        )
        for (targetLocale in listOf(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH)) {
            val english = targetLocale.language == "en"
            val resources = appContext.createConfigurationContext(
                Configuration(appContext.resources.configuration).apply { setLocale(targetLocale) }
            ).resources
            val editLabel = resources.getString(R.string.train_edit_cd)
            for (density in listOf(1f, 2f)) {
            for (width in listOf(320, 360, 371, 372, 373, 411)) {
                for (scale in listOf(1f, 1.3f, 1.5f, 2f)) {
                    for (sample in examples) {
                        compose.runOnIdle {
                            locale = targetLocale
                            screenWidth = width
                            screenDensity = density
                            fontScale = scale
                            exercise = sample
                        }
                        compose.waitForIdle()
                        val label = "${targetLocale.language} width=$width density=$density fontScale=$scale exercise=${sample.name}"
                        val row = compose.onNodeWithTag("exercise-row").getUnclippedBoundsInRoot()
                        assertEquals(label, (width - 72).toFloat(), (row.right - row.left).value, 1f)
                        val displayName = exerciseDisplayName(sample.name, english)
                        val setsReps = resources.getString(R.string.train_sets_reps).format(sample.sets, sample.reps)
                        val labels = listOf(displayName, setsReps, sample.weight)
                        val textBounds = labels.mapIndexed { index, text ->
                            val node = compose.onNodeWithText(text, useUnmergedTree = true)
                            val bounds = node.getUnclippedBoundsInRoot()
                            assertTrue(label, bounds.left >= row.left - 1.dp && bounds.right <= row.right + 1.dp)
                            assertTrue(label, bounds.top >= row.top - 1.dp && bounds.bottom <= row.bottom + 1.dp)
                            val layouts = mutableListOf<TextLayoutResult>()
                            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                            assertTrue(label, layouts.isNotEmpty())
                            // 动作名允许省略号截断（一行 / 两行由布局决定），数值单元格必须完整放下。
                            // 断言用“单行自然宽度 <= 单元格可用宽度”而不是 didOverflowWidth：
                            // 后者在该 Robolectric 版本里连 "Lv" 放进 124px 的框都会误报 true。
                            // 超小屏 + 超大系统字号 + 超长自定义重量缩到最小字号仍放不下时，
                            // 契约是退化成省略号（可见地少了尾巴），绝不允许硬裁。
                            if (index > 0) {
                                layouts.forEach {
                                    val available = it.layoutInput.constraints.maxWidth
                                    val intrinsic = it.multiParagraph.maxIntrinsicWidth
                                    assertTrue(
                                        "$label text=$text exceeds its cell" +
                                            " intrinsic=$intrinsic available=$available" +
                                            " fontSize=${it.layoutInput.style.fontSize}" +
                                            " overflow=${it.layoutInput.overflow}" +
                                            " bounds=${bounds.left}..${bounds.right} row=${row.left}..${row.right}",
                                        intrinsic <= available + 1f ||
                                            it.layoutInput.overflow == TextOverflow.Ellipsis
                                    )
                                    assertEquals("$label text=$text must stay on one line", 1, it.lineCount)
                                }
                            }
                            bounds
                        }
                        val icon = compose.onNodeWithContentDescription(editLabel, useUnmergedTree = true).getUnclippedBoundsInRoot()
                        assertTrue(label, icon.left >= row.left && icon.right <= row.right + 1.dp)
                        assertTrue(label, icon.top >= row.top && icon.bottom <= row.bottom + 1.dp)
                        assertTrue(label, textBounds[1].right < textBounds[2].left)
                        if (workoutRowStacks((width - 72).dp, scale, english)) {
                            assertTrue(label, textBounds[0].bottom < textBounds[1].top)
                        } else {
                            assertTrue(label, textBounds[0].right < textBounds[1].left)
                            assertTrue(label, textBounds[2].right < icon.left)
                        }
                    }
                }
            }
            }
        }
    }
}
