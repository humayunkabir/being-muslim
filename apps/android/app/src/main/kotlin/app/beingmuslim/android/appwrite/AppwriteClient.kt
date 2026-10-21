package app.beingmuslim.android.appwrite

import android.content.Context
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
