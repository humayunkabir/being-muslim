package app.beingmuslim.android.sync

import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.TablesDB

/**
 * One row as the backend holds it, before sync decides whether this build can
 * apply it. [settings] is null when the row exists but nothing was saved;
 * export still includes [tasbihTotal] in that case.
 */
data class StoredRow(val settings: String?, val tasbihTotal: Int)

/** Reads the account's row once for both sync and export; 404 means no row. */
internal suspend fun TablesDB.readSyncRow(userId: String): StoredRow? = try {
    storedRow(getRow(SYNC_DATABASE_ID, SYNC_TABLE_ID, userId).data)
} catch (exception: AppwriteException) {
    if (exception.code == 404) null else throw exception
}

/**
 * Maps the row's columns. A missing or malformed field falls back to what the
 * table means: no settings and a non-negative total (`docs/APPWRITE.md`).
 */
internal fun storedRow(data: Map<String, Any>?): StoredRow? = data?.let {
    StoredRow(
        settings = it[SYNC_SETTINGS] as? String,
        tasbihTotal = (it[SYNC_TASBIH_TOTAL] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
    )
}

internal const val SYNC_DATABASE_ID = "being_muslim"
internal const val SYNC_TABLE_ID = "user_sync"
internal const val SYNC_SETTINGS = "settings"
internal const val SYNC_TASBIH_TOTAL = "tasbihTotal"
