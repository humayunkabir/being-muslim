package app.beingmuslim.android.hijri

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField

/**
 * One day in the Hijri calendar: [year] (AH), [month] 1–12, and [day] 1–29/30.
 *
 * There is no 0 index and no month name here — naming a month is a UI concern
 * (`HijriDate` has no string resources).
 */
data class HijriDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

/**
 * The calculated Hijri date for a Gregorian day, on the **Umm al-Qura**
 * convention (`docs/PRODUCT_REQUIREMENTS.md` §7, `docs/HIJRI_CALENDAR.md`).
 *
 * This is a calculation, not an observation. A country or community that goes by
 * an announced moon sighting will often begin a month a day either side of it,
 * which is what [dateFor]'s `dayOffsetDays` is for: the user shifts their date
 * to match their own authority. The shift is held here, so one rule governs it
 * wherever a date is shown.
 */
object HijriCalendar {

    /** The most the user can shift a date; the app offers ±1 day. */
    const val MAX_DAY_OFFSET = 1

    /**
     * The Hijri date of [gregorian], shifted by [dayOffsetDays] to match a local
     * announcement. The offset is clamped to
     * [−MAX_DAY_OFFSET, +MAX_DAY_OFFSET][MAX_DAY_OFFSET], so no stored or
     * requested value can move the date further than the app offers.
     *
     * shortcut: the chronology's umalqura table (roughly 1882–2174 CE) is the
     * only range this covers, and a date outside it throws; the app converts
     * today's and tomorrow's date, so widen it only if a date picker or history
     * arrives (`docs/HIJRI_CALENDAR.md`).
     */
    fun dateFor(gregorian: LocalDate, dayOffsetDays: Int = 0): HijriDate {
        val adjusted = gregorian.plusDays(clampDayOffset(dayOffsetDays).toLong())
        val hijri = HijrahChronology.INSTANCE.date(adjusted)

        return HijriDate(
            year = hijri.get(ChronoField.YEAR),
            month = hijri.get(ChronoField.MONTH_OF_YEAR),
            day = hijri.get(ChronoField.DAY_OF_MONTH),
        )
    }

    /** [days] limited to the range the app offers, for a value read from storage. */
    fun clampDayOffset(days: Int): Int = days.coerceIn(-MAX_DAY_OFFSET, MAX_DAY_OFFSET)
}
