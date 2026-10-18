package app.beingmuslim.android.prayer

/**
 * The times for a city's current local date and the day after, so the times
 * screen can show both days (`docs/PRODUCT_REQUIREMENTS.md` §2.1).
 *
 * Each day is null on its own when the sun neither rises nor sets there that
 * day — an unavailable state, not an error.
 */
data class TwoDayPrayerTimes(
    val today: DailyPrayerTimes?,
    val tomorrow: DailyPrayerTimes?,
)
