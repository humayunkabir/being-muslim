package app.beingmuslim.android.sync

import app.beingmuslim.android.prayer.AsrJuristicMethod
import app.beingmuslim.android.prayer.CalculationMethodChoice
import app.beingmuslim.android.prayer.CalculationSettings
import app.beingmuslim.android.prayer.HighLatitudeRuleChoice
import app.beingmuslim.android.prayer.PrayerOffsets
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The coordinator's bookkeeping: an unreachable service or a failed write must
 * leave the local state and its marker untouched, and a pull is handed back for
 * the caller to apply before it is recorded.
 */
class SyncCoordinatorTest {

    private val payload = SyncPayload(
        settings = SyncedSettings(
            calculationSettings = CalculationSettings(
                method = CalculationMethodChoice.KARACHI,
                asrJuristicMethod = AsrJuristicMethod.STANDARD,
                highLatitudeRule = HighLatitudeRuleChoice.MIDDLE_OF_THE_NIGHT,
            ),
            hijriDayShift = 0,
            offsets = PrayerOffsets(),
            language = "en",
        ),
        tasbihTotal = 40,
    )

    private val remote = payload.copy(tasbihTotal = 12)

    @Test
    fun anUnreachableServiceChangesNothing() = runTest {
        val state = FakeState()
        val coordinator = SyncCoordinator(FakeService(readError = IOException("offline")), state)

        assertEquals(SyncOutcome.Unchanged, coordinator.sync(USER, payload))

        assertNull(state.marked)
    }

    @Test
    fun theFirstSyncSeedsAnEmptyAccountAndRecordsIt() = runTest {
        val service = FakeService(stored = null)
        val state = FakeState()

        assertEquals(SyncOutcome.Unchanged, SyncCoordinator(service, state).sync(USER, payload))

        assertEquals(SyncCodec.encode(payload.settings), service.written?.settings)
        assertEquals(payload.tasbihTotal, service.written?.tasbihTotal)
        assertEquals(payload, state.marked)
    }

    @Test
    fun theFirstSyncReturnsTheAccountsPayloadWithoutRecordingIt() = runTest {
        val state = FakeState()
        val service = FakeService(StoredSync(SyncCodec.encode(remote.settings), remote.tasbihTotal))

        assertEquals(SyncOutcome.Apply(remote), SyncCoordinator(service, state).sync(USER, payload))

        // The caller records it only after applying it; a process death in
        // between leaves the first sync to run again.
        assertNull(state.marked)
    }

    @Test
    fun aFailedWriteIsNotRecorded() = runTest {
        val state = FakeState()
        val service = FakeService(readError = null, writeError = IOException("offline"))

        assertEquals(SyncOutcome.Unchanged, SyncCoordinator(service, state).sync(USER, payload))

        assertNull(state.marked)
    }

    @Test
    fun aLocalChangeStillWritesAfterTheFirstSync() = runTest {
        val state = FakeState(syncedUserIdValue = USER, last = remote)
        val service = FakeService(StoredSync(SyncCodec.encode(remote.settings), remote.tasbihTotal))

        assertEquals(SyncOutcome.Unchanged, SyncCoordinator(service, state).sync(USER, payload))

        assertEquals(SyncCodec.encode(payload.settings), service.written?.settings)
        assertEquals(payload, state.marked)
    }

    private class FakeService(
        var stored: StoredSync? = null,
        private val readError: Exception? = null,
        private val writeError: Exception? = null,
    ) : SyncService {

        var written: StoredSync? = null

        override suspend fun read(userId: String): StoredSync? {
            readError?.let { throw it }
            return stored
        }

        override suspend fun write(userId: String, settings: String, tasbihTotal: Int) {
            writeError?.let { throw it }
            written = StoredSync(settings, tasbihTotal)
            stored = written
        }
    }

    private class FakeState(
        var syncedUserIdValue: String? = null,
        private var last: SyncPayload? = null,
    ) : SyncState {

        var marked: SyncPayload? = null

        override fun syncedUserId(): String? = syncedUserIdValue

        override fun lastSyncedPayload(userId: String): SyncPayload? =
            if (syncedUserIdValue == userId) last else null

        override fun markSynced(userId: String, payload: SyncPayload) {
            syncedUserIdValue = userId
            last = payload
            marked = payload
        }
    }

    private companion object {
        const val USER = "user-1"
    }
}
