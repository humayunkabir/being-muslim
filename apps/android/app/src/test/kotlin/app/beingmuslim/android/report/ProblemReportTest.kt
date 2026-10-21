package app.beingmuslim.android.report

import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.DailyPrayerTimes
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.prayer.PrayerOffsets
import app.beingmuslim.android.prayer.TwoDayPrayerTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The report is the record a wrong time is investigated from, so the encoded
 * shape is the specification: exactly the calculation inputs and the user's
 * note, and nothing that could identify the user or their device (`docs/
 * PRODUCT_REQUIREMENTS.md` §5, §10).
 */
class ProblemReportTest {

    private val dhaka = City(
        id = 1185241L, name = "Dhaka", bengaliName = "ঢাকা",
        latitude = 23.7104, longitude = 90.40744,
        countryCode = "BD", timezone = "Asia/Dhaka",
    )

    private val settings = CalculationSettings(
        method = CalculationMethodChoice.KARACHI,
        asrJuristicMethod = AsrJuristicMethod.HANAFI,
        highLatitudeRule = HighLatitudeRuleChoice.SEVENTH_OF_THE_NIGHT,
    )

    private val firstDay = DailyPrayerTimes(
        date = LocalDate.of(2026, 10, 21),
        zone = ZoneId.of("Asia/Dhaka"),
        times = mapOf(
            Prayer.FAJR to Instant.parse("2026-10-20T23:38:00Z"),
            Prayer.DHUHR to Instant.parse("2026-10-21T05:50:00Z"),
            Prayer.ASR to Instant.parse("2026-10-21T09:11:00Z"),
            Prayer.MAGHRIB to Instant.parse("2026-10-21T11:31:00Z"),
            Prayer.ISHA to Instant.parse("2026-10-21T12:47:00Z"),
        ),
        boundedByHighLatitudeRule = setOf(Prayer.FAJR),
    )

    private val secondDay = firstDay.copy(date = LocalDate.of(2026, 10, 22))

    private val report = ProblemReport(
        appVersion = "0.1.0",
        city = dhaka,
        settings = settings,
        offsets = PrayerOffsets(mapOf(Prayer.FAJR to -3, Prayer.ISHA to 5)),
        hijriDayShift = -1,
        days = TwoDayPrayerTimes(today = firstDay, tomorrow = secondDay),
        note = "Fajr looks ten minutes early.",
    )

    private fun encoded(): JSONObject = JSONObject(ProblemReportCodec.encode(report))

    @Test
    fun carriesOnlyTheCalculationInputsAndTheNoteTopLevel() {
        assertEquals(
            setOf(
                "version", "appVersion", "city", "method", "parameters",
                "asrJuristicMethod", "highLatitudeRule", "hijriDayShift",
                "offsets", "days", "note",
            ),
            encoded().keys().asSequence().toSet(),
        )
    }

    @Test
    fun recordsTheLocationAndTimezoneTheTimeWasCalculatedFor() {
        val city = encoded().getJSONObject("city")

        assertEquals(
            setOf("name", "countryCode", "latitude", "longitude", "timezone"),
            city.keys().asSequence().toSet(),
        )
        assertEquals("Dhaka", city.getString("name"))
        assertEquals("BD", city.getString("countryCode"))
        assertEquals(23.7104, city.getDouble("latitude"), 0.0)
        assertEquals(90.40744, city.getDouble("longitude"), 0.0)
        assertEquals("Asia/Dhaka", city.getString("timezone"))
    }

    @Test
    fun recordsTheMethodItsParametersAndTheJuristsAndLatitudeChoices() {
        val encoded = encoded()
        val parameters = encoded.getJSONObject("parameters")

        assertEquals(1, encoded.getInt("version"))
        assertEquals("0.1.0", encoded.getString("appVersion"))
        assertEquals("KARACHI", encoded.getString("method"))
        assertEquals("HANAFI", encoded.getString("asrJuristicMethod"))
        assertEquals("SEVENTH_OF_THE_NIGHT", encoded.getString("highLatitudeRule"))
        assertEquals(-1, encoded.getInt("hijriDayShift"))

        assertEquals(18.0, parameters.getDouble("fajrAngle"), 0.0)
        assertEquals(18.0, parameters.getDouble("ishaAngle"), 0.0)
        assertEquals(0, parameters.getInt("ishaInterval"))
        val adjustments = parameters.getJSONObject("adjustments")
        assertEquals(
            setOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"),
            adjustments.keys().asSequence().toSet(),
        )
        // Karachi's built-in Dhuhr adjustment, recorded rather than restated.
        assertEquals(1, adjustments.getInt("dhuhr"))
    }

    @Test
    fun recordsEveryPrayerOffsetIncludingTheUnadjustedOnes() {
        val offsets = encoded().getJSONObject("offsets")

        assertEquals(
            setOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA"),
            offsets.keys().asSequence().toSet(),
        )
        assertEquals(-3, offsets.getInt("FAJR"))
        assertEquals(0, offsets.getInt("DHUHR"))
        assertEquals(5, offsets.getInt("ISHA"))
    }

    @Test
    fun recordsTheDatesTheRawTimesAndWhereTheRuleProducedATime() {
        val days = encoded().getJSONArray("days")
        assertEquals(2, days.length())

        val today = days.getJSONObject(0)
        assertEquals(
            setOf("date", "times", "highLatitudeApplied"),
            today.keys().asSequence().toSet(),
        )
        assertEquals("2026-10-21", today.getString("date"))
        assertEquals(
            setOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA"),
            today.getJSONObject("times").keys().asSequence().toSet(),
        )
        assertEquals("2026-10-20T23:38:00Z", today.getJSONObject("times").getString("FAJR"))
        val applied = today.getJSONArray("highLatitudeApplied")
        assertEquals(listOf("FAJR"), (0 until applied.length()).map { applied.getString(it) })
        assertEquals("2026-10-22", days.getJSONObject(1).getString("date"))
    }

    @Test
    fun aPolarDayIsLeftOutRatherThanGivenAnInventedTime() {
        val report = report.copy(
            days = TwoDayPrayerTimes(today = null, tomorrow = secondDay),
        )
        val days = JSONObject(ProblemReportCodec.encode(report)).getJSONArray("days")

        assertEquals(1, days.length())
        assertEquals("2026-10-22", days.getJSONObject(0).getString("date"))
    }

    @Test
    fun theNoteIsBoundedSoTheReportCannotBecomeAnythingElse() {
        val long = report.withNote("x".repeat(ProblemReport.MAX_NOTE_LENGTH + 50))

        assertEquals(ProblemReport.MAX_NOTE_LENGTH, long.note.length)
        assertEquals(
            long.note,
            JSONObject(ProblemReportCodec.encode(long)).getString("note"),
        )
    }

    @Test
    fun anEmptyNoteIsStillAnEmptyString() {
        assertEquals("", JSONObject(ProblemReportCodec.encode(report.withNote(""))).getString("note"))
    }
}
