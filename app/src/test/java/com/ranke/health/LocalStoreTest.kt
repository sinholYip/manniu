package com.ranke.health

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    @Test
    fun `exercise cells and edit icon stay inside card at multiple widths and font scales`() {
        var screenWidth by mutableStateOf(360)
        var fontScale by mutableStateOf(1f)
        var screenDensity by mutableStateOf(1f)
        var exercise by mutableStateOf(WorkoutExercise(1, "高抓（膝上）", 5, "3", "48 kg (60%)"))
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(screenDensity, fontScale)) {
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
            WorkoutExercise(5, "哥本哈根侧支撑", 3, "20秒", "自定义"),
            WorkoutExercise(6, "髋屈肌拉伸＋胸椎旋转", 2, "1", "123.5kg（85%）")
        )
        for (density in listOf(1f, 2f)) {
        for (width in listOf(320, 360, 371, 372, 373, 411)) {
            for (scale in listOf(1f, 1.3f, 1.5f, 2f)) {
                for (sample in examples) {
                    compose.runOnIdle {
                        screenWidth = width
                        screenDensity = density
                        fontScale = scale
                        exercise = sample
                    }
                    compose.waitForIdle()
                    val context = "width=$width density=$density fontScale=$scale exercise=${sample.name}"
                    val row = compose.onNodeWithTag("exercise-row").getUnclippedBoundsInRoot()
                    assertEquals(context, (width - 72).toFloat(), (row.right - row.left).value, 1f)
                    val labels = listOf(sample.name, "${sample.sets} 组 × ${sample.reps} 次", sample.weight)
                    val textBounds = labels.map { label ->
                        val node = compose.onNodeWithText(label, useUnmergedTree = true)
                        val bounds = node.getUnclippedBoundsInRoot()
                        assertTrue(context, bounds.left >= row.left - 1.dp && bounds.right <= row.right + 1.dp)
                        assertTrue(context, bounds.top >= row.top - 1.dp && bounds.bottom <= row.bottom + 1.dp)
                        val layouts = mutableListOf<TextLayoutResult>()
                        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                        assertTrue(context, layouts.isNotEmpty())
                        layouts.forEach {
                            assertFalse("$context label=$label overflows horizontally", it.didOverflowWidth)
                            assertFalse("$context label=$label overflows vertically", it.didOverflowHeight)
                        }
                        bounds
                    }
                    val icon = compose.onNodeWithContentDescription("编辑动作", useUnmergedTree = true).getUnclippedBoundsInRoot()
                    assertTrue(context, icon.left >= row.left && icon.right <= row.right + 1.dp)
                    assertTrue(context, icon.top >= row.top && icon.bottom <= row.bottom + 1.dp)
                    assertTrue(context, textBounds[1].right < textBounds[2].left)
                    if (row.right - row.left < 300.dp * scale) {
                        assertTrue(context, textBounds[0].bottom < textBounds[1].top)
                    } else {
                        assertTrue(context, textBounds[0].right < textBounds[1].left)
                        assertTrue(context, textBounds[2].right < icon.left)
                    }
                }
            }
        }
        }
    }
}
