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

enum class SyncAction { PUSH, PULL, NONE }

/**
 * The conflict rule, as pure data in and a decision out (`docs/SYNC.md`):
 *
 * - The first sync with an account adopts the server's payload, so a new device
 *   starts with the settings and total the user already has. If the server has
 *   nothing, the device seeds it with its local payload.
 * - Afterwards, a local change not yet uploaded wins and is pushed. With no
 *   local change, a different server payload is pulled.
 * - A server payload is never overwritten when it is unavailable or written in
 *   a newer format: the local state simply stays as it is.
 */
object SyncPolicy {

    fun decide(
        firstSync: Boolean,
        local: SyncPayload,
        remote: RemoteState,
        lastSynced: SyncPayload?,
    ): SyncAction = when (remote) {
        RemoteState.Unsupported -> SyncAction.NONE
        RemoteState.Unavailable ->
            if (!firstSync && local != lastSynced) SyncAction.PUSH else SyncAction.NONE
        RemoteState.Empty -> SyncAction.PUSH
        is RemoteState.Present -> when {
            firstSync -> SyncAction.PULL
            local != lastSynced -> SyncAction.PUSH
            remote.payload != lastSynced -> SyncAction.PULL
            else -> SyncAction.NONE
        }
    }
}
