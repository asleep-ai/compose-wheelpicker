package ai.asleep.compose.wheelpicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

class TimePickerLocaleTest {

    // Patterns are what DateFormat.getBestDateTimePattern(locale, "hm") returns on Android (CLDR).
    @Test
    fun periodBeforeHour_isStart() {
        assertEquals(PeriodPosition.START, parseTimePattern("a h:mm").periodPosition) // ko
        assertEquals(PeriodPosition.START, parseTimePattern("aK:mm").periodPosition) // ja
    }

    @Test
    fun periodAfterHour_isEnd() {
        assertEquals(PeriodPosition.END, parseTimePattern("h:mm a").periodPosition) // en, hi, es, fr, id
        assertEquals(PeriodPosition.END, parseTimePattern("h:mm B").periodPosition)
    }

    @Test
    fun quotedLettersAreIgnored() {
        assertEquals(PeriodPosition.END, parseTimePattern("'a' h:mm a").periodPosition)
        assertEquals(PeriodPosition.START, parseTimePattern("a 'h' h:mm").periodPosition)
    }

    @Test
    fun noPeriodSymbol_fallsBackToStart() {
        assertEquals(PeriodPosition.START, parseTimePattern("HH:mm").periodPosition)
    }

    @Test
    fun defaultFormatters_latinLocale() {
        val locale = Locale.US
        assertEquals("7", defaultHourFormatter(twoDigits = false, locale)(7))
        assertEquals("07", defaultHourFormatter(twoDigits = true, locale)(7))
        assertEquals("05", defaultMinuteFormatter(locale)(5))
    }

    @Test
    fun defaultFormatters_hourAndMinuteShareDigits() {
        val locale = Locale.forLanguageTag("mr-IN")
        val zero = defaultMinuteFormatter(locale)(0)[0]
        val hour12 = defaultHourFormatter(twoDigits = false, locale)(7)
        val hour24 = defaultHourFormatter(twoDigits = true, locale)(7)
        val minute = defaultMinuteFormatter(locale)(7)
        assertEquals((zero + 7).toString(), hour12)
        assertEquals("$zero${zero + 7}", hour24)
        assertEquals("$zero${zero + 7}", minute)
    }

    @Test
    fun hourDigits_followPatternLetterCount() {
        assertEquals(true, parseTimePattern("HH:mm").hourTwoDigits)
        assertEquals(true, parseTimePattern("hh:mm a").hourTwoDigits)
        assertEquals(true, parseTimePattern("kk:mm").hourTwoDigits)
        assertEquals(false, parseTimePattern("H:mm").hourTwoDigits)
        assertEquals(false, parseTimePattern("a h:mm").hourTwoDigits)
        assertEquals(false, parseTimePattern("aK:mm").hourTwoDigits)
        assertEquals(false, parseTimePattern("mm").hourTwoDigits) // 시 글자 없음
    }

    @Test
    fun hourDigits_quotedLettersAreIgnored() {
        assertEquals(true, parseTimePattern("'h' HH:mm").hourTwoDigits)
        assertEquals(false, parseTimePattern("'hh' H:mm").hourTwoDigits)
    }

    /**
     * SYSTEM 기본 시 표기. 패턴은 에뮬레이터(API 37)에서
     * DateFormat.getBestDateTimePattern(locale, "hm" / "Hm")로 받은 값이다.
     */
    @Test
    fun systemHour_followsLocalePattern() {
        fun hour(tag: String, pattern: String, h: Int = 7) =
            defaultHourFormatter(parseTimePattern(pattern).hourTwoDigits, Locale.forLanguageTag(tag))(h)

        // 12시간제: 9개 로케일 모두 시 글자가 한 글자라 0이 붙지 않는다
        assertEquals("7", hour("ko-KR", "a h:mm"))
        assertEquals("7", hour("en-US", "h:mm a"))
        assertEquals("7", hour("ja-JP", "aK:mm"))
        assertEquals("7", hour("id-ID", "h.mm a"))

        // 24시간제: HH면 "07", H면 "7"
        assertEquals("07", hour("ko-KR", "HH:mm"))
        assertEquals("07", hour("en-US", "HH:mm"))
        assertEquals("07", hour("fr-FR", "HH:mm"))
        assertEquals("07", hour("id-ID", "HH.mm"))
        assertEquals("7", hour("ja-JP", "H:mm"))
        assertEquals("7", hour("es-ES", "H:mm"))
        assertEquals("0", hour("es-ES", "H:mm", h = 0))
        assertEquals("00", hour("fr-FR", "HH:mm", h = 0))
    }

