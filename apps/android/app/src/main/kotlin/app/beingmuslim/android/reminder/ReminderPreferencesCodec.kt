package app.beingmuslim.android.reminder

import app.beingmuslim.android.prayer.Prayer
import java.time.LocalTime

/**
 * The persisted form of [ReminderPreferences] (issue #12).
 *
 * Decoding is deliberately lenient and defaults to *off*: a missing, corrupt or
 * older value must never turn on a reminder the user did not ask for. Only
 * enabled prayers are written, so absence means off.
 */
object ReminderPreferencesCodec {

    val PRAYER_KEYS: Map<Prayer, String> = Prayer.entries.associateWith { "reminder_${it.name.lowercase()}" }

    const val QUIET_HOURS_ENABLED = "quiet_hours_enabled"
    const val QUIET_HOURS_START = "quiet_hours_start_minutes"
    const val QUIET_HOURS_END = "quiet_hours_end_minutes"

    /** Every key [decode] reads. */
    val KEYS = PRAYER_KEYS.values + listOf(QUIET_HOURS_ENABLED, QUIET_HOURS_START, QUIET_HOURS_END)

    fun encode(preferences: ReminderPreferences): Map<String, String> = buildMap {
        preferences.enabledPrayers.forEach { put(PRAYER_KEYS.getValue(it), "true") }
        put(QUIET_HOURS_ENABLED, preferences.quietHoursEnabled.toString())
        put(QUIET_HOURS_START, preferences.quietHours.start.minuteOfDay().toString())
        put(QUIET_HOURS_END, preferences.quietHours.end.minuteOfDay().toString())
    }

    fun decode(values: Map<String, String?>): ReminderPreferences {
        val defaults = QuietHours()
        return ReminderPreferences(
            enabledPrayers = PRAYER_KEYS.filterValues { values[it] == "true" }.keys,
            quietHoursEnabled = values[QUIET_HOURS_ENABLED]?.toBooleanStrictOrNull() ?: false,
            quietHours = QuietHours(
                start = values[QUIET_HOURS_START].minuteOfDayOr(defaults.start),
                end = values[QUIET_HOURS_END].minuteOfDayOr(defaults.end),
            ),
        )
    }

    private fun String?.minuteOfDayOr(default: LocalTime): LocalTime =
        this?.toIntOrNull()
            ?.takeIf { it in 0 until MINUTES_PER_DAY }
            ?.let { LocalTime.of(it / 60, it % 60) }
            ?: default

    private fun LocalTime.minuteOfDay(): Int = hour * 60 + minute

    private const val MINUTES_PER_DAY = 24 * 60
}
