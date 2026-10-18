package app.beingmuslim.android.prayer

import app.beingmuslim.android.citycatalog.City
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prayer times against the BatoulApps Adhan reference cases.
 *
 * Source: the `Shared/Times` files shipped with the Adhan library, which the
 * library itself validates against the official timetables they were taken from
 * (moonsighting.com for London, ummulqura.org.sa for Makkah) to **±1 minute** —
 * the tolerance reused here. Cases are pinned to adhan 1.2.1
 * (`docs/PRAYER_TIMES.md`). Tests run on the JVM with no device.
 *
 * Two cases come from the library's own test suites instead of those files: the
 * pinned 1.2.1 suite's Oslo high-latitude case, and the high-latitude rule case
 * from its Kotlin port, whose values the pinned artifact returns unchanged.
 */
class PrayerTimeCalculatorTest {

    private val london = City(
        id = 2643743L, name = "London", bengaliName = null,
        latitude = 51.507194, longitude = -0.116711,
        countryCode = "GB", timezone = "Europe/London",
    )
    private val makkah = City(
        id = 104515L, name = "Makkah", bengaliName = null,
        latitude = 21.427009, longitude = 39.828685,
        countryCode = "SA", timezone = "Asia/Riyadh",
    )
    private val dhaka = City(
        id = 1185241L, name = "Dhaka", bengaliName = "ঢাকা",
        latitude = 23.7104, longitude = 90.40744,
        countryCode = "BD", timezone = "Asia/Dhaka",
    )
    // The coordinates of Adhan's own high-latitude test case (see the rule test).
    private val edinburgh = City(
        id = 2650225L, name = "Edinburgh", bengaliName = null,
        latitude = 55.983226, longitude = -3.216649,
        countryCode = "GB", timezone = "Europe/London",
    )
    // The coordinates of the pinned library's own Oslo high-latitude case.
    private val oslo = City(
        id = 3143244L, name = "Oslo", bengaliName = null,
        latitude = 59.9094, longitude = 10.7349,
        countryCode = "NO", timezone = "Europe/Oslo",
    )
    private val longyearbyen = City(
        id = 2729907L, name = "Longyearbyen", bengaliName = null,
        latitude = 78.2232, longitude = 15.6267,
        countryCode = "SJ", timezone = "Arctic/Longyearbyen",
    )

    // Coordinates are the reference files' own, so the comparison is like for like.
    private val singapore = City(
        id = 1880252L, name = "Singapore", bengaliName = null,
        latitude = 1.283333, longitude = 103.833333,
        countryCode = "SG", timezone = "Asia/Singapore",
    )
    private val dubai = City(
        id = 292223L, name = "Dubai", bengaliName = null,
        latitude = 25.263056, longitude = 55.297222,
        countryCode = "AE", timezone = "Asia/Dubai",
    )
    private val kuwaitCity = City(
        id = 285787L, name = "Kuwait City", bengaliName = null,
        latitude = 29.370865, longitude = 47.979139,
        countryCode = "KW", timezone = "Asia/Kuwait",
    )
    private val doha = City(
        id = 290030L, name = "Doha", bengaliName = null,
        latitude = 25.283897, longitude = 51.52877,
        countryCode = "QA", timezone = "Asia/Riyadh",
    )

    private fun assertTimesOn(
        city: City,
        date: LocalDate,
        settings: CalculationSettings,
        expected: Map<Prayer, LocalTime>,
        tolerance: Long = REFERENCE_TOLERANCE_MINUTES,
    ) {
        val times = PrayerTimeCalculator.forDate(city, date, settings)
        assertEquals("no times computed for $city on $date", expected.keys, times!!.times.keys)
        expected.forEach { (prayer, wallTime) ->
            val actual = times[prayer].atZone(times.zone).toLocalTime()
            val drift = abs(Duration.between(wallTime, actual).toMinutes())
            assertTrue(
                "${city.name} $date $prayer: expected ~$wallTime but was $actual",
                drift <= tolerance,
            )
        }
    }

