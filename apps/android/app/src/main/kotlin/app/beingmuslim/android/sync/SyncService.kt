package app.beingmuslim.android.sync

/** The settings and total stored for one account, as the backend holds them. */
data class StoredSync(val settings: String, val tasbihTotal: Int)

/**
 * The sync backend seam. It only moves the two fields the product allows; no
 * city, prayer time, or devotional timestamp crosses it
 * (`docs/PRODUCT_REQUIREMENTS.md` §10). Failures are ordinary: the caller treats
 * every exception as "the service is unreachable" and keeps working locally.
 */
interface SyncService {

    /** The stored row, or null when the account has no row or no settings yet. */
    suspend fun read(userId: String): StoredSync?

    /** Writes the row, creating it through the trusted server path if it is missing. */
    suspend fun write(userId: String, settings: String, tasbihTotal: Int)
}
