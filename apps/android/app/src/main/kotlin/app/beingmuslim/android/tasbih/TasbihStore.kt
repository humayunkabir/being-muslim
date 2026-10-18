package app.beingmuslim.android.tasbih

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persists the user's [TasbihCounter] across restarts.
 *
 * This is a thin platform adapter over [TasbihCodec], like
 * [app.beingmuslim.android.selection.SelectionStore]: it only reads and writes
 * strings, and disk access happens off the main thread. The label is the user's
 * own words and stays on the device (`docs/PRODUCT_REQUIREMENTS.md` §10).
 */
class TasbihStore(context: Context) {

    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** The stored counter, or an empty one when nothing has been counted yet. */
    suspend fun load(): TasbihCounter = withContext(Dispatchers.IO) {
        TasbihCodec.decode(TasbihCodec.KEYS.associateWith { preferences.getString(it, null) })
    }

    suspend fun save(counter: TasbihCounter) = withContext(Dispatchers.IO) {
        preferences.edit {
            TasbihCodec.encode(counter).forEach { (key, value) -> putString(key, value) }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "being_muslim_tasbih"
    }
}
