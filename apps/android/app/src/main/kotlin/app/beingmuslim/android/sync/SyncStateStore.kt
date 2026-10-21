package app.beingmuslim.android.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * The sync bookkeeping the coordinator needs: which account was last synced,
 * and what was exchanged with it. Kept behind an interface so the conflict
 * decisions and failure handling can be tested without Android storage.
 */
interface SyncState {

    fun syncedUserId(): String?

    fun lastSyncedPayload(userId: String): SyncPayload?

    fun markSynced(userId: String, payload: SyncPayload)
}

/**
 * What this device knows about its sync account, stored beside the local data:
 * which account is signed in, and the payload last exchanged with the server.
 * The last-synced payload is what makes "changed on this device" answerable
 * without a transmitted timestamp (`docs/SYNC.md`).
 *
 * None of these values is a credential or devotional content.
 */
class SyncStateStore(context: Context) : SyncState {

    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * The signed-in account, remembered after a successful check or sign-in so
     * local changes can attempt a push without a network round-trip first.
     * Cleared on sign-out and on a positive "no session" answer.
     */
    fun signedInUserId(): String? = preferences.getString(SIGNED_IN_USER, null)

    fun setSignedInUserId(userId: String?) {
        preferences.edit {
            if (userId == null) remove(SIGNED_IN_USER) else putString(SIGNED_IN_USER, userId)
        }
    }

    /** The account whose payload this device last exchanged, or null before its first sync. */
    override fun syncedUserId(): String? = preferences.getString(SYNCED_USER, null)

    /** The payload last sent to or received from [userId]; null when this device has not synced it yet. */
    override fun lastSyncedPayload(userId: String): SyncPayload? {
        if (syncedUserId() != userId) return null
        val settings = SyncCodec.decode(preferences.getString(LAST_SETTINGS, null)) ?: return null
        return SyncPayload(settings, preferences.getInt(LAST_TOTAL, 0))
    }

    override fun markSynced(userId: String, payload: SyncPayload) {
        preferences.edit {
            putString(SYNCED_USER, userId)
            putString(LAST_SETTINGS, SyncCodec.encode(payload.settings))
            putInt(LAST_TOTAL, payload.tasbihTotal)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "being_muslim_sync"
        const val SIGNED_IN_USER = "signed_in_user"
        const val SYNCED_USER = "synced_user"
        const val LAST_SETTINGS = "last_settings"
        const val LAST_TOTAL = "last_total"
    }
}
