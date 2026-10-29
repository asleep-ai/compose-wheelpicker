package ai.asleep.compose.wheelpicker

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WheelPickerTextColorTest {

    @get:Rule
    val rule = createComposeRule()

    private fun countPixels(tag: String, match: (Color) -> Boolean): Int {
        val pixels = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
        var count = 0
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                if (match(pixels[x, y])) count++
            }
        }
        return count
    }

    private fun isRed(c: Color) = c.red > 0.8f && c.green < 0.25f && c.blue < 0.25f

    private fun setPicker(textColor: Color?, textStyle: TextStyle) {
        rule.setContent {
            MaterialTheme {
                if (textColor == null) {
                    WheelPicker(
                        items = listOf("88", "88", "88"),
                        initialIndex = 1,
                        onValueChange = { _, _ -> },
                        modifier = Modifier.testTag("wheel"),
                        itemHeight = 64.dp,
                        textStyle = textStyle,
                        cylindrical = false,
                        fadeEdges = false,
                    )
                } else {
                    WheelPicker(
                        items = listOf("88", "88", "88"),
                        initialIndex = 1,
                        onValueChange = { _, _ -> },
                        modifier = Modifier.testTag("wheel"),
                        itemHeight = 64.dp,
                        textStyle = textStyle,
                        textColor = textColor,
                        cylindrical = false,
                        fadeEdges = false,
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun textColor_isAppliedToItems() {
        setPicker(textColor = Color.Red, textStyle = TextStyle(fontSize = 40.sp))

        assertTrue("expected red item text", countPixels("wheel", ::isRed) > 50)
    }

    @Test
    fun textColor_takesPrecedenceOverTextStyleColor() {
        setPicker(textColor = Color.Red, textStyle = TextStyle(fontSize = 40.sp, color = Color.Blue))

        assertTrue("expected red item text", countPixels("wheel", ::isRed) > 50)
        assertEquals(
            "textStyle.color should be overridden by textColor",
            0,
            countPixels("wheel") { it.blue > 0.8f && it.red < 0.25f && it.green < 0.25f },
        )
    }

    @Test
    fun defaultTextColor_isNotRed() {
        setPicker(textColor = null, textStyle = TextStyle(fontSize = 40.sp))

        assertEquals(0, countPixels("wheel", ::isRed))
        assertTrue("expected some item text", countPixels("wheel") { it.alpha > 0.5f && it.luminanceDark() } > 50)
    }
}

private fun Color.luminanceDark(): Boolean = red < 0.4f && green < 0.4f && blue < 0.4f
