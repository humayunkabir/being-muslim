package app.beingmuslim.android.prayer

import java.time.Instant

/**
 * The user's own adjustments to calculated prayer times, in whole minutes, per
 * prayer (issue #8). They are kept separate from [CalculationSettings] and from
 * the raw calculation, so no stored input and no reported parameter is changed
 * by them.
 *
 * Only prayers the user has adjusted are present; every other prayer reads as 0,
 * which is the default and means the raw calculated time.
 */
data class PrayerOffsets(val minutes: Map<Prayer, Int> = emptyMap()) {

    operator fun get(prayer: Prayer): Int = minutes[prayer] ?: 0

    val isEmpty: Boolean get() = minutes.values.all { it == 0 }

    /** [time] moved by the adjustment for [prayer] — the time to show. */
    fun adjusted(prayer: Prayer, time: Instant): Instant =
        time.plusSeconds(get(prayer) * 60L)

    /** These offsets with [prayer] adjusted by [value] minutes, clamped to the supported range; 0 removes it. */
    fun with(prayer: Prayer, value: Int): PrayerOffsets {
        val clamped = value.coerceIn(-MAX_MINUTES, MAX_MINUTES)
        val next = minutes.toMutableMap()
        if (clamped == 0) next.remove(prayer) else next[prayer] = clamped
        return PrayerOffsets(next)
    }

    companion object {
        /** The most a prayer time may be moved either way, in minutes. */
        const val MAX_MINUTES = 60
    }
}
