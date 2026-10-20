package app.beingmuslim.android.selection

import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.PrayerOffsets

/**
 * The user's first-run choices: the city to calculate for, the calculation
 * settings to calculate with, the ±1-day shift they may have applied to the
 * calculated Hijri date to match their local announcement, and their own
 * per-prayer offsets. Persisted so they survive a restart, and never changed
 * without the user changing them (`docs/PRODUCT_REQUIREMENTS.md` §5, §7).
 * Offsets are held apart from [settings] so no calculation input is altered by
 * them (issue #8).
 */
data class UserSelection(
    val cityId: Long,
    val settings: CalculationSettings,
    val hijriDayShift: Int = 0,
    val offsets: PrayerOffsets = PrayerOffsets(),
)
