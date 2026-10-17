package app.beingmuslim.android.prayer

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The five calculated prayer times for one city and one local date.
 *
 * The [date] is the local date at the city and [zone] its IANA zone; each entry
 * in [times] is the true instant the prayer falls at, so daylight saving is
 * already accounted for and the caller only has to format in [zone].
 */
data class DailyPrayerTimes(
    val date: LocalDate,
    val zone: ZoneId,
    val times: Map<Prayer, Instant>,
) {
    operator fun get(prayer: Prayer): Instant = times.getValue(prayer)
}
