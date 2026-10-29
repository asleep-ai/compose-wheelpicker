package ai.asleep.compose.wheelpicker.sample

import ai.asleep.compose.wheelpicker.PeriodPosition
import ai.asleep.compose.wheelpicker.TimeFormat
import ai.asleep.compose.wheelpicker.TimePicker
import ai.asleep.compose.wheelpicker.WheelPicker
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.util.Locale

private val SUPPORTED_LOCALES = listOf(
    "KO" to Locale.KOREAN,
    "EN" to Locale.ENGLISH,
    "JA" to Locale.JAPANESE,
    "ZH" to Locale.CHINESE,
)

private val FRUITS = listOf("apple", "banana", "cherry", "durian", "elderberry", "fig")

private val DEFAULT_TIME: LocalTime = LocalTime.of(7, 30)

private enum class SampleTab(val label: String) {
    TIME_PICKER("TimePicker"),
    WHEEL_PICKER("WheelPicker"),
}

@Composable
fun SampleScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var selectedTab by remember { mutableStateOf(SampleTab.TIME_PICKER) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            SampleTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label) },
                )
            }
        }

        when (selectedTab) {
            SampleTab.TIME_PICKER -> TimePickerTab()
            SampleTab.WHEEL_PICKER -> WheelPickerTab()
        }
    }
}

