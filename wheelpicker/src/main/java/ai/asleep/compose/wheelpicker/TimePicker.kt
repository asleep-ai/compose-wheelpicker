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
 *   기기의 12/24시간제 설정 변경은 Configuration 변경이 아니라서, 화면이 떠 있는 동안 바꾸면
 *   다시 컴포즈될 때(화면 재진입 등)까지 반영되지 않는다. 플랫폼 `TimePicker`도 같다.
 *   [hourCycle]을 넘기면 이 값은 쓰지 않는다.
 * @param periodPosition AM/PM 휠의 위치. 12시간제일 때만 유효.
 *   기본 [PeriodPosition.LOCALE]은 [locale]의 시간 패턴에서 오전/오후가 시 앞인지 뒤인지로 정한다.
 * @param locale AM/PM 표기, 기본 숫자 모양, 그리고 [TimeFormat.SYSTEM]·[PeriodPosition.LOCALE]일 때
 *   (또는 [hourCycle]을 넘겼을 때) 시간 패턴(오전/오후 위치, 시 앞 0, `K`(0~11) 여부)에 쓰는 로케일.
 *   기본은 시스템 로케일.
 * @param hourFormatter 시 숫자를 문자열로 변환하는 함수. null이면 기본 [TimeFormat.SYSTEM]이나 [hourCycle]을 넘겼을 때
 *   플랫폼처럼 [locale]의 시간 패턴을 따른다: 시 글자가 `hh`·`HH`처럼 두 글자면 "07", 한 글자면 "7".
 *   12시간제에서 시 글자가 `K`(일본어 등)면 시 휠이 "0".."11"이 된다 (午前 0 = 00시, 午後 0 = 12시).
 *   [hourCycle] 없이 [TimeFormat.HOUR_12]면 "1".."12", [TimeFormat.HOUR_24]면 "00".."23".
 *   숫자는 모두 [locale]의 숫자 모양.
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
 * @param hourCycle 시를 세는 법. null이 아니면 [format]과 기기 설정보다 우선한다 (기기 설정을 읽지 않는다).
 *   시 휠 범위는 [HourCycle.H11] 0..11, [HourCycle.H12] 1..12, [HourCycle.H23] 0..23, [HourCycle.H24] 1..24.
 *   시 앞 0은 [TimeFormat.SYSTEM]처럼 [locale]의 시간 패턴을 따르고, 오전/오후 위치는 [periodPosition]대로.
 *   null이면 [format]대로.
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
    hourCycle: HourCycle? = null,
) {
    val localizedPeriods = remember(locale) { getLocalizedPeriods(locale) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // 12시간제 패턴은 오전/오후 위치와 'K' 판단에 같이 쓰므로 필요할 때 한 번만 읽는다.
    val pattern12 = remember(locale) { lazy { localeTimePattern(locale, is24Hour = false) } }
    val cycle = remember(format, hourCycle, context, configuration, pattern12) {
        resolveHourCycle(
            format = format,
            hourCycle = hourCycle,
            systemIs24Hour = { android.text.format.DateFormat.is24HourFormat(context) },
            localeStartsAtZero = { pattern12.value.hourStartsAtZero },
        )
    }
    val is24Hour = cycle.is24Hour
    val periodAtStart = remember(periodPosition, pattern12) {
        when (periodPosition) {
            PeriodPosition.START -> true
            PeriodPosition.END -> false
            PeriodPosition.LOCALE -> pattern12.value.periodPosition == PeriodPosition.START
        }
    }
    val initialHour = remember(cycle, initialTime) {
        displayHourOf(initialTime.hour, cycle)
    }
    val initialPeriod = remember(initialTime) {
        if (initialTime.hour < 12) Period.AM else Period.PM
    }

    var selectedHour by remember(cycle) { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialTime.minute) }
    var selectedPeriod by remember { mutableStateOf(initialPeriod) }

    val hourItems = remember(cycle, format, hourCycle, locale, pattern12, hourFormatter) {
        val range = hourRange(cycle)
        val resolved: (Int) -> String = hourFormatter ?: run {
            // HOUR_12·HOUR_24는 기존 기본값(12h "7", 24h "07")을 유지한다.
            val twoDigits = when {
                !usesLocaleHourDigits(format, hourCycle) -> is24Hour
                is24Hour -> localeTimePattern(locale, is24Hour = true).hourTwoDigits
                else -> pattern12.value.hourTwoDigits
            }
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

    val firstHour = hourRange(cycle).first
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
    LaunchedEffect(cycle) {
        snapshotFlow {
            val hour24 = hourOfDayOf(selectedHour, isPm = selectedPeriod == Period.PM, cycle)
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

            key(cycle) {
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

/** 시를 세는 법. 이름은 CLDR·Unicode 확장 `hc`(h11·h12·h23·h24)와 같다. */
public enum class HourCycle {
    /** 12시간제, 0..11 (패턴 글자 `K`). 午前 0 = 00시, 午後 0 = 12시. */
    H11,

    /** 12시간제, 1..12 (패턴 글자 `h`). */
    H12,

    /** 24시간제, 0..23 (패턴 글자 `H`). */
    H23,

    /** 24시간제, 1..24 (패턴 글자 `k`). 자정은 24. */
    H24,
}

internal val HourCycle.is24Hour: Boolean
    get() = this == HourCycle.H23 || this == HourCycle.H24

/**
 * 시 휠이 쓸 [HourCycle]. [hourCycle]이 있으면 그대로 쓰고 기기·로케일을 읽지 않는다.
 * 없으면 [format]대로: HOUR_12 → H12, HOUR_24 → H23, SYSTEM → 기기 24시간이면 H23, 아니면 로케일 `K`면 H11·아니면 H12.
 */
internal fun resolveHourCycle(
    format: TimeFormat,
    hourCycle: HourCycle?,
    systemIs24Hour: () -> Boolean,
    localeStartsAtZero: () -> Boolean,
): HourCycle = hourCycle ?: when (format) {
    TimeFormat.HOUR_12 -> HourCycle.H12
    TimeFormat.HOUR_24 -> HourCycle.H23
    TimeFormat.SYSTEM -> when {
        systemIs24Hour() -> HourCycle.H23
        localeStartsAtZero() -> HourCycle.H11
        else -> HourCycle.H12
    }
}

/** 기본 시 표기의 앞 0을 로케일 패턴으로 정하는지. SYSTEM이나 [HourCycle]을 넘기면 true, HOUR_12·HOUR_24는 false. */
internal fun usesLocaleHourDigits(format: TimeFormat, hourCycle: HourCycle?): Boolean =
    hourCycle != null || format == TimeFormat.SYSTEM

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

/** 시간 패턴에서 읽은 값. 플랫폼 `TimePicker`(spinner)와 같은 기준이다. */
internal data class TimePattern(
    /** 처음 나오는 시 글자(`h`·`H`·`K`·`k`). 없으면 null. */
    val hourLetter: Char?,
    /** 시 글자가 같은 글자 두 개(`hh`·`HH`…)면 true → "07". */
    val hourTwoDigits: Boolean,
    /** 오전/오후(`a`·`b`·`B`)가 시보다 앞이면 START, 뒤면 END. 오전/오후 기호가 없으면 START. */
    val periodPosition: PeriodPosition,
) {
    /** 시 글자가 `K`(0~11)인지. AOSP `TimePickerSpinnerDelegate.updateHourControl`의 `mHourFormat == 'K'`와 같은 판단. */
    val hourStartsAtZero: Boolean get() = hourLetter == 'K'
}

/** 시간 패턴을 한 번 훑어 [TimePattern]으로. 작은따옴표 안의 글자는 건너뛴다. */
internal fun parseTimePattern(pattern: String): TimePattern {
    var inQuote = false
    var hourIndex = -1
    var periodIndex = -1
    pattern.forEachIndexed { i, c ->
        if (c == '\'') {
            inQuote = !inQuote
        } else if (!inQuote) {
            if (hourIndex < 0 && c in HOUR_LETTERS) hourIndex = i
            if (periodIndex < 0 && c in PERIOD_LETTERS) periodIndex = i
        }
    }
    val hourLetter = pattern.getOrNull(hourIndex)
    return TimePattern(
        hourLetter = hourLetter,
        hourTwoDigits = hourLetter != null && pattern.getOrNull(hourIndex + 1) == hourLetter,
        periodPosition = if (periodIndex >= 0 && hourIndex >= 0 && periodIndex > hourIndex) {
            PeriodPosition.END
        } else {
            PeriodPosition.START
        },
    )
}

private const val HOUR_LETTERS = "hHKk"
private const val PERIOD_LETTERS = "abB"

/** `DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm")`을 읽는다. */
private fun localeTimePattern(locale: Locale, is24Hour: Boolean): TimePattern =
    parseTimePattern(android.text.format.DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"))

/** 시 휠에 보이는 범위. H11 0..11 · H12 1..12 · H23 0..23 · H24 1..24. */
internal fun hourRange(cycle: HourCycle): IntRange = when (cycle) {
    HourCycle.H11 -> 0..11
    HourCycle.H12 -> 1..12
    HourCycle.H23 -> 0..23
    HourCycle.H24 -> 1..24
}

/** 0..23시를 시 휠에 보일 숫자로. H12는 0시·12시가 12, H11은 0, H24는 0시가 24. */
internal fun displayHourOf(hourOfDay: Int, cycle: HourCycle): Int = when (cycle) {
    HourCycle.H11 -> hourOfDay % 12
    HourCycle.H12 -> (hourOfDay % 12).let { if (it == 0) 12 else it }
    HourCycle.H23 -> hourOfDay
    HourCycle.H24 -> if (hourOfDay == 0) 24 else hourOfDay
}

/** 시 휠 숫자와 오전/오후를 0..23시로. 12시간제는 `% 12`, 24시간제는 `% 24`(H24의 24 → 0). */
internal fun hourOfDayOf(displayHour: Int, isPm: Boolean, cycle: HourCycle): Int =
    if (cycle.is24Hour) displayHour % 24 else displayHour % 12 + if (isPm) 12 else 0

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