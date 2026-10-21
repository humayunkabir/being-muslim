package app.beingmuslim.android.sync

import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.prayer.PrayerOffsets
import app.beingmuslim.android.selection.UserSelection
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The synced payload's shape: exactly the settings that may leave the device
 * and the tasbih total — no city or location, no label or target, no devotional
 * timestamp (`docs/SYNC.md`).
 */
class SyncCodecTest {

    private val settings = SyncedSettings(
        calculationSettings = CalculationSettings(
            method = CalculationMethodChoice.KARACHI,
            asrJuristicMethod = AsrJuristicMethod.HANAFI,
            highLatitudeRule = HighLatitudeRuleChoice.SEVENTH_OF_THE_NIGHT,
        ),
        hijriDayShift = 1,
        offsets = PrayerOffsets()
            .with(Prayer.FAJR, 5)
            .with(Prayer.ISHA, -3),
        language = "bn",
    )

    @Test
    fun roundTripsTheSyncedSettings() {
        assertEquals(settings, SyncCodec.decode(SyncCodec.encode(settings)))
    }

    @Test
    fun thePayloadCarriesOnlyTheAllowedKeys() {
        val keys = JSONObject(SyncCodec.encode(settings)).keys().asSequence().toSet()

        assertEquals(
            setOf(
                "v",
                "calculationMethod",
                "asrJuristicMethod",
                "highLatitudeRule",
                "hijriDayShift",
                "offsets",
                "language",
            ),
            keys,
        )
    }

    @Test
    fun theCityIsNeverPartOfThePayload() {
        val selection = UserSelection(
            cityId = 1185241L,
            settings = settings.calculationSettings,
            hijriDayShift = 0,
            offsets = PrayerOffsets(),
        )

        val json = SyncCodec.encode(selection.toSyncedSettings("en"))

        assertFalse(json.contains("city", ignoreCase = true))
        assertFalse(json.contains("1185241"))
    }

    @Test
    fun thereIsNoDevotionalTimestampInThePayload() {
        val json = SyncCodec.encode(settings).lowercase()

        assertFalse(json.contains("time"))
        assertFalse(json.contains("date"))
    }

    @Test
    fun aPayloadFromANewerVersionIsNotUnderstood() {
        val newer = JSONObject(SyncCodec.encode(settings)).put("v", SyncCodec.VERSION + 1).toString()

        assertNull(SyncCodec.decode(newer))
    }

    @Test
    fun anUnreadablePayloadIsNotUnderstood() {
        assertNull(SyncCodec.decode("not json"))
        assertNull(SyncCodec.decode(null))
        assertNull(SyncCodec.decode(""))
    }

    @Test
    fun aCalculationMethodThisBuildNoLongerKnowsIsNotUnderstood() {
        val stored = JSONObject(SyncCodec.encode(settings))
            .put("calculationMethod", "SOME_RETIRED_METHOD")
            .toString()

        assertNull(SyncCodec.decode(stored))
    }

    @Test
    fun anUnknownAsrOrHighLatitudeValueIsNotUnderstood() {
        val root = JSONObject(SyncCodec.encode(settings))

        assertNull(SyncCodec.decode(root.put("asrJuristicMethod", "SOME_RETIRED_METHOD").toString()))
        assertNull(SyncCodec.decode(root.put("highLatitudeRule", "SOME_RETIRED_RULE").toString()))
    }

    @Test
    fun anOffsetBeyondTheSupportedRangeIsClamped() {
        val stored = JSONObject(SyncCodec.encode(settings))
            .put("offsets", JSONObject().put(Prayer.ASR.name, 500))
            .toString()

        assertEquals(PrayerOffsets.MAX_MINUTES, SyncCodec.decode(stored)?.offsets?.get(Prayer.ASR))
    }

    @Test
    fun aMissingLanguageStaysMissing() {
        val root = JSONObject(SyncCodec.encode(settings))
        root.remove("language")

        assertNull(SyncCodec.decode(root.toString())?.language)
    }

    @Test
    fun anOffsetThisBuildDoesNotRecogniseLosesOnlyThatOffset() {
        val stored = JSONObject(SyncCodec.encode(settings))
            .put("offsets", JSONObject().put("SOME_RETIRED_PRAYER", 5))
            .toString()

        assertEquals(PrayerOffsets(), SyncCodec.decode(stored)?.offsets)
    }

    @Test
    fun theEncodedFormNamesEveryPrayer() {
        val offsets = JSONObject(SyncCodec.encode(settings)).getJSONObject("offsets")

        assertTrue(Prayer.entries.all { offsets.has(it.name) })
    }
}
