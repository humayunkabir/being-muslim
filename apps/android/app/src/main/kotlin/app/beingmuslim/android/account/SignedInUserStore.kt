package app.beingmuslim.android.account

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The account this device last verified, kept so sync can decide whether to
 * attempt a push without a network round-trip first. The Appwrite session
 * remains the source of truth; a stale id only causes a failed attempt that is
 * discarded, and a fresh sign-in or account check replaces it.
 *
 * This is not a credential and holds no devotional content.
 */
class SignedInUserStore(context: Context) {

    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun signedInUserId(): String? = preferences.getString(SIGNED_IN_USER, null)

    fun setSignedInUserId(userId: String?) {
        preferences.edit {
            if (userId == null) remove(SIGNED_IN_USER) else putString(SIGNED_IN_USER, userId)
        }
    }

    /** Loads the file off the main thread so later reads are in-memory. */
    suspend fun warmUp() = withContext(Dispatchers.IO) {
        preferences.all
    }

    private companion object {
        const val PREFERENCES_NAME = "being_muslim_account"
        const val SIGNED_IN_USER = "signed_in_user"
    }
}
