package app.beingmuslim.android.ui

import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import app.beingmuslim.android.prayer.PrayerOffsets
import app.beingmuslim.android.prayer.TwoDayPrayerTimes
import app.beingmuslim.android.report.ProblemReport
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The report screen's two rules: what the user may still change, and which send
 * an outcome belongs to. Both are pure so they can be pinned here rather than
 * only through the ViewModel, which needs an Application to exist.
 */
class ReportStateTest {

    private val report = ProblemReport(
        appVersion = "0.1.0",
        city = City(
            id = 1185241L, name = "Dhaka", bengaliName = "ঢাকা",
            latitude = 23.7104, longitude = 90.40744,
            countryCode = "BD", timezone = "Asia/Dhaka",
        ),
        settings = CalculationSettings(
            method = CalculationMethodChoice.KARACHI,
            asrJuristicMethod = AsrJuristicMethod.HANAFI,
            highLatitudeRule = HighLatitudeRuleChoice.SEVENTH_OF_THE_NIGHT,
        ),
        offsets = PrayerOffsets(),
        hijriDayShift = 0,
        date = LocalDate.of(2026, 10, 21),
        days = TwoDayPrayerTimes(today = null, tomorrow = null),
    )

    private fun state(sendState: ReportSendState) = AppUiState.Report(report, sendState)

    @Test
    fun theReportIsEditableOnlyBeforeItIsSent() {
        assertTrue(ReportSendState.NOT_SENT.isReportEditable)
        assertTrue(ReportSendState.FAILED.isReportEditable)
        assertFalse(ReportSendState.SENDING.isReportEditable)
        assertFalse(ReportSendState.SENT.isReportEditable)
    }

    @Test
    fun aNoteIsTakenBeforeSending() {
        val next = state(ReportSendState.NOT_SENT).withNote("Fajr looks early.")

        assertEquals("Fajr looks early.", next?.report?.note)
        assertEquals(
            "Retry",
            state(ReportSendState.FAILED).withNote("Retry")?.report?.note,
        )
    }

    @Test
    fun aNoteIsIgnoredOnceSendingHasBegun() {
        assertNull(state(ReportSendState.SENDING).withNote("Too late"))
        assertNull(state(ReportSendState.SENT).withNote("Too late"))
    }

    @Test
    fun anOutcomeBelongsToTheReportThatWasSent() {
        assertEquals(
            ReportSendState.SENT,
            state(ReportSendState.SENDING).withSendOutcome(report, sent = true)?.sendState,
        )
        assertEquals(
            ReportSendState.FAILED,
            state(ReportSendState.SENDING).withSendOutcome(report, sent = false)?.sendState,
        )
    }

    @Test
    fun anOutcomeIsIgnoredForAReopenedReportOrWhenNoSendIsInFlight() {
        // A reopened screen builds a new report even from identical inputs, so
        // an earlier send's outcome must not land on it.
        val reopened = report.copy(note = "Something else")
        assertNull(state(ReportSendState.SENDING).withSendOutcome(reopened, sent = true))
        assertNull(state(ReportSendState.NOT_SENT).withSendOutcome(report, sent = true))
        assertNull(state(ReportSendState.SENT).withSendOutcome(report, sent = true))
    }

    @Test
    fun aSendKeepsTheReportItselfUnchanged() {
        val before = state(ReportSendState.SENDING)
        val after = before.withSendOutcome(report, sent = false)

        assertSame(before.report, after?.report)
    }
}
