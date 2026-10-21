package app.beingmuslim.android.auth

import app.beingmuslim.android.BuildConfig
import java.net.URLEncoder

/**
 * The public Appwrite coordinates for the token flow. The endpoint and project
 * ID are build configuration (`BuildConfig`), so a staging or production run
 * overrides them with Gradle properties; the OAuth client secret stays in
 * Appwrite.
 */
object AuthEndpoints {

    /**
     * The token endpoint the browser visits. It redirects to Google and, after
     * the callback, to our success/failure links. The success link carries the
     * state we verify before exchanging the token.
     */
    fun tokenUrl(state: String): String {
        val success = "${AuthRedirects.SCHEME}://${AuthRedirects.HOST}/success?appstate=$state"
        val failure = "${AuthRedirects.SCHEME}://${AuthRedirects.HOST}/failure?appstate=$state"
        return "${BuildConfig.APPWRITE_ENDPOINT}/account/tokens/oauth2/google" +
            "?project=${encode(BuildConfig.APPWRITE_PROJECT_ID)}" +
            "&success=${encode(success)}" +
            "&failure=${encode(failure)}"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
