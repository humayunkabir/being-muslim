package app.beingmuslim.android.account

import app.beingmuslim.android.auth.AuthUser
import app.beingmuslim.android.sync.StoredRow
import org.json.JSONException
import org.json.JSONObject

/**
 * The user's copy of what the account holds, as one JSON document
 * (`docs/SYNC.md`, `docs/PRIVACY.md`). It is read-only and contains no device
 * data: the city, prayer times, reminders, and tasbih label/target never leave
 * the device, so they are not part of an account export.
 */
object AccountExport {

    const val FORMAT = "being-muslim-account"
    const val VERSION = 1
    const val FILE_NAME = "being-muslim-account.json"

    fun document(user: AuthUser, stored: StoredRow?): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put(
            "account",
            JSONObject().apply {
                put("id", user.id)
                put("name", user.name ?: JSONObject.NULL)
                put("email", user.email ?: JSONObject.NULL)
            },
        )
        put(
            "stored",
            stored?.let {
                JSONObject().apply {
                    put("settings", readableSettings(it.settings))
                    put("tasbihTotal", it.tasbihTotal)
                }
            } ?: JSONObject.NULL,
        )
    }.toString(2)

    /** Nest the saved object, not its escaped string; keep unreadable data rather than lose it. */
    private fun readableSettings(settings: String?): Any = when {
        settings == null -> JSONObject.NULL
        else -> try {
            JSONObject(settings)
        } catch (_: JSONException) {
            settings
        }
    }
}
