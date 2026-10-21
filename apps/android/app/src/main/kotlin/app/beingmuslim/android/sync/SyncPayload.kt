package app.beingmuslim.android.sync

import app.beingmuslim.android.hijri.HijriCalendar
import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.prayer.PrayerOffsets
import app.beingmuslim.android.selection.UserSelection
import org.json.JSONException
import org.json.JSONObject

/**
 * The user's settings as they travel between devices: everything the settings
 * screen controls except the city, which is location data and never leaves the
 * device (`docs/PRODUCT_REQUIREMENTS.md` §10, `docs/SYNC.md`).
 */
data class SyncedSettings(
    val calculationSettings: CalculationSettings,
    val hijriDayShift: Int,
    val offsets: PrayerOffsets,
    val language: String?,
)

/** Everything one device holds for its account: settings and the tasbih total. */
data class SyncPayload(
    val settings: SyncedSettings,
    val tasbihTotal: Int,
)

/** The sync payload built from what this device has stored. */
fun UserSelection.toSyncedSettings(language: String?): SyncedSettings = SyncedSettings(
    calculationSettings = settings,
    hijriDayShift = hijriDayShift,
    offsets = offsets,
    language = language,
)

/**
 * The stored JSON form of [SyncedSettings], versioned so a future shape can be
 * told apart from this one. Encoding is explicit; decoding is strict about the
 * version and the known enum values, because a payload this build cannot fully
 * understand must never be applied or overwritten (`docs/SYNC.md`).
 *
 * The tasbih total is a column of its own, not part of this object.
 */
object SyncCodec {

    /** Bump only when the shape changes incompatibly; older clients then leave it alone. */
    const val VERSION = 1

    private const val KEY_VERSION = "v"
    private const val KEY_METHOD = "calculationMethod"
    private const val KEY_ASR = "asrJuristicMethod"
    private const val KEY_RULE = "highLatitudeRule"
    private const val KEY_HIJRI_SHIFT = "hijriDayShift"
    private const val KEY_OFFSETS = "offsets"
    private const val KEY_LANGUAGE = "language"

    fun encode(settings: SyncedSettings): String = JSONObject().apply {
        put(KEY_VERSION, VERSION)
        put(KEY_METHOD, settings.calculationSettings.method.name)
        put(KEY_ASR, settings.calculationSettings.asrJuristicMethod.name)
        put(KEY_RULE, settings.calculationSettings.highLatitudeRule.name)
        put(KEY_HIJRI_SHIFT, settings.hijriDayShift)
        put(
            KEY_OFFSETS,
            JSONObject().apply {
                Prayer.entries.forEach { prayer -> put(prayer.name, settings.offsets[prayer]) }
            },
        )
        settings.language?.let { put(KEY_LANGUAGE, it) }
    }.toString()

    /** Null when the payload is unreadable, a version this build does not know, or has a value it cannot apply. */
    fun decode(json: String?): SyncedSettings? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = JSONObject(json)
            if (root.optInt(KEY_VERSION, 0) != VERSION) return null
            SyncedSettings(
                calculationSettings = CalculationSettings(
                    method = enumOrNull<CalculationMethodChoice>(root.optString(KEY_METHOD)) ?: return null,
                    asrJuristicMethod = enumOrNull<AsrJuristicMethod>(root.optString(KEY_ASR)) ?: return null,
                    highLatitudeRule = enumOrNull<HighLatitudeRuleChoice>(root.optString(KEY_RULE)) ?: return null,
                ),
                hijriDayShift = HijriCalendar.clampDayShift(root.optInt(KEY_HIJRI_SHIFT, 0)),
                offsets = offsetsFrom(root.optJSONObject(KEY_OFFSETS)),
                language = root.optString(KEY_LANGUAGE).takeIf { it.isNotBlank() },
            )
        } catch (_: JSONException) {
            null
        }
    }

    /** Each prayer decodes on its own, like the local store: one bad value loses only that offset. */
    private fun offsetsFrom(offsets: JSONObject?): PrayerOffsets =
        Prayer.entries.fold(PrayerOffsets()) { result, prayer ->
            result.with(prayer, offsets?.optInt(prayer.name, 0) ?: 0)
        }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        name?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } }
}
