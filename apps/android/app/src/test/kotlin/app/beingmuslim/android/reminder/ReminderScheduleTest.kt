package app.beingmuslim.android.reminder

import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.prayer.PrayerOffsets
import app.beingmuslim.android.prayer.PrayerTimeCalculator
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reminder scheduling policy (issue #12): which prayers produce a
 * notification, at what instant, and which are skipped by quiet hours.
 *
 * The policy is pure — the current time is passed in and nothing here touches
 * Android, storage, or the notification system.
 */
class ReminderScheduleTest {

    private val dhaka = City(
        id = 1185241L, name = "Dhaka", bengaliName = "ঢাকা",
        latitude = 23.7104, longitude = 90.40744,
        countryCode = "BD", timezone = "Asia/Dhaka",
    )
    private val settings = CalculationSettings(
        method = CalculationMethodChoice.MUSLIM_WORLD_LEAGUE,
        asrJuristicMethod = AsrJuristicMethod.STANDARD,
        highLatitudeRule = HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT,
    )
    private val zone = ZoneId.of(dhaka.timezone)

    /** Midnight at the city, before any of that day's prayers. */
    private val midnight = Instant.parse("2026-10-19T18:00:00Z")

    private fun scheduled(
        preferences: ReminderPreferences,
        now: Instant = midnight,
        offsets: PrayerOffsets = PrayerOffsets(),
    ) = ReminderSchedule.upcoming(dhaka, settings, offsets, preferences, now)

    private fun rawTime(prayer: Prayer, dayOffset: Long = 0): Instant {
        val date = midnight.atZone(zone).toLocalDate().plusDays(dayOffset)
        return PrayerTimeCalculator.forDate(dhaka, date, settings)!![prayer]
    }

    @Test
    fun noReminderIsScheduledUntilTheUserEnablesOne() {
        assertEquals(emptyList<Reminder>(), scheduled(ReminderPreferences()))
    }

    @Test
    fun onlyThePrayersTheUserEnabledAreScheduled() {
        val reminders = scheduled(ReminderPreferences(enabledPrayers = setOf(Prayer.FAJR, Prayer.ISHA)))

        assertTrue(reminders.isNotEmpty())
        assertEquals(setOf(Prayer.FAJR, Prayer.ISHA), reminders.map { it.prayer }.toSet())
    }

    @Test
    fun aReminderOnEachOfTheNextSevenDaysIsScheduled() {
        val reminders = scheduled(ReminderPreferences(enabledPrayers = setOf(Prayer.FAJR)))

        assertEquals(ReminderSchedule.HORIZON_DAYS, reminders.size)
        assertEquals(
            (0 until ReminderSchedule.HORIZON_DAYS).map { rawTime(Prayer.FAJR, it.toLong()) },
            reminders.map { it.at },
        )
    }

    @Test
    fun aPrayerWhoseTimeHasPassedIsNotScheduled() {
        val afterFajr = rawTime(Prayer.FAJR).plusSeconds(60)
        val reminders = scheduled(
            now = afterFajr,
            preferences = ReminderPreferences(enabledPrayers = setOf(Prayer.FAJR, Prayer.DHUHR)),
        )

        assertEquals(rawTime(Prayer.DHUHR), reminders.first().at)
        assertFalse(reminders.any { it.prayer == Prayer.FAJR && it.at == rawTime(Prayer.FAJR) })
    }

    @Test
    fun quietHoursSkipAReminderRatherThanDelayingIt() {
        val dhuhr = rawTime(Prayer.DHUHR).atZone(zone).toLocalTime()
        val preferences = ReminderPreferences(
            enabledPrayers = setOf(Prayer.DHUHR),
            quietHoursEnabled = true,
            quietHours = QuietHours(dhuhr.minusMinutes(5), dhuhr.plusMinutes(5)),
        )

        assertTrue(scheduled(preferences).isEmpty())
    }

    @Test
    fun quietHoursThatWrapPastMidnightSuppressTheEarlyPrayer() {
        val fajr = rawTime(Prayer.FAJR).atZone(zone).toLocalTime()
        val preferences = ReminderPreferences(
            enabledPrayers = setOf(Prayer.FAJR),
            quietHoursEnabled = true,
            quietHours = QuietHours(LocalTime.of(23, 0), fajr.plusMinutes(5)),
        )

        assertTrue(scheduled(preferences).isEmpty())
    }

    @Test
    fun quietHoursAreNotAppliedWhenTheUserHasTurnedThemOff() {
        val dhuhr = rawTime(Prayer.DHUHR).atZone(zone).toLocalTime()
        val preferences = ReminderPreferences(
            enabledPrayers = setOf(Prayer.DHUHR),
            quietHoursEnabled = false,
            quietHours = QuietHours(dhuhr.minusMinutes(5), dhuhr.plusMinutes(5)),
        )

        assertEquals(ReminderSchedule.HORIZON_DAYS, scheduled(preferences).size)
    }

    @Test
    fun aQuietWindowWithTheSameStartAndEndSuppressesNothing() {
        val quietHours = QuietHours(LocalTime.of(22, 0), LocalTime.of(22, 0))

        assertFalse(quietHours.contains(LocalTime.of(22, 0)))
        assertFalse(quietHours.contains(LocalTime.NOON))
    }

    @Test
    fun theUsersOffsetsMoveTheReminderWithTheTimeTheySee() {
        val offsets = PrayerOffsets().with(Prayer.DHUHR, 5)
        val reminders = scheduled(
            preferences = ReminderPreferences(enabledPrayers = setOf(Prayer.DHUHR)),
            offsets = offsets,
        )

        assertEquals(rawTime(Prayer.DHUHR).plusSeconds(5 * 60), reminders.first().at)
    }

    @Test
    fun aDifferentCalculationMethodMovesTheReminder() {
        // North America uses a 15° Fajr angle; the default method uses 18°.
        val northAmerica = CalculationSettings(
            method = CalculationMethodChoice.NORTH_AMERICA,
            asrJuristicMethod = AsrJuristicMethod.STANDARD,
            highLatitudeRule = HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT,
        )
        val preferences = ReminderPreferences(enabledPrayers = setOf(Prayer.FAJR))

        val defaultFajr = ReminderSchedule.upcoming(dhaka, settings, PrayerOffsets(), preferences, midnight)
        val northAmericaFajr = ReminderSchedule.upcoming(dhaka, northAmerica, PrayerOffsets(), preferences, midnight)

        assertTrue(
            "the method's Fajr angle must change the scheduled instant",
            defaultFajr.first().at != northAmericaFajr.first().at,
        )
    }

    @Test
    fun remindersAreInTheOrderTheyWillFire() {
        val reminders = scheduled(
            ReminderPreferences(enabledPrayers = Prayer.entries.toSet()),
        )

        assertEquals(reminders.sortedBy { it.at }, reminders)
    }
}
