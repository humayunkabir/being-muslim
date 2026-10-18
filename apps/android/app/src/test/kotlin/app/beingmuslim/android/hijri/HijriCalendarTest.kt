package app.beingmuslim.android.hijri

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The calculated Hijri date and its user adjustment, against the Umm al-Qura
 * convention (`docs/PRODUCT_REQUIREMENTS.md` §7, `docs/HIJRI_CALENDAR.md`).
 *
 * Expected values are **published Umm al-Qura dates from independent sources** —
 * the Saudi Supreme Court's announcement of 1 Muharram 1446 quoting the
 * calendar, and the Umm al-Qura civil calendars of Ramadan/Shawwal 1445 and
 * Ramadan 1446 — not values produced by the code under test. Tests run on the
 * JVM with no device.
 */
class HijriCalendarTest {

    @Test
    fun convertsOnTheUmmAlQuraConvention() {
        // The label the app shows names a convention; this pins it to the
        // platform's, so a platform default changing is a loud failure.
        assertEquals("Hijrah-umalqura", HijrahChronology.INSTANCE.id)
    }

    @Test
    fun matchesPublishedUmmAlQuraDates() {
        // 1 Ramadan 1445 AH; also 1 Shawaal 1445 AH (Eid al-Fitr), 1 Muharram
        // 1446 AH, and 1 Ramadan 1446 AH.
        assertEquals(HijriDate(1445, 9, 1), HijriCalendar.dateFor(LocalDate.of(2024, 3, 11)))
        assertEquals(HijriDate(1445, 10, 1), HijriCalendar.dateFor(LocalDate.of(2024, 4, 10)))
        assertEquals(HijriDate(1446, 1, 1), HijriCalendar.dateFor(LocalDate.of(2024, 7, 7)))
        assertEquals(HijriDate(1446, 9, 1), HijriCalendar.dateFor(LocalDate.of(2025, 3, 1)))
    }

    @Test
    fun shiftsAcrossAMonthOf29Days() {
        // Sha'ban 1446 ran 29 days, so the day after 29 Sha'ban is 1 Ramadan —
        // reached from the calculated date by the user's +1 day.
        assertEquals(HijriDate(1446, 8, 29), HijriCalendar.dateFor(LocalDate.of(2025, 2, 28)))
        assertEquals(HijriDate(1446, 9, 1), HijriCalendar.dateFor(LocalDate.of(2025, 2, 28), dayOffsetDays = 1))
    }

    @Test
    fun shiftsAcrossAMonthOf30Days() {
        // Ramadan 1445 ran 30 days; the Eid date less one day is 30 Ramadan.
        assertEquals(HijriDate(1445, 9, 30), HijriCalendar.dateFor(LocalDate.of(2024, 4, 10), dayOffsetDays = -1))
    }

    @Test
    fun shiftsAcrossAYearBoundary() {
        // The last day of 1445 AH and the first of 1446 AH, either side of the
        // adjustment.
        assertEquals(HijriDate(1446, 1, 1), HijriCalendar.dateFor(LocalDate.of(2024, 7, 6), dayOffsetDays = 1))
        assertEquals(HijriDate(1445, 12, 30), HijriCalendar.dateFor(LocalDate.of(2024, 7, 7), dayOffsetDays = -1))
    }

    @Test
    fun matchesALocalMoonSightingAnnouncementADayAfterTheCalculation() {
        // Singapore's MUIS calendar announced 1 Ramadan 1445 on 12 March 2024,
        // a day after the Umm al-Qura date — the case the ±1 day is for.
        assertEquals(HijriDate(1445, 9, 1), HijriCalendar.dateFor(LocalDate.of(2024, 3, 12), dayOffsetDays = -1))
    }

    @Test
    fun clampsTheAdjustmentToTheOneDayTheAppOffers() {
        assertEquals(HijriDate(1445, 9, 2), HijriCalendar.dateFor(LocalDate.of(2024, 3, 11), dayOffsetDays = 5))
        assertEquals(HijriDate(1445, 8, 29), HijriCalendar.dateFor(LocalDate.of(2024, 3, 11), dayOffsetDays = -5))
    }
}
