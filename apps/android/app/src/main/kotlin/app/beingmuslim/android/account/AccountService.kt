package app.beingmuslim.android.account

import app.beingmuslim.android.auth.AuthUser

/**
 * The account's data operations, hidden behind a seam so deletion and export can
 * be tested without a network. Neither touches the device's city, prayer,
 * reminder, or tasbih data.
 */
interface AccountService {

    /** The user's stored data as the export document (`docs/SYNC.md`). Never writes anything. */
    suspend fun export(user: AuthUser): String

    /** Deletes the synced row and the identity. Local data and features are untouched. */
    suspend fun delete()
}