@Composable
private fun TimePickerTab() {
    var format by remember { mutableStateOf(TimeFormat.SYSTEM) }
    var periodPosition by remember { mutableStateOf(PeriodPosition.LOCALE) }
    var showIndicator by remember { mutableStateOf(true) }
    var useLocalizedSuffixes by remember { mutableStateOf(false) }
    var visibleItemCount by remember { mutableIntStateOf(3) }
    var locale by remember { mutableStateOf(Locale.KOREAN) }
    var cylindrical by remember { mutableStateOf(true) }
    var fadeEdges by remember { mutableStateOf(true) }
    var showSeparator by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localizedResources = remember(locale, context, baseConfiguration) {
        val config = Configuration(baseConfiguration).apply {
            setLocale(locale)
        }
        context.createConfigurationContext(config).resources
    }
    val hourSuffix = localizedResources.getString(R.string.hour_suffix)
    val minuteSuffix = localizedResources.getString(R.string.minute_suffix)

    val hourFormatter = remember(useLocalizedSuffixes, hourSuffix) {
        if (useLocalizedSuffixes) { h: Int -> "$h$hourSuffix" } else null
    }
    val minuteFormatter = remember(useLocalizedSuffixes, minuteSuffix) {
        if (useLocalizedSuffixes) { m: Int -> "$m$minuteSuffix" } else null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TimePickerSection(
            visibleItemCount = visibleItemCount,
            format = format,
            periodPosition = periodPosition,
            locale = locale,
            hourFormatter = hourFormatter,
            minuteFormatter = minuteFormatter,
            showIndicator = showIndicator,
            cylindrical = cylindrical,
            fadeEdges = fadeEdges,
            showSeparator = showSeparator,
        )

        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TimePickerControls(
                format = format,
                onFormatChange = { format = it },
                periodPosition = periodPosition,
                onPeriodPositionChange = { periodPosition = it },
                visibleItemCount = visibleItemCount,
                onVisibleItemCountChange = { visibleItemCount = it },
                showIndicator = showIndicator,
                onShowIndicatorChange = { showIndicator = it },
                useLocalizedSuffixes = useLocalizedSuffixes,
                onUseLocalizedSuffixesChange = { useLocalizedSuffixes = it },
                locale = locale,
                onLocaleChange = { locale = it },
                cylindrical = cylindrical,
                onCylindricalChange = { cylindrical = it },
                fadeEdges = fadeEdges,
                onFadeEdgesChange = { fadeEdges = it },
                showSeparator = showSeparator,
                onShowSeparatorChange = { showSeparator = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WheelPickerTab() {
    var selectedFruitIndex by remember { mutableIntStateOf(-1) }
    var selectedFruit by remember { mutableStateOf("apple") }
    var cylindrical by remember { mutableStateOf(true) }
    var fadeEdges by remember { mutableStateOf(true) }
    var repeatable by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectedFruitLabel(
                fruit = { selectedFruit },
                index = { selectedFruitIndex },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                WheelPicker(
                    items = FRUITS,
                    initialIndex = 0,
                    onValueChange = { index, value ->
                        selectedFruitIndex = index
                        selectedFruit = value
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall,
                    repeatable = repeatable,
                    cylindrical = cylindrical,
                    fadeEdges = fadeEdges,
                )
            }
        }

        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Switch(checked = cylindrical, onCheckedChange = { cylindrical = it })
                Text("Cylindrical 3D", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Switch(checked = fadeEdges, onCheckedChange = { fadeEdges = it })
                Text("Fade edges", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Switch(checked = repeatable, onCheckedChange = { repeatable = it })
                Text("Repeatable (infinite scroll)", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimePickerControls(
    format: TimeFormat,
    onFormatChange: (TimeFormat) -> Unit,
    periodPosition: PeriodPosition,
    onPeriodPositionChange: (PeriodPosition) -> Unit,
    visibleItemCount: Int,
    onVisibleItemCountChange: (Int) -> Unit,
    showIndicator: Boolean,
    onShowIndicatorChange: (Boolean) -> Unit,
    useLocalizedSuffixes: Boolean,
    onUseLocalizedSuffixesChange: (Boolean) -> Unit,
    locale: Locale,
    onLocaleChange: (Locale) -> Unit,
    cylindrical: Boolean,
    onCylindricalChange: (Boolean) -> Unit,
    fadeEdges: Boolean,
    onFadeEdgesChange: (Boolean) -> Unit,
    showSeparator: Boolean,
    onShowSeparatorChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ControlLabel("Locale")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SUPPORTED_LOCALES.forEach { (label, option) ->
                FilterChip(
                    selected = locale == option,
                    onClick = { onLocaleChange(option) },
                    label = { Text(label) },
                )
            }
        }

        ControlLabel("Format")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeFormat.entries.forEach { option ->
                FilterChip(
                    selected = format == option,
                    onClick = { onFormatChange(option) },
                    label = {
                        Text(
                            when (option) {
                                TimeFormat.HOUR_12 -> "12h"
                                TimeFormat.HOUR_24 -> "24h"
                                TimeFormat.SYSTEM -> "System"
                            }
                        )
                    },
                )
            }
        }

        ControlLabel("AM/PM position")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodPosition.entries.forEach { option ->
                FilterChip(
                    selected = periodPosition == option,
                    onClick = { onPeriodPositionChange(option) },
                    label = { Text(option.name) },
                    enabled = format != TimeFormat.HOUR_24,
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }

        ControlLabel("Visible items: $visibleItemCount")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onVisibleItemCountChange(visibleItemCount - 2) },
                enabled = visibleItemCount > 1,
            ) { Text("−") }
            OutlinedButton(
                onClick = { onVisibleItemCountChange(visibleItemCount + 2) },
                enabled = visibleItemCount < 9,
            ) { Text("+") }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Switch(checked = showIndicator, onCheckedChange = onShowIndicatorChange)
            Text("Show selection indicator", style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Switch(
                checked = useLocalizedSuffixes,
                onCheckedChange = onUseLocalizedSuffixesChange,
            )
            Text(
                "Localized suffixes (strings.xml)",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Switch(checked = cylindrical, onCheckedChange = onCylindricalChange)
            Text("Cylindrical 3D", style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Switch(checked = fadeEdges, onCheckedChange = onFadeEdgesChange)
            Text("Fade edges", style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Switch(checked = showSeparator, onCheckedChange = onShowSeparatorChange)
            Text("Show separator (:)", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TimePickerSection(
    visibleItemCount: Int,
    format: TimeFormat,
    periodPosition: PeriodPosition,
    locale: Locale,
    hourFormatter: ((Int) -> String)?,
    minuteFormatter: ((Int) -> String)?,
    showIndicator: Boolean,
    cylindrical: Boolean,
    fadeEdges: Boolean,
    showSeparator: Boolean,
) {
    var time by remember { mutableStateOf(DEFAULT_TIME) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SelectedTimeLabel(time = { time })
        TimePicker(
            initialTime = DEFAULT_TIME,
            onTimeChange = { time = it },
            modifier = Modifier.fillMaxWidth(),
            visibleItemCount = visibleItemCount,
            format = format,
            periodPosition = periodPosition,
            locale = locale,
            hourFormatter = hourFormatter,
            minuteFormatter = minuteFormatter,
            dividerThickness = if (showIndicator) 1.dp else null,
            cylindrical = cylindrical,
            fadeEdges = fadeEdges,
            separator = if (showSeparator) ":" else null,
        )
    }
}

@Composable
private fun SelectedTimeLabel(time: () -> LocalTime) {
    Text("Selected: ${time()}", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun SelectedFruitLabel(
    fruit: () -> String,
    index: () -> Int,
) {
    Text(
        "Selected: ${fruit()}, Index: ${index()}",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ControlLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}