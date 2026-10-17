package app.beingmuslim.android.selection

import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings

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

    /** Every key [decode] reads, for a caller assembling the stored values. */
    val KEYS = listOf(CITY_ID, METHOD, ASR_METHOD)

    fun encode(selection: UserSelection): Map<String, String> = mapOf(
        CITY_ID to selection.cityId.toString(),
        METHOD to selection.settings.method.name,
        ASR_METHOD to selection.settings.asrJuristicMethod.name,
    )

    fun decode(values: Map<String, String?>): UserSelection? {
        val cityId = values[CITY_ID]?.toLongOrNull() ?: return null
        val method = enumOrNull<CalculationMethodChoice>(values[METHOD]) ?: return null
        val asrJuristicMethod = enumOrNull<AsrJuristicMethod>(values[ASR_METHOD]) ?: return null

        return UserSelection(cityId, CalculationSettings(method, asrJuristicMethod))
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        name?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } }
}
