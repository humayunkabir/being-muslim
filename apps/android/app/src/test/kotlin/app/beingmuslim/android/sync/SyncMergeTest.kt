package app.beingmuslim.android.sync

import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import app.beingmuslim.android.prayer.Prayer
import app.beingmuslim.android.prayer.PrayerOffsets
import app.beingmuslim.android.selection.UserSelection
import app.beingmuslim.android.tasbih.TasbihCounter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The merge a pull performs on this device's state: the account's settings are
 * adopted, but the city stays local and the tasbih label and target stay the
 * user's own (`docs/SYNC.md`).
 */
class SyncMergeTest {

    private val local = UserSelection(
        cityId = 1185241L,
        settings = CalculationSettings(
            method = CalculationMethodChoice.KARACHI,
            asrJuristicMethod = AsrJuristicMethod.HANAFI,
            highLatitudeRule = HighLatitudeRuleChoice.SEVENTH_OF_THE_NIGHT,
        ),
        hijriDayShift = 1,
        offsets = PrayerOffsets().with(Prayer.FAJR, 5),
    )

    private val synced = SyncedSettings(
        calculationSettings = CalculationSettings(
            method = CalculationMethodChoice.UMM_AL_QURA,
            asrJuristicMethod = AsrJuristicMethod.STANDARD,
            highLatitudeRule = HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT,
        ),
        hijriDayShift = -1,
        offsets = PrayerOffsets().with(Prayer.ISHA, -3),
        language = "bn",
    )

    @Test
    fun aPullKeepsTheDeviceCity() {
        assertEquals(local.cityId, local.withSynced(synced).cityId)
    }

    @Test
    fun aPullAdoptsTheAccountsSettingsShiftAndOffsets() {
        val merged = local.withSynced(synced)

        assertEquals(synced.calculationSettings, merged.settings)
        assertEquals(synced.hijriDayShift, merged.hijriDayShift)
        assertEquals(synced.offsets, merged.offsets)
    }

    @Test
    fun aPullAdoptsTheTotalButKeepsTheUsersLabelAndTarget() {
        val counter = TasbihCounter(label = "evening", target = 33, count = 10)

        assertEquals(TasbihCounter("evening", 33, 12), counter.withTotal(12))
    }

    @Test
    fun aTotalBelowZeroIsZero() {
        assertEquals(0, TasbihCounter("evening", 33, 10).withTotal(-4).count)
    }
}
