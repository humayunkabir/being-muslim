package app.beingmuslim.android.appwrite

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import app.beingmuslim.android.BuildConfig
import io.appwrite.Client

/**
 * The one Appwrite client configuration auth and sync share: the endpoint and
 * project ID are public, and [SecureCookieContext] keeps the session cookie in
 * Keystore-backed storage. [context] must be an application context.
 */
internal fun appwriteClient(context: Context): Client =
    Client(SecureCookieContext(context))
        .setEndpoint(BuildConfig.APPWRITE_ENDPOINT)
        .setProject(BuildConfig.APPWRITE_PROJECT_ID)

/**
 * A client that carries no session, for a problem report. Reporting needs no
 * account, and sending through the signed-in client would let the function's
 * execution record tie the report to that account (`docs/PRIVACY.md`).
 */
internal fun appwriteGuestClient(context: Context): Client =
    Client(GuestCookieContext(context))
        .setEndpoint(BuildConfig.APPWRITE_ENDPOINT)
        .setProject(BuildConfig.APPWRITE_PROJECT_ID)

/**
 * The SDK always reads its cookie store through this context. Pointing it at a
 * file no sign-in ever writes means no session cookie can be sent; it is
 * plaintext, but always empty.
 */
private class GuestCookieContext(base: Context) : ContextWrapper(base) {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
        baseContext.getSharedPreferences(GUEST_COOKIES, mode)
}

private const val GUEST_COOKIES = "report-guest-cookies"

