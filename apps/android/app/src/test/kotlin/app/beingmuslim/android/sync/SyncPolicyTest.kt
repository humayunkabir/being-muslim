package app.beingmuslim.android.sync

import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import app.beingmuslim.android.prayer.PrayerOffsets
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The documented conflict rule (`docs/SYNC.md`): the first sync adopts the
 * server's payload; after that, unsynced local changes win and are uploaded,
 * while an unchanged device adopts a different server payload. A newer payload
 * is never overwritten, and an unreachable service changes nothing.
 */
class SyncPolicyTest {

    private val local = payload(total = 40, method = CalculationMethodChoice.KARACHI)
    private val other = payload(total = 12, method = CalculationMethodChoice.UMM_AL_QURA)

    @Test
    fun theFirstSyncAdoptsWhatTheAccountAlreadyHas() {
        assertEquals(
            SyncDecision.Pull(other),
            SyncPolicy.decide(
                firstSync = true,
                local = local,
                remote = RemoteState.Present(other),
                lastSynced = null,
            ),
        )
    }

    @Test
    fun theFirstSyncSeedsAnEmptyAccount() {
        assertEquals(
            SyncDecision.Push,
            SyncPolicy.decide(true, local, RemoteState.Empty, lastSynced = null),
        )
    }

    @Test
    fun theFirstSyncLeavesAnUnreachableAccountAlone() {
        assertEquals(
            SyncDecision.None,
            SyncPolicy.decide(true, local, RemoteState.Unavailable, lastSynced = null),
        )
    }

    @Test
    fun aNewerPayloadIsNeverOverwritten() {
        assertEquals(
            SyncDecision.None,
            SyncPolicy.decide(true, local, RemoteState.Unsupported, lastSynced = null),
        )
        assertEquals(
            SyncDecision.None,
            SyncPolicy.decide(false, local, RemoteState.Unsupported, lastSynced = other),
        )
    }

    @Test
    fun anUnsyncedLocalChangeIsUploaded() {
        assertEquals(
            SyncDecision.Push,
            SyncPolicy.decide(false, local, RemoteState.Present(other), lastSynced = other),
        )
    }

    @Test
    fun anUnsyncedLocalChangeStillTriesWhileTheServiceIsUnreachable() {
        // The write may be the first thing to succeed when connectivity returns;
        // failing it leaves the local state and marker untouched.
        assertEquals(
            SyncDecision.Push,
            SyncPolicy.decide(false, local, RemoteState.Unavailable, lastSynced = other),
        )
    }

    @Test
    fun anUnchangedDeviceAdoptsAnotherDevicesChange() {
        assertEquals(
            SyncDecision.Pull(local),
            SyncPolicy.decide(false, other, RemoteState.Present(local), lastSynced = other),
        )
    }

    @Test
    fun anUnchangedDeviceWithTheSamePayloadDoesNothing() {
        assertEquals(
            SyncDecision.None,
            SyncPolicy.decide(false, local, RemoteState.Present(local), lastSynced = local),
        )
    }

    @Test
    fun anUnchangedDeviceRestoresAnEmptyAccount() {
        assertEquals(
            SyncDecision.Push,
            SyncPolicy.decide(false, local, RemoteState.Empty, lastSynced = local),
        )
    }

    @Test
    fun anUnchangedDeviceDoesNothingWhileTheServiceIsUnreachable() {
        assertEquals(
            SyncDecision.None,
            SyncPolicy.decide(false, local, RemoteState.Unavailable, lastSynced = local),
        )
    }

    private fun payload(total: Int, method: CalculationMethodChoice) = SyncPayload(
        settings = SyncedSettings(
            calculationSettings = CalculationSettings(
                method = method,
                asrJuristicMethod = AsrJuristicMethod.STANDARD,
                highLatitudeRule = HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT,
            ),
            hijriDayShift = 0,
            offsets = PrayerOffsets(),
            language = "en",
        ),
        tasbihTotal = total,
    )
}
