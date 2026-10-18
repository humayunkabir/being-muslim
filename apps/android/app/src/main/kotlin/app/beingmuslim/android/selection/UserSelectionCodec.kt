package app.beingmuslim.android.selection

import app.beingmuslim.android.hijri.HijriCalendar
import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice

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

    /** Every key [decode] reads, for a caller assembling the stored values. */
    val KEYS = listOf(CITY_ID, METHOD, ASR_METHOD, HIGH_LATITUDE_RULE, HIJRI_DAY_SHIFT)

    fun encode(selection: UserSelection): Map<String, String> = mapOf(
        CITY_ID to selection.cityId.toString(),
        METHOD to selection.settings.method.name,
        ASR_METHOD to selection.settings.asrJuristicMethod.name,
        HIGH_LATITUDE_RULE to selection.settings.highLatitudeRule.name,
        HIJRI_DAY_SHIFT to selection.hijriDayShift.toString(),
    )

    fun decode(values: Map<String, String?>): UserSelection? {
        val cityId = values[CITY_ID]?.toLongOrNull() ?: return null
        val method = enumOrNull<CalculationMethodChoice>(values[METHOD]) ?: return null
        val asrJuristicMethod = enumOrNull<AsrJuristicMethod>(values[ASR_METHOD]) ?: return null
        val highLatitudeRule = enumOrNull<HighLatitudeRuleChoice>(values[HIGH_LATITUDE_RULE]) ?: return null
        val hijriDayShift = HijriCalendar.clampDayShift(values[HIJRI_DAY_SHIFT]?.toIntOrNull() ?: 0)

        return UserSelection(
            cityId,
            CalculationSettings(method, asrJuristicMethod, highLatitudeRule),
            hijriDayShift,
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        name?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } }
}
