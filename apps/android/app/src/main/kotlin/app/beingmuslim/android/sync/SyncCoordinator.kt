package app.beingmuslim.android.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The result of one sync attempt: the server's payload to apply, or nothing to change. */
sealed interface SyncOutcome {

    data object Unchanged : SyncOutcome

    data class Apply(val payload: SyncPayload) : SyncOutcome
}

/**
 * Runs one sync attempt at a time, decides with [SyncPolicy], and never lets a
 * backend failure reach the caller. A pull is returned to the caller, which
 * applies it and records the resulting local payload with
 * [SyncState.markSynced]; a push records it here, once the write succeeded.
 *
 * Serialising attempts keeps a slow launch pull from overwriting a change the
 * user made while it was in flight; the caller also re-checks that its local
 * snapshot is still current before applying.
 */
class SyncCoordinator(
    private val service: SyncService,
    private val state: SyncState,
) {

    private val mutex = Mutex()

    suspend fun sync(userId: String, local: SyncPayload): SyncOutcome = mutex.withLock {
        val firstSync = state.syncedUserId() != userId
        val lastSynced = if (firstSync) null else state.lastSyncedPayload(userId)
        val remote = quietly { readRemote(userId) } ?: RemoteState.Unavailable

        when (SyncPolicy.decide(firstSync, local, remote, lastSynced)) {
            SyncAction.PUSH -> {
                write(userId, local)
                SyncOutcome.Unchanged
            }

            SyncAction.PULL -> when (remote) {
                is RemoteState.Present -> SyncOutcome.Apply(remote.payload)
                // The policy only asks for a pull when a payload is present.
                else -> SyncOutcome.Unchanged
            }

            SyncAction.NONE -> SyncOutcome.Unchanged
        }
    }

    private suspend fun readRemote(userId: String): RemoteState {
        val stored = service.read(userId) ?: return RemoteState.Empty
        val settings = SyncCodec.decode(stored.settings) ?: return RemoteState.Unsupported
        return RemoteState.Present(SyncPayload(settings, stored.tasbihTotal))
    }

    private suspend fun write(userId: String, local: SyncPayload) {
        quietly {
            service.write(userId, SyncCodec.encode(local.settings), local.tasbihTotal)
            state.markSynced(userId, local)
        }
        // Null means unreachable: the local store stays authoritative, the
        // payload is still unsynced, and the next attempt tries again.
    }

    /**
     * Runs [block], turning an ordinary backend failure into null. Cancellation
     * is not a backend failure and must keep unwinding.
     */
    private suspend fun <T> quietly(block: suspend () -> T): T? =
        try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
}
