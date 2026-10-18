package app.beingmuslim.android.hijri

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField

/**
 * One day in the Hijri calendar: [year] (AH), [month] 1–12, and [day] 1–29/30.
 *
 * Month naming is a UI concern, so this carries no string resources.
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
 * This is a calculation, not an observation. A country or community that begins
 * a month by an announced moon sighting will often begin it a day either side of
 * this, and [dateFor]'s `dayShiftDays` is how the user reconciles the two. The
 * limit on that shift is applied here alone, so every caller gets the same one.
 */
object HijriCalendar {

    /** The most the user can shift a date; the app offers ±1 day. */
    const val MAX_DAY_SHIFT = 1

    /**
     * The Hijri date of [gregorian], shifted by [dayShiftDays] to match a local
     * announcement. The shift is clamped to ±[MAX_DAY_SHIFT], so neither a
     * stored nor a requested value can move the date further than the app
     * offers.
     *
     * shortcut: the chronology's umalqura table (roughly 1882–2174 CE) is the
     * only range this covers, and a date outside it throws; the app converts
     * today's and tomorrow's date, so widen it only if a date picker or history
     * arrives (`docs/HIJRI_CALENDAR.md`).
     */
    fun dateFor(gregorian: LocalDate, dayShiftDays: Int = 0): HijriDate {
        val shifted = gregorian.plusDays(clampDayShift(dayShiftDays).toLong())
        val hijri = HijrahChronology.INSTANCE.date(shifted)

        return HijriDate(
            year = hijri.get(ChronoField.YEAR),
            month = hijri.get(ChronoField.MONTH_OF_YEAR),
            day = hijri.get(ChronoField.DAY_OF_MONTH),
        )
    }

    /** [days] limited to the range the app offers, for a value read from storage. */
    fun clampDayShift(days: Int): Int = days.coerceIn(-MAX_DAY_SHIFT, MAX_DAY_SHIFT)
}
