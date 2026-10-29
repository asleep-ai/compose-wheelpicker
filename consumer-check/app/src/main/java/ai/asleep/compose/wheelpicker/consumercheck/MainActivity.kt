package ai.asleep.compose.wheelpicker.consumercheck

import ai.asleep.compose.wheelpicker.TimePicker
import ai.asleep.compose.wheelpicker.WheelPicker
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import java.time.LocalTime

// Compiled by Kotlin 1.9.24 + Compose compiler 1.5.14 against the published artifact.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column {
                    Text("compose-wheelpicker consumer check")
                    WheelPicker(
                        items = (0..12).map { "$it" },
                        initialIndex = 6,
                        onValueChange = { _, _ -> },
                        itemHeight = 48.dp,
                        repeatable = true,
                    )
                    TimePicker(
                        initialTime = LocalTime.of(7, 30),
                        onTimeChange = {},
                        itemHeight = 48.dp,
                    )
                }
            }
        }
    }
}