    @Test
    fun matchesTheLondonMoonsightingCommitteeReferenceCases() {
        val settings = CalculationSettings(
            method = CalculationMethodChoice.MOON_SIGHTING_COMMITTEE,
            asrJuristicMethod = AsrJuristicMethod.HANAFI,
            highLatitudeRule = HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT,
        )

        assertTimesOn(
            london, LocalDate.of(2016, 1, 1), settings,
            mapOf(
                Prayer.FAJR to LocalTime.of(6, 25),
                Prayer.DHUHR to LocalTime.of(12, 9),
                Prayer.ASR to LocalTime.of(14, 15),
                Prayer.MAGHRIB to LocalTime.of(16, 5),
                Prayer.ISHA to LocalTime.of(17, 38),
            ),
        )

        // A daylight-saving date: the same city is on BST (+1).
        assertTimesOn(
            london, LocalDate.of(2016, 7, 1), settings,
            mapOf(
                Prayer.FAJR to LocalTime.of(2, 51),
                Prayer.DHUHR to LocalTime.of(13, 9),
                Prayer.ASR to LocalTime.of(18, 40),
                Prayer.MAGHRIB to LocalTime.of(21, 24),
                Prayer.ISHA to LocalTime.of(22, 38),
            ),
        )
    }

    /**
     * The pinned library's own high-latitude case (`PrayerTimesTest
     * .testMoonsightingMethodHighLat` in adhan 1.2.1: 60°N, Oslo, Moonsighting
     * Committee, Hanafi), whose times come from the method's seasonal adjustment
     * at high latitude. This is the case the times screen reports as the
     * high-latitude rule not being used at all (`docs/PRAYER_TIMES.md`).
     */
    @Test
    fun matchesTheOsloHighLatitudeReferenceCase() {
        assertTimesOn(
            oslo, LocalDate.of(2016, 1, 1),
            CalculationSettings(
                method = CalculationMethodChoice.MOON_SIGHTING_COMMITTEE,
                asrJuristicMethod = AsrJuristicMethod.HANAFI,
                highLatitudeRule = HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT,
            ),
            mapOf(
                Prayer.FAJR to LocalTime.of(7, 34),
                Prayer.DHUHR to LocalTime.of(12, 25),
                Prayer.ASR to LocalTime.of(13, 36),
                Prayer.MAGHRIB to LocalTime.of(15, 25),
                Prayer.ISHA to LocalTime.of(17, 2),
            ),
        )
    }

    @Test
    fun matchesTheHighLatitudeReferenceCaseForEveryRule() {
        // The high-latitude rule case from the library's Kotlin port
        // (`adhan2`'s `PrayerTimesTest.testHighLatitudeRule`: 56°N, Muslim World
        // League, Shafi'i) — the pinned 1.2.1 artifact returns exactly these
        // values. Here the rule, and only the rule, decides Fajr and Isha, two
        // hours apart between the loosest and the strictest. Cited in
        // `docs/PRAYER_TIMES.md`.
        val expectedPerRule = mapOf(
            HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT to mapOf(
                Prayer.FAJR to LocalTime.of(1, 14),
                Prayer.DHUHR to LocalTime.of(13, 14),
                Prayer.ASR to LocalTime.of(17, 46),
                Prayer.MAGHRIB to LocalTime.of(22, 1),
                Prayer.ISHA to LocalTime.of(1, 14),
            ),
            HighLatitudeRuleChoice.SEVENTH_OF_THE_NIGHT to mapOf(
                Prayer.FAJR to LocalTime.of(3, 31),
                Prayer.DHUHR to LocalTime.of(13, 14),
                Prayer.ASR to LocalTime.of(17, 46),
                Prayer.MAGHRIB to LocalTime.of(22, 1),
                Prayer.ISHA to LocalTime.of(22, 56),
            ),
            HighLatitudeRuleChoice.TWILIGHT_ANGLE to mapOf(
                Prayer.FAJR to LocalTime.of(2, 31),
                Prayer.DHUHR to LocalTime.of(13, 14),
                Prayer.ASR to LocalTime.of(17, 46),
                Prayer.MAGHRIB to LocalTime.of(22, 1),
                Prayer.ISHA to LocalTime.of(23, 50),
            ),
        )

        expectedPerRule.forEach { (rule, expected) ->
            assertTimesOn(
                edinburgh,
                LocalDate.of(2020, 6, 15),
                CalculationSettings(
                    method = CalculationMethodChoice.MUSLIM_WORLD_LEAGUE,
                    asrJuristicMethod = AsrJuristicMethod.STANDARD,
                    highLatitudeRule = rule,
                ),
                expected,
            )
        }
    }

