package app.beingmuslim.android.sync

/** What the service found for this account, without turning a failure into a decision. */
sealed interface RemoteState {

    /** No row, or a row whose settings have never been written. */
    data object Empty : RemoteState

    /** The service could not be read: offline, unreachable, or not signed in. */
    data object Unavailable : RemoteState

    /** A payload written by a newer app version this build must not overwrite. */
    data object Unsupported : RemoteState

    data class Present(val payload: SyncPayload) : RemoteState
}

/** What one sync attempt should do, with the payload to apply when it should pull. */
sealed interface SyncDecision {

    data object Push : SyncDecision

    data class Pull(val payload: SyncPayload) : SyncDecision

    data object None : SyncDecision
}

/**
 * The conflict rule, as pure data in and a decision out (`docs/SYNC.md`):
 *
 * - The first sync with an account adopts the server's payload, so a new device
 *   starts with the settings and total the user already has. If the server has
 *   nothing, the device seeds it with its local payload.
 * - Afterwards, a local change not yet uploaded wins and is pushed. With no
 *   local change, a different server payload is pulled.
 * - A server payload is never applied or overwritten when it is unavailable or
 *   written in a newer format: the local state simply stays as it is. A local
 *   change still attempts an upload when the service was merely unavailable.
 */
object SyncPolicy {

    fun decide(
        firstSync: Boolean,
        local: SyncPayload,
        remote: RemoteState,
        lastSynced: SyncPayload?,
    ): SyncDecision = when (remote) {
        RemoteState.Unsupported -> SyncDecision.None
        RemoteState.Unavailable ->
            if (!firstSync && local != lastSynced) SyncDecision.Push else SyncDecision.None
        RemoteState.Empty -> SyncDecision.Push
        is RemoteState.Present -> when {
            firstSync -> SyncDecision.Pull(remote.payload)
            local != lastSynced -> SyncDecision.Push
            remote.payload != lastSynced -> SyncDecision.Pull(remote.payload)
            else -> SyncDecision.None
        }
    }
}
