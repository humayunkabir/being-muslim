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
        val row = tables.readSyncRow(userId) ?: return@withContext null
        val settings = row.settings ?: return@withContext null
        StoredSync(settings, row.tasbihTotal)
    }

    override suspend fun write(userId: String, settings: String, tasbihTotal: Int) {
        withContext(Dispatchers.IO) {
            val data = mapOf(SYNC_SETTINGS to settings, SYNC_TASBIH_TOTAL to tasbihTotal)
            try {
                tables.updateRow(SYNC_DATABASE_ID, SYNC_TABLE_ID, userId, data)
            } catch (exception: AppwriteException) {
                if (exception.code != 404) throw exception
                functions.createExecution(BuildConfig.APPWRITE_SYNC_FUNCTION_ID, async = false)
                tables.updateRow(SYNC_DATABASE_ID, SYNC_TABLE_ID, userId, data)
            }
        }
    }
}