    @Test
    fun matchesTheMakkahUmmAlQuraReferenceCases() {
        val settings = CalculationSettings(
            method = CalculationMethodChoice.UMM_AL_QURA,
            asrJuristicMethod = AsrJuristicMethod.STANDARD,
            highLatitudeRule = HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT,
        )

        assertTimesOn(
            makkah, LocalDate.of(2016, 1, 5), settings,
            mapOf(
                Prayer.FAJR to LocalTime.of(5, 38),
                Prayer.DHUHR to LocalTime.of(12, 26),
                Prayer.ASR to LocalTime.of(15, 31),
                Prayer.MAGHRIB to LocalTime.of(17, 52),
                Prayer.ISHA to LocalTime.of(19, 22),
            ),
        )

        assertTimesOn(
            makkah, LocalDate.of(2016, 6, 4), settings,
            mapOf(
                Prayer.FAJR to LocalTime.of(4, 11),
                Prayer.DHUHR to LocalTime.of(12, 19),
                Prayer.ASR to LocalTime.of(15, 36),
                Prayer.MAGHRIB to LocalTime.of(19, 1),
                Prayer.ISHA to LocalTime.of(20, 31),
            ),
        )
    }

    @Test
    fun matchesTheRemainingMethodReferenceCases() {
        // Singapore, Gulf (Dubai), Kuwait and Qatar, each from its own reference
        // file; tolerances are the files' own variances (Kuwait allows 2 minutes).
        val singaporeSettings = CalculationSettings(CalculationMethodChoice.SINGAPORE, AsrJuristicMethod.STANDARD, HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT)
        val dubaiSettings = CalculationSettings(CalculationMethodChoice.DUBAI, AsrJuristicMethod.STANDARD, HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT)
        val kuwaitSettings = CalculationSettings(CalculationMethodChoice.KUWAIT, AsrJuristicMethod.STANDARD, HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT)
        val qatarSettings = CalculationSettings(CalculationMethodChoice.QATAR, AsrJuristicMethod.STANDARD, HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT)
        val date = LocalDate.of(2016, 1, 1)

        assertTimesOn(
            singapore, date, singaporeSettings,
            mapOf(
                Prayer.FAJR to LocalTime.of(5, 43),
                Prayer.DHUHR to LocalTime.of(13, 9),
                Prayer.ASR to LocalTime.of(16, 33),
                Prayer.MAGHRIB to LocalTime.of(19, 9),
                Prayer.ISHA to LocalTime.of(20, 24),
            ),
        )
        assertTimesOn(
            dubai, date, dubaiSettings,
            mapOf(
                Prayer.FAJR to LocalTime.of(5, 42),
                Prayer.DHUHR to LocalTime.of(12, 25),
                Prayer.ASR to LocalTime.of(15, 24),
                Prayer.MAGHRIB to LocalTime.of(17, 44),
                Prayer.ISHA to LocalTime.of(19, 3),
            ),
        )
        assertTimesOn(
            kuwaitCity, date, kuwaitSettings,
            mapOf(
                Prayer.FAJR to LocalTime.of(5, 18),
                Prayer.DHUHR to LocalTime.of(11, 51),
                Prayer.ASR to LocalTime.of(14, 42),
                Prayer.MAGHRIB to LocalTime.of(17, 1),
                Prayer.ISHA to LocalTime.of(18, 23),
            ),
            tolerance = 2,
        )
        assertTimesOn(
            doha, date, qatarSettings,
            mapOf(
                Prayer.FAJR to LocalTime.of(4, 58),
                Prayer.DHUHR to LocalTime.of(11, 37),
                Prayer.ASR to LocalTime.of(14, 35),
                Prayer.MAGHRIB to LocalTime.of(16, 55),
                Prayer.ISHA to LocalTime.of(18, 25),
            ),
        )
    }

