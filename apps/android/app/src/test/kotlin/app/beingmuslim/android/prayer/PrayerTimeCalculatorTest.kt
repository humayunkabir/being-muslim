package app.beingmuslim.android.prayer

import app.beingmuslim.android.citycatalog.City
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
    private val longyearbyen = City(
        id = 2729907L, name = "Longyearbyen", bengaliName = null,
        latitude = 78.2232, longitude = 15.6267,
        countryCode = "SJ", timezone = "Arctic/Longyearbyen",
    )

    private fun assertTimesOn(
        city: City,
        date: LocalDate,
        settings: CalculationSettings,
        expected: Map<Prayer, LocalTime>,
    ) {
        val times = PrayerTimeCalculator.forDate(city, date, settings)
        assertEquals("no times computed for $city on $date", expected.keys, times!!.times.keys)
        expected.forEach { (prayer, wallTime) ->
            val actual = times[prayer].atZone(times.zone).toLocalTime()
            val drift = abs(Duration.between(wallTime, actual).toMinutes())
            assertTrue(
                "${city.name} $date $prayer: expected ~$wallTime but was $actual",
                drift <= REFERENCE_TOLERANCE_MINUTES,
            )
        }
    }

    @Test
    fun matchesTheLondonMoonsightingCommitteeReferenceCases() {
        val settings = CalculationSettings(
            method = CalculationMethodChoice.MOON_SIGHTING_COMMITTEE,
            asrJuristicMethod = AsrJuristicMethod.HANAFI,
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

    @Test
    fun matchesTheMakkahUmmAlQuraReferenceCases() {
        val settings = CalculationSettings(
            method = CalculationMethodChoice.UMM_AL_QURA,
            asrJuristicMethod = AsrJuristicMethod.STANDARD,
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
    fun theHanafiAsrIsLaterThanTheStandardAsr() {
        val standard = CalculationSettings(CalculationMethodChoice.KARACHI, AsrJuristicMethod.STANDARD)
        val hanafi = CalculationSettings(CalculationMethodChoice.KARACHI, AsrJuristicMethod.HANAFI)

        val standardAsr = PrayerTimeCalculator.forDate(dhaka, LocalDate.of(2016, 6, 21), standard)!![Prayer.ASR]
        val hanafiAsr = PrayerTimeCalculator.forDate(dhaka, LocalDate.of(2016, 6, 21), hanafi)!![Prayer.ASR]

        assertTrue("Hanafi Asr should be later", hanafiAsr.isAfter(standardAsr))
    }

    @Test
    fun anInstantResolvesToTheCitiesLocalDate() {
        // 2016-07-01T20:00Z is already the 2nd of July in Dhaka (UTC+6).
        val times = PrayerTimeCalculator.forInstant(
            dhaka,
            Instant.parse("2016-07-01T20:00:00Z"),
            CalculationSettings(CalculationMethodChoice.KARACHI, AsrJuristicMethod.STANDARD),
        )

        assertEquals(LocalDate.of(2016, 7, 2), times!!.date)
        assertEquals(ZoneId.of("Asia/Dhaka"), times.zone)
    }

    @Test
    fun returnsNoTimesWhenTheSunNeitherRisesNorSets() {
        val settings = CalculationSettings(
            method = CalculationMethodChoice.MUSLIM_WORLD_LEAGUE,
            asrJuristicMethod = AsrJuristicMethod.STANDARD,
        )

        assertNull(PrayerTimeCalculator.forDate(longyearbyen, LocalDate.of(2016, 6, 21), settings))
    }

    private companion object {
        /** The Adhan reference cases' own tolerance: ±1 minute. */
        const val REFERENCE_TOLERANCE_MINUTES = 1L
    }
}
