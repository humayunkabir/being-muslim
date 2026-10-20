package app.beingmuslim.android.reminder

import app.beingmuslim.android.prayer.Prayer
import java.time.LocalTime

/**
 * A window in which the user does not want to be prompted, in the chosen city's
 * local time — the same clock the prayer times are shown on. A window that
 * crosses midnight (start later than end) is read as wrapping, so 22:00–06:00
 * covers the late evening and the early morning. Equal start and end means an
 * empty window, not a whole day.
 */
data class QuietHours(
    val start: LocalTime = LocalTime.of(22, 0),
    val end: LocalTime = LocalTime.of(6, 0),
) {
    fun contains(time: LocalTime): Boolean = when {
        start == end -> false
        start < end -> time >= start && time < end
        else -> time >= start || time < end
    }
}

/**
 * What the user asked to be reminded about (issue #12). Reminders are off until
 * the user turns one on, one prayer at a time; nothing here requests a
 * notification permission or schedules anything — [ReminderSchedule] decides
 * what would be sent, and the platform layer sends it.
 */
data class ReminderPreferences(
    val enabledPrayers: Set<Prayer> = emptySet(),
    val quietHoursEnabled: Boolean = false,
    val quietHours: QuietHours = QuietHours(),
) {
    /** These preferences with [prayer]'s reminder turned on or off. */
    fun withReminder(prayer: Prayer, enabled: Boolean): ReminderPreferences = copy(
        enabledPrayers = if (enabled) enabledPrayers + prayer else enabledPrayers - prayer,
    )
}
