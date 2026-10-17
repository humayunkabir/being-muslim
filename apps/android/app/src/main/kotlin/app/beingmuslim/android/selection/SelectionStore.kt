package app.beingmuslim.android.selection

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persists the user's [UserSelection] across restarts.
 *
 * This is a thin platform adapter over [UserSelectionCodec]: it only reads and
 * writes strings. Disk access happens off the main thread, and the value stored
 * is the raw choice — the app derives nothing from it that could not be
 * recomputed (`docs/PRODUCT_REQUIREMENTS.md` §10).
 */
class SelectionStore(context: Context) {

    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** The stored selection, or null when the user has not chosen yet. */
    suspend fun load(): UserSelection? = withContext(Dispatchers.IO) {
        UserSelectionCodec.decode(UserSelectionCodec.KEYS.associateWith { preferences.getString(it, null) })
    }

    suspend fun save(selection: UserSelection) = withContext(Dispatchers.IO) {
        preferences.edit {
            UserSelectionCodec.encode(selection).forEach { (key, value) -> putString(key, value) }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "being_muslim_selection"
    }
}
