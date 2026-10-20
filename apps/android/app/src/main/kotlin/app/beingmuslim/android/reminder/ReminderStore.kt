package app.beingmuslim.android.reminder

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The user's reminder preferences, kept beside the selection but in their own
 * file: reminding is a separate choice from how times are calculated, and a
 * corrupt preference loses only itself (issue #12).
 */
class ReminderStore(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun load(): ReminderPreferences = withContext(Dispatchers.IO) {
        ReminderPreferencesCodec.decode(
            ReminderPreferencesCodec.KEYS.associateWith { preferences.getString(it, null) },
        )
    }

    suspend fun save(value: ReminderPreferences) = withContext(Dispatchers.IO) {
        val encoded = ReminderPreferencesCodec.encode(value)
        preferences.edit {
            encoded.forEach { (key, stored) -> putString(key, stored) }
            // An enabled prayer is written as "true"; clearing it must remove the
            // key so the absence keeps meaning off.
            ReminderPreferencesCodec.PRAYER_KEYS.values
                .filterNot { it in encoded }
                .forEach { remove(it) }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "being_muslim_reminders"
    }
}
