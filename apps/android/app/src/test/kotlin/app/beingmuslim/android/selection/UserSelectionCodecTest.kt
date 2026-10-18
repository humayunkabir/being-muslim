package app.beingmuslim.android.selection

import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
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
}
