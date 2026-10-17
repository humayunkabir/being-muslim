package app.beingmuslim.android.selection

import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
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
}
