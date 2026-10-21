package app.beingmuslim.android.appwrite

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.KeyStore

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
            // A lost or invalidated Keystore key makes both the old file and
            // the key unusable. Discard both so the retry makes a new key; the
            // user signs in again.
            baseContext.deleteSharedPreferences(SECURE_PREFERENCES)
            runCatching {
                KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                    .deleteEntry(MASTER_KEY_ALIAS)
            }
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
        val masterKey = MasterKey.Builder(baseContext, MASTER_KEY_ALIAS)
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
        val copied = preferences.edit().apply {
            values.forEach { (key, value) ->
                if (value is String) putString(key, value)
            }
        }.commit()
        if (!copied) throw IOException("Could not migrate the session cookie")
        val cleared = legacy.edit().clear().commit()
        if (!cleared) throw IOException("Could not clear the plaintext cookie")
    }

    private companion object {
        const val LEGACY_PREFERENCES = "myCookie"
        const val SECURE_PREFERENCES = "myCookie_secure"
        val MASTER_KEY_ALIAS: String = MasterKey.DEFAULT_MASTER_KEY_ALIAS
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}
