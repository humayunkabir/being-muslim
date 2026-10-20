package app.beingmuslim.android.selection

import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.prayer.PrayerOffsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The persisted form of the user's selection — first-run choices that must
 * survive a restart. Decoding is deliberately lenient so a value written by an
 * older build falls back to setup rather than crashing.
 */
class UserSelectionCodecTest {

    private val selection = UserSelection(
        cityId = 1185241L,
        settings = CalculationSettings(
            method = CalculationMethodChoice.KARACHI,
            asrJuristicMethod = AsrJuristicMethod.HANAFI,
            highLatitudeRule = HighLatitudeRuleChoice.SEVENTH_OF_THE_NIGHT,
        ),
    )

    @Test
    fun roundTripsASelection() {
        val restored = UserSelectionCodec.decode(UserSelectionCodec.encode(selection))

        assertEquals(selection, restored)
    }

    @Test
    fun aSelectionWithNoStoredCityIsAbsent() {
        val stored = UserSelectionCodec.encode(selection) - UserSelectionCodec.CITY_ID

        assertNull(UserSelectionCodec.decode(stored))
    }

    @Test
    fun aCityIdThatIsNotANumberIsTreatedAsAbsent() {
        val stored = UserSelectionCodec.encode(selection) + (UserSelectionCodec.CITY_ID to "not-a-number")

        assertNull(UserSelectionCodec.decode(stored))
    }

    @Test
    fun aCalculationMethodThisBuildNoLongerKnowsIsTreatedAsAbsent() {
        val stored = UserSelectionCodec.encode(selection) + (UserSelectionCodec.METHOD to "SOME_RETIRED_METHOD")

        assertNull(UserSelectionCodec.decode(stored))
    }

    @Test
    fun anAsrMethodThisBuildNoLongerKnowsIsTreatedAsAbsent() {
        val stored = UserSelectionCodec.encode(selection) + (UserSelectionCodec.ASR_METHOD to "SOME_RETIRED_METHOD")

        assertNull(UserSelectionCodec.decode(stored))
    }

    @Test
    fun aHighLatitudeRuleThisBuildNoLongerKnowsIsTreatedAsAbsent() {
        val stored = UserSelectionCodec.encode(selection) +
            (UserSelectionCodec.HIGH_LATITUDE_RULE to "SOME_RETIRED_RULE")

        assertNull(UserSelectionCodec.decode(stored))
    }

    /**
     * Issue #7 added the rule to the stored selection. A selection stored before
     * it existed has no rule in it, and is treated as absent like any other
     * missing value — the user is asked again rather than a rule being picked for
     * them.
     */
    @Test
    fun aSelectionStoredBeforeTheRuleWasChosenIsTreatedAsAbsent() {
        val stored = UserSelectionCodec.encode(selection) - UserSelectionCodec.HIGH_LATITUDE_RULE

        assertNull(UserSelectionCodec.decode(stored))
    }

    @Test
    fun roundTripsTheHijriDayShift() {
        val shifted = selection.copy(hijriDayShift = 1)

        assertEquals(shifted, UserSelectionCodec.decode(UserSelectionCodec.encode(shifted)))
    }

    @Test
    fun aSelectionStoredBeforeTheHijriDateExistedHasNoShift() {
        // Lenient, like every other missing value here: an older build's stored
        // selection keeps working, with the calculated date and no shift.
        val stored = UserSelectionCodec.encode(selection) - UserSelectionCodec.HIJRI_DAY_SHIFT

        assertEquals(0, UserSelectionCodec.decode(stored)?.hijriDayShift)
    }

    @Test
    fun aShiftThatIsNotANumberIsTreatedAsNone() {
        val stored = UserSelectionCodec.encode(selection) + (UserSelectionCodec.HIJRI_DAY_SHIFT to "later")

        assertEquals(0, UserSelectionCodec.decode(stored)?.hijriDayShift)
    }

    @Test
    fun aShiftBeyondTheOneDayOfferedIsClamped() {
        val stored = UserSelectionCodec.encode(selection) + (UserSelectionCodec.HIJRI_DAY_SHIFT to "9")

        assertEquals(1, UserSelectionCodec.decode(stored)?.hijriDayShift)
    }

    @Test
    fun roundTripsTheUsersPrayerTimeOffsets() {
        val adjusted = selection.copy(
            offsets = PrayerOffsets()
                .with(Prayer.FAJR, 5)
                .with(Prayer.ISHA, -2),
        )

        assertEquals(adjusted, UserSelectionCodec.decode(UserSelectionCodec.encode(adjusted)))
    }

    @Test
    fun aSelectionStoredBeforeOffsetsExistedHasNone() {
        // Existing users upgrading must not gain an adjustment they never made.
        val stored = UserSelectionCodec.encode(selection) - UserSelectionCodec.PRAYER_OFFSET_KEYS.values

        assertEquals(PrayerOffsets(), UserSelectionCodec.decode(stored)?.offsets)
    }

    @Test
    fun anOffsetThatIsNotANumberIsTreatedAsNone() {
        val stored = UserSelectionCodec.encode(selection) +
            (UserSelectionCodec.PRAYER_OFFSET_KEYS.getValue(Prayer.DHUHR) to "later")

        assertEquals(0, UserSelectionCodec.decode(stored)?.offsets?.get(Prayer.DHUHR))
    }

    @Test
    fun anOffsetBeyondTheSupportedRangeIsClamped() {
        val stored = UserSelectionCodec.encode(selection) +
            (UserSelectionCodec.PRAYER_OFFSET_KEYS.getValue(Prayer.ASR) to "500")

        assertEquals(
            PrayerOffsets.MAX_MINUTES,
            UserSelectionCodec.decode(stored)?.offsets?.get(Prayer.ASR),
        )
    }

    @Test
    fun everyPrayerHasAStoredOffsetKey() {
        // A new prayer without a key would silently lose its offset on restart.
        assertEquals(Prayer.entries.toSet(), UserSelectionCodec.PRAYER_OFFSET_KEYS.keys)
    }
}
