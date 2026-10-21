package app.beingmuslim.android.sync

import android.content.Context
import app.beingmuslim.android.BuildConfig
import app.beingmuslim.android.appwrite.appwriteClient
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Functions
import io.appwrite.services.TablesDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Appwrite behind [SyncService]. The client is shared with auth through
 * [appwriteClient]; the session cookie lets the row-level permissions decide
 * what this user may read and update.
 *
 * Row creation is not a client permission: [write] asks the `user-sync`
 * function to create the row through the trusted server path, then retries.
 */
class AppwriteSyncService(context: Context) : SyncService {

    private val appContext = context.applicationContext

    // Built lazily inside the IO calls: constructing the client reads and
    // decrypts the Keystore-backed cookie store.
    private val client by lazy { appwriteClient(appContext) }
    private val tables by lazy { TablesDB(client) }
    private val functions by lazy { Functions(client) }

    override suspend fun read(userId: String): StoredSync? = withContext(Dispatchers.IO) {
        try {
            val row = tables.getRow(DATABASE_ID, TABLE_ID, userId)
            val settings = row.data[SETTINGS] as? String ?: return@withContext null
            val total = (row.data[TASBIH_TOTAL] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
            StoredSync(settings, total)
        } catch (exception: AppwriteException) {
            if (exception.code == 404) null else throw exception
        }
    }

    override suspend fun write(userId: String, settings: String, tasbihTotal: Int) {
        withContext(Dispatchers.IO) {
            val data = mapOf(SETTINGS to settings, TASBIH_TOTAL to tasbihTotal)
            try {
                tables.updateRow(DATABASE_ID, TABLE_ID, userId, data)
            } catch (exception: AppwriteException) {
                if (exception.code != 404) throw exception
                functions.createExecution(BuildConfig.APPWRITE_SYNC_FUNCTION_ID, async = false)
                tables.updateRow(DATABASE_ID, TABLE_ID, userId, data)
            }
        }
    }

    private companion object {
        const val DATABASE_ID = "being_muslim"
        const val TABLE_ID = "user_sync"
        const val SETTINGS = "settings"
        const val TASBIH_TOTAL = "tasbihTotal"
    }
}