    @Test
    fun theHanafiAsrIsLaterThanTheStandardAsr() {
        val standard = CalculationSettings(CalculationMethodChoice.KARACHI, AsrJuristicMethod.STANDARD, HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT)
        val hanafi = CalculationSettings(CalculationMethodChoice.KARACHI, AsrJuristicMethod.HANAFI, HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT)

        val standardAsr = PrayerTimeCalculator.forDate(dhaka, LocalDate.of(2016, 6, 21), standard)!![Prayer.ASR]
        val hanafiAsr = PrayerTimeCalculator.forDate(dhaka, LocalDate.of(2016, 6, 21), hanafi)!![Prayer.ASR]

        assertTrue("Hanafi Asr should be later", hanafiAsr.isAfter(standardAsr))
    }

    @Test
    fun todayAndTomorrowFollowTheCitiesLocalCalendar() {
        val dhakaSettings =
            CalculationSettings(CalculationMethodChoice.KARACHI, AsrJuristicMethod.STANDARD, HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT)

        // 2016-07-01T20:00Z is already the 2nd of July in Dhaka (UTC+6): both
        // days come from the city's zone, not the device's.
        val dhakaDays = PrayerTimeCalculator.forInstantAndNextDay(
            dhaka,
            Instant.parse("2016-07-01T20:00:00Z"),
            dhakaSettings,
        )

        assertEquals(LocalDate.of(2016, 7, 2), dhakaDays.today!!.date)
        assertEquals(LocalDate.of(2016, 7, 3), dhakaDays.tomorrow!!.date)
        assertEquals(ZoneId.of("Asia/Dhaka"), dhakaDays.tomorrow.zone)
        assertTrue(dhakaDays.tomorrow[Prayer.FAJR].isAfter(dhakaDays.today[Prayer.ISHA]))

        // The eve of the spring clock change (BST begins on 2016-03-27). A day
        // added as 24 hours would land on the 28th, so tomorrow is asserted to
        // be the next local date instead.
        val londonDays = PrayerTimeCalculator.forInstantAndNextDay(
            london,
            Instant.parse("2016-03-26T23:30:00Z"),
            CalculationSettings(CalculationMethodChoice.MOON_SIGHTING_COMMITTEE, AsrJuristicMethod.HANAFI, HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT),
        )

        assertEquals(LocalDate.of(2016, 3, 26), londonDays.today!!.date)
        assertEquals(LocalDate.of(2016, 3, 27), londonDays.tomorrow!!.date)
        // The clock change falls inside tomorrow, so each day must carry the
        // offset its own date's rules give it: GMT then BST, not one shared
        // offset applied to both.
        assertEquals(ZoneOffset.ofHours(0), londonDays.today.zone.rules.getOffset(londonDays.today[Prayer.ISHA]))
        assertEquals(ZoneOffset.ofHours(1), londonDays.tomorrow.zone.rules.getOffset(londonDays.tomorrow[Prayer.ISHA]))
    }

    @Test
    fun aDayTheSunDoesNotRiseIsUnavailableOnItsOwn() {
        val days = PrayerTimeCalculator.forInstantAndNextDay(
            longyearbyen,
            Instant.parse("2016-06-21T12:00:00Z"),
            CalculationSettings(CalculationMethodChoice.MUSLIM_WORLD_LEAGUE, AsrJuristicMethod.STANDARD, HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT),
        )

        assertNull(days.today)
        assertNull(days.tomorrow)
    }

    @Test
    fun returnsNoTimesWhenTheSunNeitherRisesNorSets() {
        val settings = CalculationSettings(
            method = CalculationMethodChoice.MUSLIM_WORLD_LEAGUE,
            asrJuristicMethod = AsrJuristicMethod.STANDARD,
            highLatitudeRule = HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT,
        )

        assertNull(PrayerTimeCalculator.forDate(longyearbyen, LocalDate.of(2016, 6, 21), settings))
    }

    /**
     * Issue #7: the times screen says so where the high-latitude rule, rather
     * than true twilight, produced a day's Fajr or Isha. This is the reference
     * case above — at 56°N in June, neither Fajr nor Isha can come from true
     * twilight under any of the three rules.
     */
    @Test
    fun reportsEveryTimeTheHighLatitudeRuleProduced() {
        val date = LocalDate.of(2020, 6, 15)

        HighLatitudeRuleChoice.entries.forEach { rule ->
            assertEquals(
                "$rule should account for Fajr and Isha on $date",
                setOf(Prayer.FAJR, Prayer.ISHA),
                boundedPrayersAt(edinburgh, date, settings(rule)),
            )
        }
    }

