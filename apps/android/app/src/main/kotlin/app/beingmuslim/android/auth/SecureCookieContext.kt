package app.beingmuslim.android.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * The Appwrite SDK constructs its own `SharedPreferencesCookieStore` from
 * `context.getSharedPreferences("myCookie", ...)` and offers no way to replace
 * it. This wrapper returns a Keystore-backed file for that one name, so the
 * session cookie is encrypted at rest instead of sitting in plaintext.
 *
 * The previous plaintext store is migrated once, then cleared. Both files are
 * excluded from platform backup (`res/xml/backup_rules.xml`,
 * `res/xml/data_extraction_rules.xml`).
 */
internal class SecureCookieContext(base: Context) : ContextWrapper(base) {

    private val securePreferences: SharedPreferences by lazy {
        try {
            createSecurePreferences()
        } catch (_: Exception) {
            // A lost or invalid Keystore key makes the old file unreadable.
            // Discard it and start clean; the user signs in again.
            baseContext.deleteSharedPreferences(SECURE_PREFERENCES)
            createSecurePreferences()
        }
    }

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
        if (name == LEGACY_PREFERENCES) securePreferences else super.getSharedPreferences(name, mode)

    // Jetpack Security is deprecated upstream, but the SDK only accepts a
    // SharedPreferences and has no cookie-store hook. This is the smallest
    // Keystore-backed adapter; replace it when the SDK exposes a store.
    @Suppress("DEPRECATION")
    private fun createSecurePreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(baseContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val preferences = EncryptedSharedPreferences.create(
            baseContext,
            SECURE_PREFERENCES,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        migrateLegacy(preferences)
        return preferences
    }

    /**
     * Copy the old plaintext cookies once, then leave no readable copy behind.
     * `commit` is deliberate here: `apply` could lose the migration or leave
     * plaintext behind if the process dies.
     */
    @SuppressLint("ApplySharedPref", "UseKtx")
    private fun migrateLegacy(preferences: SharedPreferences) {
        val legacy = baseContext.getSharedPreferences(LEGACY_PREFERENCES, Context.MODE_PRIVATE)
        val values = legacy.all
        if (values.isEmpty()) return
        preferences.edit().apply {
            values.forEach { (key, value) ->
                if (value is String) putString(key, value)
            }
        }.commit()
        legacy.edit().clear().commit()
    }

    private companion object {
        const val LEGACY_PREFERENCES = "myCookie"
        const val SECURE_PREFERENCES = "myCookie_secure"
    }
}
