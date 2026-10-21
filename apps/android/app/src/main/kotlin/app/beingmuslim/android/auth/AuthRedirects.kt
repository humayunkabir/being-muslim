package app.beingmuslim.android.auth

import java.net.URI
import java.net.URLDecoder

/** What a deep link received by [AuthCallbackActivity] means. */
sealed interface AuthRedirect {

    /** The token flow finished; exchange [userId] and [secret] for a session. */
    data class Success(val userId: String, val secret: String, val state: String?) : AuthRedirect

    /** The provider or Appwrite reported a failure; there is no token to exchange. */
    data class Failure(val state: String?) : AuthRedirect

    /** Not one of this app's auth links. */
    data object Ignored : AuthRedirect
}

/**
 * Parses the token flow's callback without touching Android types, so the
 * success/failure boundary is covered by ordinary JVM tests. The success URL
 * carries the `state` this app put there; Appwrite adds `userId` and `secret`.
 */
object AuthRedirects {

    const val SCHEME = "appwrite-callback-being-muslim"
    const val HOST = "auth"

    fun parse(url: String): AuthRedirect {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return AuthRedirect.Ignored
        }

        if (!uri.scheme.equals(SCHEME, ignoreCase = true) ||
            !uri.host.equals(HOST, ignoreCase = true)
        ) {
            return AuthRedirect.Ignored
        }

        val parameters = parameters(uri.rawQuery)
        val state = parameters["appstate"]

        return when (uri.path) {
            "/success" -> {
                val userId = parameters["userId"]
                val secret = parameters["secret"]
                if (userId.isNullOrBlank() || secret.isNullOrBlank()) {
                    AuthRedirect.Failure(state)
                } else {
                    AuthRedirect.Success(userId, secret, state)
                }
            }

            "/failure" -> AuthRedirect.Failure(state)
            else -> AuthRedirect.Ignored
        }
    }

    private fun parameters(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrEmpty()) return emptyMap()
        return rawQuery.split('&').mapNotNull { pair ->
            val separator = pair.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val key = decode(pair.substring(0, separator)) ?: return@mapNotNull null
            val value = decode(pair.substring(separator + 1)) ?: return@mapNotNull null
            key to value
        }.toMap()
    }

    private fun decode(value: String): String? =
        try {
            URLDecoder.decode(value, "UTF-8")
        } catch (_: IllegalArgumentException) {
            null
        }
}