    /**
     * The case a rule-to-rule comparison gets wrong: at 51.5°N in August the
     * middle-of-the-night limit is never reached, so true dawn and dusk stand and
     * nothing is disclosed — while the tighter seventh rule does cap both. The
     * two rules return different times either way, so the disclosure cannot be
     * inferred from the times alone.
     */
    @Test
    fun doesNotReportALooserRuleThatKeptTrueTwilight() {
        val date = LocalDate.of(2016, 8, 15)

        assertTrue(boundedPrayersAt(london, date, settings(HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT)).isEmpty())
        assertTrue(boundedPrayersAt(london, date, settings(HighLatitudeRuleChoice.TWILIGHT_ANGLE)).isEmpty())
        assertEquals(
            setOf(Prayer.FAJR, Prayer.ISHA),
            boundedPrayersAt(london, date, settings(HighLatitudeRuleChoice.SEVENTH_OF_THE_NIGHT)),
        )
        assertTrue(
            "the seventh rule did move Fajr on $date",
            PrayerTimeCalculator.forDate(london, date, settings(HighLatitudeRuleChoice.SEVENTH_OF_THE_NIGHT))!![Prayer.FAJR]
                .isAfter(PrayerTimeCalculator.forDate(london, date, settings(HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT))!![Prayer.FAJR]),
        )
    }

    @Test
    fun reportsNothingWhereTrueTwilightIsReached() {
        val date = LocalDate.of(2016, 6, 21)

        HighLatitudeRuleChoice.entries.forEach { rule ->
            assertTrue(
                "$rule should account for nothing in Dhaka on $date",
                boundedPrayersAt(
                    dhaka,
                    date,
                    CalculationSettings(CalculationMethodChoice.KARACHI, AsrJuristicMethod.STANDARD, rule),
                ).isEmpty(),
            )
        }
    }

    /** A method with a fixed Isha interval takes Isha from Maghrib, never a rule. */
    @Test
    fun neverReportsAnIshaThatComesFromAFixedInterval() {
        val date = LocalDate.of(2020, 6, 15)
        val ummAlQura = CalculationSettings(
            method = CalculationMethodChoice.UMM_AL_QURA,
            asrJuristicMethod = AsrJuristicMethod.STANDARD,
            highLatitudeRule = HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT,
        )

        assertEquals(setOf(Prayer.FAJR), boundedPrayersAt(edinburgh, date, ummAlQura))
    }

    /**
     * The Moonsighting Committee method applies its own seasonal adjustment at
     * high latitude, so the user's rule cannot be reported as having produced a
     * time, and cannot change the times either (`docs/PRAYER_TIMES.md`).
     */
    @Test
    fun neverReportsTheUsersRuleForAMethodWithItsOwn() {
        val date = LocalDate.of(2020, 6, 15)

        val daysPerRule = HighLatitudeRuleChoice.entries.map { rule ->
            val times = PrayerTimeCalculator.forDate(
                edinburgh,
                date,
                CalculationSettings(
                    method = CalculationMethodChoice.MOON_SIGHTING_COMMITTEE,
                    asrJuristicMethod = AsrJuristicMethod.STANDARD,
                    highLatitudeRule = rule,
                ),
            )!!
            assertTrue("$rule should account for nothing", times.boundedByHighLatitudeRule.isEmpty())
            times.times
        }

        assertEquals("the method ignores the rule entirely", 1, daysPerRule.distinct().size)
    }

    /** The Muslim World League settings the rule cases share, with [rule]. */
    private fun settings(rule: HighLatitudeRuleChoice) = CalculationSettings(
        method = CalculationMethodChoice.MUSLIM_WORLD_LEAGUE,
        asrJuristicMethod = AsrJuristicMethod.STANDARD,
        highLatitudeRule = rule,
    )

    /** The prayers the high-latitude rule produced at [city] on [date]. */
    private fun boundedPrayersAt(
        city: City,
        date: LocalDate,
        settings: CalculationSettings,
    ): Set<Prayer> = PrayerTimeCalculator.forDate(city, date, settings)!!.boundedByHighLatitudeRule

    private companion object {
        /** The Adhan reference cases' own tolerance: ±1 minute. */
        const val REFERENCE_TOLERANCE_MINUTES = 1L
    }
}