    @Test
    fun systemHour_keepsLocaleDigits() {
        val locale = Locale.forLanguageTag("mr-IN")
        val zero = defaultMinuteFormatter(locale)(0)[0]
        // mr-IN 24시간제 패턴은 "H:mm" → 0 없이 한 자리, 숫자는 분과 같은 체계
        assertEquals((zero + 7).toString(), defaultHourFormatter(parseTimePattern("H:mm").hourTwoDigits, locale)(7))
    }

    @Test
    fun hourStartsAtZero_onlyForK() {
        assertEquals(true, parseTimePattern("aK:mm").hourStartsAtZero) // ja
        assertEquals(true, parseTimePattern("aKK:mm").hourStartsAtZero)
        assertEquals(false, parseTimePattern("a h:mm").hourStartsAtZero) // ko
        assertEquals(false, parseTimePattern("h:mm a").hourStartsAtZero) // en
        assertEquals(false, parseTimePattern("'K' h:mm a").hourStartsAtZero)
        assertEquals(true, parseTimePattern("'h' aK:mm").hourStartsAtZero)
        assertEquals(false, parseTimePattern("mm").hourStartsAtZero)
    }

    /**
     * initialTime → 휠 위치(시 인덱스, 오전/오후 인덱스) → onTimeChange의 LocalTime 왕복.
     * TimePicker와 같은 함수(hourRange·displayHourOf·hourOfDayOf)로 계산한다.
     */
    private data class Wheel(val hourIndex: Int, val periodIndex: Int, val label: String)

    private fun toWheel(time: LocalTime, startsAtZero: Boolean): Wheel {
        val display = displayHourOf(time.hour, is24Hour = false, startsAtZero)
        val range = hourRange(is24Hour = false, startsAtZero)
        assertTrue("$display !in $range", display in range)
        val periodIndex = if (time.hour < 12) 0 else 1
        return Wheel(display - range.first, periodIndex, "${if (periodIndex == 0) "AM" else "PM"} $display")
    }

    private fun fromWheel(w: Wheel, minute: Int, startsAtZero: Boolean): LocalTime {
        val display = hourRange(is24Hour = false, startsAtZero).first + w.hourIndex
        return LocalTime.of(hourOfDayOf(display, isPm = w.periodIndex == 1, is24Hour = false), minute)
    }

    private fun assertRoundTrip(pattern: String, expected: Map<String, String>) {
        val zero = parseTimePattern(pattern).hourStartsAtZero
        expected.forEach { (text, label) ->
            val time = LocalTime.parse(text)
            val wheel = toWheel(time, zero)
            assertEquals("$pattern $text", label, wheel.label)
            assertEquals("$pattern $text", time, fromWheel(wheel, time.minute, zero))
        }
    }

    @Test
    fun roundTrip_kPattern() {
        assertRoundTrip(
            "aK:mm",
            mapOf("00:30" to "AM 0", "12:30" to "PM 0", "23:59" to "PM 11", "07:00" to "AM 7"),
        )
    }

    @Test
    fun roundTrip_hPattern() {
        assertRoundTrip(
            "a h:mm",
            mapOf("00:30" to "AM 12", "12:30" to "PM 12", "23:59" to "PM 11", "07:00" to "AM 7"),
        )
    }

    @Test
    fun kPattern_everyWheelPositionMapsToDistinctHour() {
        // 휠 0..11 × 오전/오후 = 24칸이 0..23시에 하나씩 대응한다 (午前 0 → 午後 0 경계 포함)
        val hours = (0..1).flatMap { p -> (0..11).map { i -> fromWheel(Wheel(i, p, ""), 0, true).hour } }
        assertEquals((0..23).toList(), hours)
    }

    @Test
    fun explicitFormats_unchanged() {
        assertEquals(1..12, hourRange(is24Hour = false, startsAtZero = false))
        assertEquals(0..23, hourRange(is24Hour = true, startsAtZero = false))
        assertEquals(12, displayHourOf(0, is24Hour = false, startsAtZero = false))
        assertEquals(12, displayHourOf(12, is24Hour = false, startsAtZero = false))
        assertEquals(0, displayHourOf(0, is24Hour = true, startsAtZero = false))
        assertEquals(23, hourOfDayOf(23, isPm = false, is24Hour = true))
    }
}
