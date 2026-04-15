package ai.asleep.compose.wheelpicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.DateFormatSymbols
import java.time.LocalTime
import java.util.Locale

/**
 * 시각 선택기. 12시간제에서는 AM/PM + 시 + 분, 24시간제에서는 시 + 분만 보여준다.
 *
 * @param initialTime 초기 시각.
 * @param onTimeChange 선택이 바뀔 때마다 호출.
 * @param modifier Modifier.
 * @param visibleItemCount 각 휠에 보일 아이템 개수 (중앙 정렬을 위해 홀수만 허용).
 * @param itemHeight 아이템 하나의 높이. null이면 텍스트 스타일에서 자동으로 계산한다.
 * @param format 12/24 시간제. [TimeFormat.HOUR_24]면 AM/PM 휠이 숨겨진다.
 * @param periodPosition AM/PM 휠의 위치. [TimeFormat.HOUR_12]일 때만 유효.
 * @param locale AM/PM 표기와 기본 숫자 포맷에 사용할 로케일. 기본은 시스템 로케일.
 * @param hourFormatter 시 숫자를 문자열로 변환하는 함수. null이면 12시간제는 "1".."12", 24시간제는 "00".."23".
 * @param minuteFormatter 분 숫자를 문자열로 변환하는 함수. null이면 "00".."59".
 * @param hourTextStyle 시(hour) 텍스트 스타일.
 * @param minuteTextStyle 분(minute) 텍스트 스타일.
 * @param periodTextStyle AM/PM 텍스트 스타일.
 * @param separator 시·분 사이에 표시할 구분자 문자열. null이면 숨긴다. 기본은 ":".
 * @param separatorTextStyle 구분자 텍스트 스타일.
 * @param dividerColor 선택 박스 상/하단 구분선 컬러.
 * @param dividerThickness 선택 박스 상/하단 구분선 두께. null이면 구분선을 숨긴다.
 * @param cylindrical true면 각 휠에 원통형 3D 변환을 적용한다.
 * @param fadeEdges true면 각 휠의 위/아래 가장자리를 그라데이션으로 페이드아웃한다.
 */
