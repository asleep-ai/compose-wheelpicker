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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
 *   기본 [TimeFormat.SYSTEM]은 기기 설정(사용자가 고른 값, 없으면 로케일 관례)을 따른다.
 * @param periodPosition AM/PM 휠의 위치. 12시간제일 때만 유효.
 *   기본 [PeriodPosition.LOCALE]은 [locale]의 시간 패턴에서 오전/오후가 시 앞인지 뒤인지로 정한다.
 * @param locale AM/PM 표기와 기본 숫자 포맷에 사용할 로케일. 기본은 시스템 로케일.
 * @param hourFormatter 시 숫자를 문자열로 변환하는 함수. null이면 12시간제는 "1".."12", 24시간제는 "00".."23"
 *   ([locale]의 숫자 모양). [TimeFormat.SYSTEM]이면 시 앞 0도 플랫폼처럼 [locale]의 시간 패턴을 따른다
 *   (시 글자가 `hh`·`HH`처럼 두 글자면 "07", 한 글자면 "7").
 *   12시간제에서 패턴의 시 글자가 `K`(일본어 등)면 시 휠도 플랫폼처럼 "0".."11"이 된다 (午前 0 = 00시, 午後 0 = 12시).
 * @param minuteFormatter 분 숫자를 문자열로 변환하는 함수. null이면 "00".."59" ([locale]의 숫자 모양).
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
    format: TimeFormat = TimeFormat.SYSTEM,
    periodPosition: PeriodPosition = PeriodPosition.LOCALE,
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
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val is24Hour = remember(format, context, configuration) {
        when (format) {
            TimeFormat.HOUR_12 -> false
            TimeFormat.HOUR_24 -> true
            TimeFormat.SYSTEM -> android.text.format.DateFormat.is24HourFormat(context)
        }
    }
    val periodAtStart = remember(periodPosition, locale) {
        when (periodPosition) {
            PeriodPosition.START -> true
            PeriodPosition.END -> false
            PeriodPosition.LOCALE -> localePeriodPosition(locale) == PeriodPosition.START
        }
    }
    // 플랫폼 TimePicker처럼 12시간제 패턴의 시 글자가 'K'면 시 휠을 0..11로 둔다. HOUR_12는 항상 1..12.
    val hourStartsAtZero = remember(format, is24Hour, locale) {
        format == TimeFormat.SYSTEM && !is24Hour && localeHourStartsAtZero(locale)
    }
    val initialHour = remember(is24Hour, hourStartsAtZero, initialTime) {
        displayHourOf(initialTime.hour, is24Hour, hourStartsAtZero)
    }
    val initialPeriod = remember(initialTime) {
        if (initialTime.hour < 12) Period.AM else Period.PM
    }

    var selectedHour by remember(is24Hour, hourStartsAtZero) { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialTime.minute) }
    var selectedPeriod by remember { mutableStateOf(initialPeriod) }

    val hourItems = remember(is24Hour, hourStartsAtZero, format, locale, hourFormatter) {
        val range = hourRange(is24Hour, hourStartsAtZero)
        val resolved: (Int) -> String = hourFormatter ?: run {
            // HOUR_12·HOUR_24는 기존 기본값(12h "7", 24h "07")을 유지한다.
            val twoDigits = if (format == TimeFormat.SYSTEM) localeHourHasTwoDigits(is24Hour, locale) else is24Hour
            defaultHourFormatter(twoDigits, locale)
        }
        range.map(resolved)
    }
    val periodItems = remember(localizedPeriods) {
        listOf(localizedPeriods.am, localizedPeriods.pm)
    }
    val minuteItems = remember(locale, minuteFormatter) {
        val resolved: (Int) -> String = minuteFormatter ?: defaultMinuteFormatter(locale)
        (0..59).map(resolved)
    }

    val firstHour = hourRange(is24Hour, hourStartsAtZero).first
    val hourInitialIndex = initialHour - firstHour
    val minuteInitialIndex = initialTime.minute
    val periodInitialIndex = if (initialPeriod == Period.AM) 0 else 1

    // Stable callbacks so child WheelPickers can skip recomposition.
    val onHourChange = remember(firstHour) {
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
    LaunchedEffect(is24Hour, hourStartsAtZero) {
        snapshotFlow {
            val hour24 = hourOfDayOf(selectedHour, isPm = selectedPeriod == Period.PM, is24Hour)
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

            if (!is24Hour && periodAtStart) periodWheel()

            key(is24Hour, hourStartsAtZero) {
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

            if (!is24Hour && !periodAtStart) periodWheel()

        }
    }
}

/** 시간 표기 방식. */
public enum class TimeFormat {
    /** 12시간제. AM/PM 휠을 함께 보여준다. */
    HOUR_12,

    /** 24시간제. AM/PM 휠이 숨겨진다. */
    HOUR_24,

    /**
     * 기기 설정을 따른다 ([android.text.format.DateFormat.is24HourFormat]).
     * 사용자가 설정에서 12/24시간제를 골랐으면 그 값, 고르지 않았으면 앱 로케일의 관례.
     * `hourFormatter`가 없으면 시 앞 0도 `locale`의 시간 패턴을 따른다(플랫폼 `TimePicker`와 같음).
     */
    SYSTEM,
}

/** 12시간제에서 AM/PM 휠이 표시될 위치. */
public enum class PeriodPosition {
    /** 시/분 앞쪽(좌측)에 표시. */
    START,

    /** 시/분 뒤쪽(우측)에 표시. */
    END,

    /**
     * `locale`의 12시간제 패턴(`DateFormat.getBestDateTimePattern(locale, "hm")`)을 따른다.
     * 오전/오후가 시보다 앞이면 [START], 뒤면 [END]. 예: ko·ja → START, en·es·hi → END.
     */
    LOCALE,
}

private enum class Period { AM, PM }

private data class LocalizedPeriods(val am: String, val pm: String)

/** 기본 시 표기. [twoDigits]면 "07", 아니면 "7" — 둘 다 [locale]의 숫자 모양. */
internal fun defaultHourFormatter(twoDigits: Boolean, locale: Locale): (Int) -> String =
    if (twoDigits) {
        { h -> String.format(locale, "%02d", h) }
    } else {
        { h -> String.format(locale, "%d", h) }
    }

/** 기본 분 표기. "00".."59", [locale]의 숫자 모양. */
internal fun defaultMinuteFormatter(locale: Locale): (Int) -> String =
    { m -> String.format(locale, "%02d", m) }

/**
 * 플랫폼 `TimePicker`(spinner)와 같은 방식으로 시 앞 0을 정한다:
 * `DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm")`의 시 글자가 두 글자인지.
 */
private fun localeHourHasTwoDigits(is24Hour: Boolean, locale: Locale): Boolean =
    hourHasTwoDigits(android.text.format.DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"))

/**
 * 시간 패턴에서 처음 나오는 시 글자(`h`·`H`·`K`·`k`)가 같은 글자 두 개(`hh`·`HH`…)면 true.
 * 작은따옴표 안의 글자는 건너뛴다. 시 글자가 없으면 false.
 */
internal fun hourHasTwoDigits(pattern: String): Boolean {
    var inQuote = false
    pattern.forEachIndexed { i, c ->
        if (c == '\'') {
            inQuote = !inQuote
        } else if (!inQuote && (c == 'h' || c == 'H' || c == 'K' || c == 'k')) {
            return pattern.getOrNull(i + 1) == c
        }
    }
    return false
}

/**
 * 시 휠에 보이는 범위. 24시간제 0..23, 12시간제는 [startsAtZero]면 0..11(`K`), 아니면 1..12(`h`).
 */
internal fun hourRange(is24Hour: Boolean, startsAtZero: Boolean): IntRange = when {
    is24Hour -> 0..23
    startsAtZero -> 0..11
    else -> 1..12
}

/** 0..23시를 시 휠에 보일 숫자로. 12시간제에서 0시·12시는 [startsAtZero]면 0, 아니면 12. */
internal fun displayHourOf(hourOfDay: Int, is24Hour: Boolean, startsAtZero: Boolean): Int {
    if (is24Hour) return hourOfDay
    val h = hourOfDay % 12
    return if (h == 0 && !startsAtZero) 12 else h
}

/** 시 휠 숫자와 오전/오후를 0..23시로. 1..12와 0..11 둘 다 `% 12`로 같은 값이 된다. */
internal fun hourOfDayOf(displayHour: Int, isPm: Boolean, is24Hour: Boolean): Int =
    if (is24Hour) displayHour else displayHour % 12 + if (isPm) 12 else 0

private fun localeHourStartsAtZero(locale: Locale): Boolean =
    hourStartsAtZero(android.text.format.DateFormat.getBestDateTimePattern(locale, "hm"))

/**
 * 12시간제 패턴에서 처음 나오는 시 글자가 `K`(0~11)면 true. 작은따옴표 안의 글자는 건너뛴다.
 * AOSP `TimePickerSpinnerDelegate.updateHourControl`의 `mHourFormat == 'K'`와 같은 판단.
 */
internal fun hourStartsAtZero(pattern: String): Boolean {
    var inQuote = false
    pattern.forEach { c ->
        if (c == '\'') {
            inQuote = !inQuote
        } else if (!inQuote && (c == 'h' || c == 'H' || c == 'K' || c == 'k')) {
            return c == 'K'
        }
    }
    return false
}

private fun localePeriodPosition(locale: Locale): PeriodPosition =
    periodPositionOf(android.text.format.DateFormat.getBestDateTimePattern(locale, "hm"))

/**
 * 시간 패턴에서 오전/오후(`a`·`b`·`B`)가 시(`h`·`K`·`H`·`k`)보다 앞이면 START, 뒤면 END.
 * 작은따옴표 안의 글자는 건너뛴다. 오전/오후 기호가 없으면 기본값 START.
 */
internal fun periodPositionOf(pattern: String): PeriodPosition {
    var inQuote = false
    var periodIndex = -1
    var hourIndex = -1
    pattern.forEachIndexed { i, c ->
        if (c == '\'') {
            inQuote = !inQuote
        } else if (!inQuote) {
            if (periodIndex < 0 && (c == 'a' || c == 'b' || c == 'B')) periodIndex = i
            if (hourIndex < 0 && (c == 'h' || c == 'K' || c == 'H' || c == 'k')) hourIndex = i
        }
    }
    if (periodIndex < 0 || hourIndex < 0) return PeriodPosition.START
    return if (periodIndex < hourIndex) PeriodPosition.START else PeriodPosition.END
}

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