package app.beingmuslim.android.auth

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.net.URI
import java.net.URLDecoder

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun resolveShowsTheDashboardForAnExistingSession() = runTest(main.dispatcher) {
        val user = AuthUser("user-1", "Amina", "amina@example.com")
        val viewModel = accountViewModel(FakeAuthService(current = user))

        viewModel.resolve()
        advanceUntilIdle()

        assertEquals(AccountUiState.SignedIn(user), viewModel.state.value)
    }

    @Test
    fun resolveShowsLoginWhenThereIsNoSession() = runTest(main.dispatcher) {
        val viewModel = accountViewModel(FakeAuthService())

        viewModel.resolve()
        advanceUntilIdle()

        assertEquals(AccountUiState.SignedOut(), viewModel.state.value)
    }

    @Test
    fun resolveShowsLoginWithAnErrorWhenTheCheckFails() = runTest(main.dispatcher) {
        val viewModel = accountViewModel(FakeAuthService(checkError = IOException("offline")))

        viewModel.resolve()
        advanceUntilIdle()

        assertEquals(
            AccountUiState.SignedOut(AccountError.CHECK_FAILED),
            viewModel.state.value,
        )
    }

    @Test
    fun resolveFallsBackToLoginWhenTheCheckHangs() = runTest(main.dispatcher) {
        val viewModel = accountViewModel(
            FakeAuthService(
                current = AuthUser("user-1", "Amina", null),
                checkDelayMs = 30_000,
            ),
        )

        viewModel.resolve()
        advanceTimeBy(10_001)
        advanceUntilIdle()

        assertEquals(AccountUiState.SignedOut(), viewModel.state.value)
    }

    @Test
    fun startSignInWaitsAndReturnsTheTokenUrl() = runTest(main.dispatcher) {
        val viewModel = accountViewModel(FakeAuthService())
        viewModel.resolve()
        advanceUntilIdle()

        val url = viewModel.startSignIn(AuthProvider.GOOGLE)

        assertNotNull(url)
        assertEquals(AccountUiState.SigningIn(), viewModel.state.value)
        assertTrue(url!!.startsWith("https://appwrite.alveo.dev/v1/account/tokens/oauth2/google?"))
        assertTrue(url.contains("project=being-muslim"))
        assertTrue(url.contains("success="))
        assertTrue(url.contains("appstate%3D"))
    }

    @Test
    fun appleSignInUsesTheAppleProvider() = runTest(main.dispatcher) {
        val viewModel = accountViewModel(FakeAuthService())
        viewModel.resolve()
        advanceUntilIdle()

        val url = viewModel.startSignIn(AuthProvider.APPLE)

        assertNotNull(url)
        assertTrue(url!!.contains("/account/tokens/oauth2/apple?"))
        assertEquals(AccountUiState.SigningIn(), viewModel.state.value)
    }

    @Test
    fun cancellingSignInReturnsToLoginWithACancelledState() = runTest(main.dispatcher) {
        val viewModel = accountViewModel(FakeAuthService())
        viewModel.resolve()
        advanceUntilIdle()
        viewModel.startSignIn(AuthProvider.GOOGLE)

        viewModel.cancelSignIn()

        assertEquals(
            AccountUiState.SignedOut(AccountError.SIGN_IN_CANCELLED),
            viewModel.state.value,
        )
    }

    @Test
    fun cancellingAfterTheCallbackDoesNotDenyTheExchange() = runTest(main.dispatcher) {
        val user = AuthUser("user-1", "Amina", null)
        val auth = FakeAuthService(createDelayMs = 1_000)
        val viewModel = accountViewModel(auth)
        viewModel.resolve()
        advanceUntilIdle()
        val state = stateFrom(viewModel.startSignIn(AuthProvider.GOOGLE)!!)
        auth.current = user

        viewModel.handleRedirect(
            "appwrite-callback-being-muslim://auth/success" +
                "?userId=user-1&secret=secret-1&appstate=$state",
        )
        advanceTimeBy(100)
        viewModel.cancelSignIn()
        advanceUntilIdle()

        assertEquals(AccountUiState.SignedIn(user), viewModel.state.value)
    }

    @Test
    fun aBrowserThatCannotOpenReturnsToLoginWithAnError() = runTest(main.dispatcher) {
        val viewModel = accountViewModel(FakeAuthService())
        viewModel.resolve()
        advanceUntilIdle()
        viewModel.startSignIn(AuthProvider.GOOGLE)

        viewModel.onBrowserUnavailable()

        assertEquals(
            AccountUiState.SignedOut(AccountError.SIGN_IN_FAILED),
            viewModel.state.value,
        )
    }

    @Test
    fun aSuccessfulRedirectCreatesTheSessionAndShowsTheDashboard() = runTest(main.dispatcher) {
        val user = AuthUser("user-1", "Amina", null)
        val auth = FakeAuthService()
        val viewModel = accountViewModel(auth)
        viewModel.resolve()
        advanceUntilIdle()
        val state = stateFrom(viewModel.startSignIn(AuthProvider.GOOGLE)!!)
        auth.current = user

        viewModel.handleRedirect(
            "appwrite-callback-being-muslim://auth/success" +
                "?userId=user-1&secret=secret-1&appstate=$state",
        )
        advanceUntilIdle()

        assertEquals("user-1" to "secret-1", auth.createdWith)
        assertEquals(AccountUiState.SignedIn(user), viewModel.state.value)
        assertEquals(1, viewModel.redirects.value)
    }

    @Test
    fun aRedirectWithTheWrongStateDoesNotCreateASession() = runTest(main.dispatcher) {
        val auth = FakeAuthService()
        val viewModel = accountViewModel(auth)
        viewModel.resolve()
        advanceUntilIdle()
        viewModel.startSignIn(AuthProvider.GOOGLE)

        viewModel.handleRedirect(
            "appwrite-callback-being-muslim://auth/success" +
                "?userId=user-1&secret=secret-1&appstate=not-the-state",
        )
        advanceUntilIdle()

        assertEquals(null, auth.createdWith)
        assertEquals(AccountUiState.SigningIn(), viewModel.state.value)
        assertEquals(0, viewModel.redirects.value)
    }

    @Test
    fun aFailureWithTheWrongStateIsIgnored() = runTest(main.dispatcher) {
        val auth = FakeAuthService()
        val viewModel = accountViewModel(auth)
        viewModel.resolve()
        advanceUntilIdle()
        viewModel.startSignIn(AuthProvider.GOOGLE)

        viewModel.handleRedirect(
            "appwrite-callback-being-muslim://auth/failure?error=denied&appstate=not-the-state",
        )
        advanceUntilIdle()

        assertEquals(AccountUiState.SigningIn(), viewModel.state.value)
        assertEquals(0, viewModel.redirects.value)
    }

    @Test
    fun aCallbackSurvivesARecreatedViewModel() = runTest(main.dispatcher) {
        val user = AuthUser("user-1", "Amina", null)
        val auth = FakeAuthService()
        val savedState = SavedStateHandle()
        val first = AccountViewModel(auth, savedState)
        first.resolve()
        advanceUntilIdle()
        val state = stateFrom(first.startSignIn(AuthProvider.GOOGLE)!!)
        auth.current = user

        val recreated = AccountViewModel(auth, savedState)
        recreated.handleRedirect(
            "appwrite-callback-being-muslim://auth/success" +
                "?userId=user-1&secret=secret-1&appstate=$state",
        )
        advanceUntilIdle()

        assertEquals("user-1" to "secret-1", auth.createdWith)
        assertEquals(AccountUiState.SignedIn(user), recreated.state.value)
    }

    @Test
    fun aFailureRedirectReturnsToLoginWithAnError() = runTest(main.dispatcher) {
        val auth = FakeAuthService()
        val viewModel = accountViewModel(auth)
        viewModel.resolve()
        advanceUntilIdle()
        val state = stateFrom(viewModel.startSignIn(AuthProvider.GOOGLE)!!)

        viewModel.handleRedirect(
            "appwrite-callback-being-muslim://auth/failure?error=denied&appstate=$state",
        )
        advanceUntilIdle()

        assertEquals(null, auth.createdWith)
        assertEquals(
            AccountUiState.SignedOut(AccountError.SIGN_IN_FAILED),
            viewModel.state.value,
        )
    }

    @Test
    fun signOutReturnsToLogin() = runTest(main.dispatcher) {
        val user = AuthUser("user-1", "Amina", null)
        val auth = FakeAuthService(current = user)
        val viewModel = accountViewModel(auth)
        viewModel.resolve()
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        assertTrue(auth.signedOut)
        assertEquals(AccountUiState.SignedOut(), viewModel.state.value)
    }

    @Test
    fun aSecondSignOutDoesNotRestoreTheDashboard() = runTest(main.dispatcher) {
        val user = AuthUser("user-1", "Amina", null)
        val auth = FakeAuthService(current = user, failSignOutAfterFirst = true)
        val viewModel = accountViewModel(auth)
        viewModel.resolve()
        advanceUntilIdle()

        viewModel.signOut()
        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(1, auth.signOutCalls)
        assertEquals(AccountUiState.SignedOut(), viewModel.state.value)
    }

    @Test
    fun signOutFailureKeepsTheDashboardAndExplains() = runTest(main.dispatcher) {
        val user = AuthUser("user-1", "Amina", null)
        val auth = FakeAuthService(current = user, signOutError = IOException("offline"))
        val viewModel = accountViewModel(auth)
        viewModel.resolve()
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(
            AccountUiState.SignedIn(user, AccountError.SIGN_OUT_FAILED),
            viewModel.state.value,
        )
    }

    private fun accountViewModel(
        auth: AuthService,
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = AccountViewModel(auth, savedState)

    private fun stateFrom(tokenUrl: String): String {
        val query = URI(tokenUrl).rawQuery
        val success = query.split('&').first { it.startsWith("success=") }.substringAfter('=')
        val decoded = URLDecoder.decode(success, "UTF-8")
        return URI(decoded).rawQuery.substringAfter("appstate=")
    }
}

private class FakeAuthService(
    var current: AuthUser? = null,
    var checkError: Exception? = null,
    var checkDelayMs: Long = 0,
    var createDelayMs: Long = 0,
    var createError: Exception? = null,
    var signOutError: Exception? = null,
    var failSignOutAfterFirst: Boolean = false,
) : AuthService {

    var createdWith: Pair<String, String>? = null
    var signedOut = false
    var signOutCalls = 0

    override suspend fun currentUser(): AuthUser? {
        if (checkDelayMs > 0) delay(checkDelayMs)
        checkError?.let { throw it }
        return current
    }

    override suspend fun createSession(userId: String, secret: String): AuthUser {
        if (createDelayMs > 0) delay(createDelayMs)
        createError?.let { throw it }
        createdWith = userId to secret
        return current ?: AuthUser("created", null, null)
    }

    override suspend fun signOut() {
        signOutCalls += 1
        if (failSignOutAfterFirst && signOutCalls > 1) throw IOException("already signed out")
        signOutError?.let { throw it }
        signedOut = true
    }
}