@Composable
public fun TimePicker(
    initialTime: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 3,
    itemHeight: Dp? = null,
    format: TimeFormat = TimeFormat.HOUR_12,
    periodPosition: PeriodPosition = PeriodPosition.START,
    locale: Locale = Locale.getDefault(),
    hourFormatter: ((Int) -> String)? = null,
    minuteFormatter: ((Int) -> String)? = null,
    hourTextStyle: TextStyle = MaterialTheme.typography.headlineLarge,
    minuteTextStyle: TextStyle = MaterialTheme.typography.headlineLarge,
    periodTextStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    separator: String? = ":",
    separatorTextStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    dividerColor: Color = MaterialTheme.colorScheme.outline,
    dividerThickness: Dp? = 1.dp,
    cylindrical: Boolean = true,
    fadeEdges: Boolean = true,
) {
    val localizedPeriods = remember(locale) { getLocalizedPeriods(locale) }
    val initialHour = remember(format, initialTime) {
        when (format) {
            TimeFormat.HOUR_12 -> when {
                initialTime.hour == 0 -> 12
                initialTime.hour > 12 -> initialTime.hour - 12
                else -> initialTime.hour
            }

            TimeFormat.HOUR_24 -> initialTime.hour
        }
    }
    val initialPeriod = remember(initialTime) {
        if (initialTime.hour < 12) Period.AM else Period.PM
    }

    var selectedHour by remember(format) { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialTime.minute) }
    var selectedPeriod by remember { mutableStateOf(initialPeriod) }

    val hourItems = remember(format, locale, hourFormatter) {
        val range = if (format == TimeFormat.HOUR_12) 1..12 else 0..23
        val resolved: (Int) -> String = hourFormatter ?: when (format) {
            TimeFormat.HOUR_12 -> { h -> "$h" }
            TimeFormat.HOUR_24 -> { h -> String.format(locale, "%02d", h) }
        }
        range.map(resolved)
    }
    val periodItems = remember(localizedPeriods) {
        listOf(localizedPeriods.am, localizedPeriods.pm)
    }
    val minuteItems = remember(locale, minuteFormatter) {
        val resolved: (Int) -> String = minuteFormatter
            ?: { m -> String.format(locale, "%02d", m) }
        (0..59).map(resolved)
    }

    val hourInitialIndex = if (format == TimeFormat.HOUR_12) initialHour - 1 else initialHour
    val minuteInitialIndex = initialTime.minute
    val periodInitialIndex = if (initialPeriod == Period.AM) 0 else 1

    // Stable callbacks so child WheelPickers can skip recomposition.
    val onHourChange = remember(format) {
        val firstHour = if (format == TimeFormat.HOUR_12) 1 else 0
        { index: Int, _: String -> selectedHour = firstHour + index }
    }
    val onMinuteChange = remember<(Int, String) -> Unit> {
        { index, _ -> selectedMinute = index }
    }
    val onPeriodChange = remember(localizedPeriods) {
        { _: Int, label: String ->
            selectedPeriod =
                if (label == localizedPeriods.am) Period.AM else Period.PM
        }
    }

    // State reads happen inside the snapshot coroutine, not in composition — so
    // scrolling one wheel won't invalidate the parent and recompose others.
    val currentOnTimeChange by rememberUpdatedState(onTimeChange)
    LaunchedEffect(format) {
        snapshotFlow {
            val hour24 = when (format) {
                TimeFormat.HOUR_24 -> selectedHour
                TimeFormat.HOUR_12 -> when (selectedPeriod) {
                    Period.AM -> if (selectedHour == 12) 0 else selectedHour
                    Period.PM -> if (selectedHour == 12) 12 else selectedHour + 12
                }
            }
            LocalTime.of(hour24, selectedMinute)
        }.drop(1).collect { currentOnTimeChange(it) }
    }

    val localDensity = LocalDensity.current
    val resolvedItemHeight =
        itemHeight ?: remember(periodTextStyle, hourTextStyle, minuteTextStyle, localDensity) {
            listOf(periodTextStyle, hourTextStyle, minuteTextStyle)
                .map { deriveItemHeight(it, localDensity) }
                .max()
        }

    // Match WheelPicker's cylindrical-aware container height so the outer Box hugs the
    // actual wheel footprint (no dead space at top/bottom for larger visibleItemCount).
    val containerRatio = wheelContainerRatio(visibleItemCount, peekItemCount = 1, cylindrical = cylindrical)
    val containerHeight = resolvedItemHeight * containerRatio
    Box(modifier = modifier.height(containerHeight)) {
        // 중앙 선택 박스 (상/하단 구분선)
        if (dividerThickness != null) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                HorizontalDivider(thickness = dividerThickness, color = dividerColor)
                Box(
                    modifier = Modifier
                        .height(resolvedItemHeight)
                        .fillMaxWidth()
                )
                HorizontalDivider(thickness = dividerThickness, color = dividerColor)
            }
        }

        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val periodWheel: @Composable RowScope.() -> Unit = {
                WheelPicker(
                    items = periodItems,
                    initialIndex = periodInitialIndex,
                    onValueChange = onPeriodChange,
                    modifier = Modifier.weight(1f),
                    visibleItemCount = visibleItemCount,
                    itemHeight = resolvedItemHeight,
                    textStyle = periodTextStyle,
                    cylindrical = cylindrical,
                    fadeEdges = fadeEdges,
                )
            }

            if (format == TimeFormat.HOUR_12 && periodPosition == PeriodPosition.START) periodWheel()

            key(format) {
                WheelPicker(
                    items = hourItems,
                    initialIndex = hourInitialIndex,
                    onValueChange = onHourChange,
                    modifier = Modifier.weight(1f),
                    visibleItemCount = visibleItemCount,
                    itemHeight = resolvedItemHeight,
                    repeatable = true,
                    textStyle = hourTextStyle,
                    cylindrical = cylindrical,
                    fadeEdges = fadeEdges,
                )
            }

            if (separator != null) {
                Text(
                    text = separator,
                    textAlign = TextAlign.Center,
                    style = separatorTextStyle
                )
            }

            WheelPicker(
                items = minuteItems,
                initialIndex = minuteInitialIndex,
                onValueChange = onMinuteChange,
                modifier = Modifier.weight(1f),
                visibleItemCount = visibleItemCount,
                itemHeight = resolvedItemHeight,
                repeatable = true,
                textStyle = minuteTextStyle,
                cylindrical = cylindrical,
                fadeEdges = fadeEdges,
            )

            if (format == TimeFormat.HOUR_12 && periodPosition == PeriodPosition.END) periodWheel()

        }
    }
}

/** 시간 표기 방식. */
public enum class TimeFormat {
    /** 12시간제. AM/PM 휠을 함께 보여준다. */
    HOUR_12,

    /** 24시간제. AM/PM 휠이 숨겨진다. */
    HOUR_24,
}

/** 12시간제에서 AM/PM 휠이 표시될 위치. */
public enum class PeriodPosition {
    /** 시/분 앞쪽(좌측)에 표시. */
    START,

    /** 시/분 뒤쪽(우측)에 표시. */
    END,
}

private enum class Period { AM, PM }

private data class LocalizedPeriods(val am: String, val pm: String)

private fun getLocalizedPeriods(locale: Locale): LocalizedPeriods {
    val symbols = DateFormatSymbols.getInstance(locale).amPmStrings
    return LocalizedPeriods(am = symbols[0], pm = symbols[1])
}

@Preview(showBackground = true, backgroundColor = 0xFFF)
@Composable
private fun TimePickerPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            TimePicker(initialTime = LocalTime.of(7, 30), onTimeChange = {})
        }
    }
}