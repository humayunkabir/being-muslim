package app.beingmuslim.android.reminder

import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.prayer.PrayerOffsets
import app.beingmuslim.android.prayer.PrayerTimeCalculator
import java.time.Instant
import java.time.ZoneId

/** One notification the app should ask the operating system to deliver. */
data class Reminder(val prayer: Prayer, val at: Instant)

/**
 * The whole reminder scheduling policy (issue #12): which prayers the user asked
 * to be reminded about, at the time they see (the raw calculated time plus their
 * own offset), for the next [HORIZON_DAYS] days, skipping any that have already
 * passed and any inside the user's quiet hours.
 *
 * Pure by design — the current instant is passed in, and nothing here touches
 * Android, storage, or the notification system. A skipped quiet-hours reminder is
 * dropped, not moved to the end of the window: the prompt belongs to the prayer,
 * not to the first moment the phone may beep.
 *
 * Quiet hours are read in the city's zone, because that is the clock the times
 * are shown on. A polar day or night has no times to remind for and is skipped.
 */
object ReminderSchedule {

    /** How many days ahead are scheduled, today included. */
    const val HORIZON_DAYS = 7

    fun upcoming(
        city: City,
        settings: CalculationSettings,
        offsets: PrayerOffsets,
        preferences: ReminderPreferences,
        now: Instant,
    ): List<Reminder> {
        if (preferences.enabledPrayers.isEmpty()) return emptyList()

        val zone = ZoneId.of(city.timezone)
        val today = now.atZone(zone).toLocalDate()
        return (0 until HORIZON_DAYS)
            .flatMap { dayOffset ->
                val day = PrayerTimeCalculator.forDate(
                    city,
                    today.plusDays(dayOffset.toLong()),
                    settings,
                ) ?: return@flatMap emptyList()
                Prayer.entries.mapNotNull { prayer ->
                    if (prayer !in preferences.enabledPrayers) return@mapNotNull null
                    val at = offsets.adjusted(prayer, day[prayer])
                    val quiet = preferences.quietHoursEnabled &&
                        preferences.quietHours.contains(at.atZone(zone).toLocalTime())
                    if (quiet || !at.isAfter(now)) null else Reminder(prayer, at)
                }
            }
            .sortedBy { it.at }
    }
}
