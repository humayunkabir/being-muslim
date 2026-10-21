package app.beingmuslim.android.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * The payload this device last exchanged with its account, stored beside the
 * local data. It is what makes "changed on this device" answerable without a
 * transmitted timestamp (`docs/SYNC.md`). The signed-in account itself lives in
 * [app.beingmuslim.android.account.SignedInUserStore], so this store changes
 * only when sync does.
 *
 * None of these values is a credential.
 */
class SyncStateStore(context: Context) : SyncState {

    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

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

    /** Loads the file off the main thread so the sync trigger's read is in-memory. */
    suspend fun warmUp() = withContext(Dispatchers.IO) {
        preferences.all
    }

    private companion object {
        const val PREFERENCES_NAME = "being_muslim_sync"
        const val SYNCED_USER = "synced_user"
        const val LAST_SETTINGS = "last_settings"
        const val LAST_TOTAL = "last_total"
    }
}
