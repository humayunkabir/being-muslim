package app.beingmuslim.android.auth

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The deep link returned by the OAuth2 token flow. Appwrite appends the token's
 * `userId` and `secret`, and the success URL's own `state` survives the round
 * trip; the parser is the boundary between that URL and the session exchange.
 */
class AuthRedirectsTest {

    @Test
    fun parsesASuccessRedirect() {
        val redirect = AuthRedirects.parse(
            "appwrite-callback-being-muslim://auth/success" +
                "?userId=user-1&secret=token%2Bvalue&appstate=state-1",
        )

        assertEquals(AuthRedirect.Success("user-1", "token+value", "state-1"), redirect)
    }

    @Test
    fun aSuccessWithoutAUserIdOrSecretIsAFailure() {
        assertEquals(
            AuthRedirect.Failure("state-1"),
            AuthRedirects.parse("appwrite-callback-being-muslim://auth/success?appstate=state-1"),
        )
        assertEquals(
            AuthRedirect.Failure(null),
            AuthRedirects.parse("appwrite-callback-being-muslim://auth/success?userId=user-1"),
        )
    }

    @Test
    fun parsesAFailureRedirect() {
        assertEquals(
            AuthRedirect.Failure("state-2"),
            AuthRedirects.parse("appwrite-callback-being-muslim://auth/failure?error=denied&appstate=state-2"),
        )
    }

    @Test
    fun ignoresAnotherSchemeHostOrPath() {
        assertEquals(
            AuthRedirect.Ignored,
            AuthRedirects.parse("https://auth/success?userId=user-1&secret=secret"),
        )
        assertEquals(
            AuthRedirect.Ignored,
            AuthRedirects.parse("appwrite-callback-being-muslim://other/success?userId=user-1&secret=secret"),
        )
        assertEquals(
            AuthRedirect.Ignored,
            AuthRedirects.parse("appwrite-callback-being-muslim://auth/other?userId=user-1&secret=secret"),
        )
    }

    @Test
    fun ignoresAMalformedUri() {
        assertEquals(AuthRedirect.Ignored, AuthRedirects.parse("not a uri"))
    }
}
