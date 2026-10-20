package app.beingmuslim.android.selection

import app.beingmuslim.android.hijri.HijriCalendar
import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.prayer.PrayerOffsets

/**
 * Encodes and decodes the user's [UserSelection] to and from flat string
 * key/values. Pure — no platform types — so the persisted form and its migration
 * behaviour are covered by ordinary JVM unit tests.
 *
 * Decoding is **lenient**: a missing, unparsable, or no-longer-known value
 * yields `null`, which the app treats as "not set up yet". A stored selection
 * written by an older build therefore falls back to first-run rather than
 * crashing, and a city id that is no longer in the catalog is handled by the
 * caller.
 */
object UserSelectionCodec {

    /** Preference keys. Stable — changing one silently discards a stored choice. */
    const val CITY_ID = "city_id"
    const val METHOD = "calculation_method"
    const val ASR_METHOD = "asr_juristic_method"
    const val HIGH_LATITUDE_RULE = "high_latitude_rule"

    /**
     * The user's ±1-day Hijri date shift. Absent in selections written before
     * the Hijri date shipped, which read as the calculated date.
     */
    const val HIJRI_DAY_SHIFT = "hijri_day_shift"

    /**
     * The user's own per-prayer offsets, in minutes. Absent in selections written
     * before offsets shipped, or for a prayer the user never adjusted, which read
     * as the raw calculated time (issue #8).
     */
    val PRAYER_OFFSET_KEYS: Map<Prayer, String> = mapOf(
        Prayer.FAJR to "offset_fajr",
        Prayer.DHUHR to "offset_dhuhr",
        Prayer.ASR to "offset_asr",
        Prayer.MAGHRIB to "offset_maghrib",
        Prayer.ISHA to "offset_isha",
    )

    /** Every key [decode] reads, for a caller assembling the stored values. */
    val KEYS = listOf(CITY_ID, METHOD, ASR_METHOD, HIGH_LATITUDE_RULE, HIJRI_DAY_SHIFT) +
        PRAYER_OFFSET_KEYS.values

    fun encode(selection: UserSelection): Map<String, String> = buildMap {
        put(CITY_ID, selection.cityId.toString())
        put(METHOD, selection.settings.method.name)
        put(ASR_METHOD, selection.settings.asrJuristicMethod.name)
        put(HIGH_LATITUDE_RULE, selection.settings.highLatitudeRule.name)
        put(HIJRI_DAY_SHIFT, selection.hijriDayShift.toString())
        PRAYER_OFFSET_KEYS.forEach { (prayer, key) ->
            put(key, selection.offsets[prayer].toString())
        }
    }

    fun decode(values: Map<String, String?>): UserSelection? {
        val cityId = values[CITY_ID]?.toLongOrNull() ?: return null
        val method = enumOrNull<CalculationMethodChoice>(values[METHOD]) ?: return null
        val asrJuristicMethod = enumOrNull<AsrJuristicMethod>(values[ASR_METHOD]) ?: return null
        val highLatitudeRule = enumOrNull<HighLatitudeRuleChoice>(values[HIGH_LATITUDE_RULE]) ?: return null
        val hijriDayShift = HijriCalendar.clampDayShift(values[HIJRI_DAY_SHIFT]?.toIntOrNull() ?: 0)
        // Each prayer decodes on its own: one corrupt value loses only that
        // prayer's adjustment, and the raw time is shown instead.
        val offsets = PRAYER_OFFSET_KEYS.entries.fold(PrayerOffsets()) { offsets, (prayer, key) ->
            offsets.with(prayer, values[key]?.toIntOrNull() ?: 0)
        }

        return UserSelection(
            cityId,
            CalculationSettings(method, asrJuristicMethod, highLatitudeRule),
            hijriDayShift,
            offsets,
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        name?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } }
}
