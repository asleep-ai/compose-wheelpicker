package ai.asleep.compose.wheelpicker.consumercheck

import ai.asleep.compose.wheelpicker.TimePicker
import ai.asleep.compose.wheelpicker.WheelPicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Runs the published artifact, compiled by Kotlin 1.9.24 + Compose compiler 1.5.14 against
// Compose 1.7.2, on a real device/emulator. Use an API 26 (minSdk) emulator.
//
// Drags are 1.5 items so they clear touch slop and the snap threshold. The exact item the
// pager snaps to depends on the injected gesture, so the tests assert direction and that the
// callbacks reach the app, not an exact index.
@RunWith(AndroidJUnit4::class)
class PickerInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun wheelPicker_dragUp_selectsLaterItem() {
        var selected: Pair<Int, String>? = null
        rule.setContent {
            MaterialTheme {
                WheelPicker(
                    items = (0..12).map { "$it" },
                    initialIndex = 6,
                    onValueChange = { index, value -> selected = index to value },
                    modifier = Modifier.testTag("wheel"),
                    itemHeight = 48.dp,
                )
            }
        }

        rule.onNodeWithTag("wheel").performTouchInput {
            swipe(start = center, end = center - Offset(0f, 72.dp.toPx()), durationMillis = 600)
        }
        rule.waitForIdle()

        assertNotNull(selected)
        val (index, value) = selected!!
        assertTrue("expected index > 6, was $index", index > 6)
        assertEquals("$index", value)
    }

    @Test
    fun timePicker_dragMinuteUp_emitsLaterMinute() {
        var time: LocalTime? = null
        rule.setContent {
            MaterialTheme {
                TimePicker(
                    initialTime = LocalTime.of(7, 30),
                    onTimeChange = { time = it },
                    itemHeight = 48.dp,
                )
            }
        }

        rule.onNodeWithText("30").performTouchInput {
            swipe(start = center, end = center - Offset(0f, 72.dp.toPx()), durationMillis = 600)
        }
        rule.waitForIdle()

        val t = time
        assertNotNull(t)
        assertEquals(7, t!!.hour)
        assertTrue("expected minute > 30, was ${t.minute}", t.minute > 30)
    }

    @Test
    fun timePicker_dragHourDown_emitsEarlierHour() {
        var time: LocalTime? = null
        rule.setContent {
            MaterialTheme {
                TimePicker(
                    initialTime = LocalTime.of(7, 30),
                    onTimeChange = { time = it },
                    itemHeight = 48.dp,
                )
            }
        }

        rule.onNodeWithText("7").performTouchInput {
            swipe(start = center, end = center + Offset(0f, 72.dp.toPx()), durationMillis = 600)
        }
        rule.waitForIdle()

        val t = time
        assertNotNull(t)
        assertEquals(30, t!!.minute)
        assertTrue("expected hour < 7, was ${t.hour}", t.hour < 7)
    }
}
