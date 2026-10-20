package app.beingmuslim.android.reminder

import app.beingmuslim.android.prayer.Prayer
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The persisted form of the reminder preferences (issue #12). Reminders are off
 * by default, and a value written by an older or corrupt build falls back to the
 * default rather than to a reminder the user never asked for.
 */
class ReminderPreferencesCodecTest {

    @Test
    fun nothingIsEnabledByDefault() {
        val stored = ReminderPreferencesCodec.decode(emptyMap())

        assertEquals(emptySet<Prayer>(), stored.enabledPrayers)
        assertFalse(stored.quietHoursEnabled)
    }

    @Test
    fun theDefaultQuietWindowIsLateEveningToEarlyMorning() {
        val stored = ReminderPreferencesCodec.decode(emptyMap())

        assertEquals(LocalTime.of(22, 0), stored.quietHours.start)
        assertEquals(LocalTime.of(6, 0), stored.quietHours.end)
    }

    @Test
    fun roundTripsTheUsersPreferences() {
        val preferences = ReminderPreferences(
            enabledPrayers = setOf(Prayer.FAJR, Prayer.ISHA),
            quietHoursEnabled = true,
            quietHours = QuietHours(LocalTime.of(21, 30), LocalTime.of(5, 15)),
        )

        assertEquals(
            preferences,
            ReminderPreferencesCodec.decode(ReminderPreferencesCodec.encode(preferences)),
        )
    }

    @Test
    fun everyPrayerHasItsOwnStoredKey() {
        assertEquals(Prayer.entries.toSet(), ReminderPreferencesCodec.PRAYER_KEYS.keys)
    }

    @Test
    fun aPrayerKeyThatIsNotTrueReadsAsOff() {
        val stored = mapOf(ReminderPreferencesCodec.PRAYER_KEYS.getValue(Prayer.DHUHR) to "sometimes")

        assertEquals(emptySet<Prayer>(), ReminderPreferencesCodec.decode(stored).enabledPrayers)
    }

    @Test
    fun aQuietHoursFlagThatIsNotBooleanReadsAsOff() {
        val stored = mapOf(ReminderPreferencesCodec.QUIET_HOURS_ENABLED to "maybe")

        assertFalse(ReminderPreferencesCodec.decode(stored).quietHoursEnabled)
    }

    @Test
    fun aQuietHourThatIsNotAValidTimeFallsBackToTheDefault() {
        val stored = mapOf(
            ReminderPreferencesCodec.QUIET_HOURS_START to "later",
            ReminderPreferencesCodec.QUIET_HOURS_END to "1440",
        )

        val quietHours = ReminderPreferencesCodec.decode(stored).quietHours
        assertEquals(LocalTime.of(22, 0), quietHours.start)
        assertEquals(LocalTime.of(6, 0), quietHours.end)
    }

    @Test
    fun aMissingQuietHourFallsBackToTheDefault() {
        val stored = mapOf(ReminderPreferencesCodec.QUIET_HOURS_START to "60")

        assertEquals(LocalTime.of(6, 0), ReminderPreferencesCodec.decode(stored).quietHours.end)
        assertEquals(LocalTime.of(1, 0), ReminderPreferencesCodec.decode(stored).quietHours.start)
    }

    @Test
    fun theCodecNeverInventsAPrayerInTheUsersSet() {
        val stored = ReminderPreferencesCodec.encode(
            ReminderPreferences(enabledPrayers = setOf(Prayer.ASR)),
        )

        assertEquals(setOf(Prayer.ASR), ReminderPreferencesCodec.decode(stored).enabledPrayers)
        assertNull(stored[ReminderPreferencesCodec.PRAYER_KEYS.getValue(Prayer.FAJR)])
    }
}
