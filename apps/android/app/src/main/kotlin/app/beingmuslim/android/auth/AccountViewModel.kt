package app.beingmuslim.android.auth

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

private const val AUTH_CHECK_TIMEOUT_MS = 10_000L
private const val AUTH_EXCHANGE_TIMEOUT_MS = 10_000L

/**
 * Owns the account guard and the sign-in state machine. The screens never call
 * Appwrite directly: every check and every navigation decision waits for an
 * [AuthService] result, so the dashboard is never rendered from an unverified
 * session.
 *
 * The pending OAuth state lives in [SavedStateHandle], not only in memory: the
 * browser can push the app out of memory, and a valid callback must still be
 * recognised when the process is recreated.
 */
class AccountViewModel(
    private val auth: AuthService,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    private val _redirects = MutableStateFlow(0)
    val redirects: StateFlow<Int> = _redirects.asStateFlow()

    private val authMutex = Mutex()

    private var pendingState: String?
        get() = savedState.get<String>(PENDING_STATE)
        set(value) {
            savedState[PENDING_STATE] = value
        }

    /**
     * The guard both screens share: a live session opens the dashboard, a
     * signed-out user opens the login screen, and a failed check is treated as
     * signed out so the offline app is still reachable.
     *
     * A sign-in failure already on screen is not re-checked: otherwise the
     * screen that should explain it would immediately replace it with a plain
     * login screen.
     */
    fun resolve() = viewModelScope.launch {
        val current = _state.value
        if (current is AccountUiState.SigningIn) return@launch
        if (current is AccountUiState.SignedOut && current.error == AccountError.SIGN_IN_FAILED) {
            return@launch
        }
        authMutex.withLock {
            if (_state.value is AccountUiState.SigningIn) return@withLock
            _state.value = AccountUiState.Loading
            _state.value = try {
                val user = withTimeoutOrNull(AUTH_CHECK_TIMEOUT_MS) { auth.currentUser() }
                user?.let { AccountUiState.SignedIn(it) } ?: AccountUiState.SignedOut()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                AccountUiState.SignedOut(AccountError.CHECK_FAILED)
            }
        }
    }

    /**
     * Starts the token flow and returns the URL the browser should open. The
     * state is generated here and checked on the callback, so a link that this
     * screen did not start cannot create a session.
     */
    fun startSignIn(provider: AuthProvider): String? {
        if (_state.value !is AccountUiState.SignedOut) return null
        val state = UUID.randomUUID().toString()
        pendingState = state
        _state.value = AccountUiState.SigningIn()
        return AuthEndpoints.tokenUrl(provider, state)
    }

    /**
     * Handles the deep link from the browser. A callback whose state does not
     * match the one this screen created is ignored: it must not create a
     * session, cancel a sign-in, or move the screen. The token exchange is
     * suspended before the dashboard is shown.
     */
    fun handleRedirect(url: String) {
        when (val redirect = AuthRedirects.parse(url)) {
            AuthRedirect.Ignored -> return

            is AuthRedirect.Failure -> {
                if (redirect.state == null || redirect.state != pendingState) return
                pendingState = null
                _state.value = AccountUiState.SignedOut(AccountError.SIGN_IN_FAILED)
            }

            is AuthRedirect.Success -> {
                if (redirect.state == null || redirect.state != pendingState) return
                _state.value = AccountUiState.SigningIn(exchanging = true)
                viewModelScope.launch {
                    authMutex.withLock {
                        _state.value = try {
                            val user = withTimeoutOrNull(AUTH_EXCHANGE_TIMEOUT_MS) {
                                auth.createSession(redirect.userId, redirect.secret)
                            }
                            user?.let { AccountUiState.SignedIn(it) }
                                ?: AccountUiState.SignedOut(AccountError.SIGN_IN_FAILED)
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (_: Exception) {
                            AccountUiState.SignedOut(AccountError.SIGN_IN_FAILED)
                        }
                        pendingState = null
                    }
                }
            }
        }
        _redirects.value += 1
    }

    fun signOut() {
        viewModelScope.launch {
            authMutex.withLock {
                val current = _state.value as? AccountUiState.SignedIn ?: return@withLock
                _state.value = AccountUiState.SigningOut
                _state.value = try {
                    auth.signOut()
                    AccountUiState.SignedOut()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    AccountUiState.SignedIn(current.user, AccountError.SIGN_OUT_FAILED)
                }
            }
        }
    }

    /** The browser could not be opened; the login screen explains and retries. */
    fun onBrowserUnavailable() {
        _state.value = AccountUiState.SignedOut(AccountError.SIGN_IN_FAILED)
    }

    /**
     * The user gave up on the browser; the next attempt starts a fresh state.
     * Once the callback has arrived, the one-time token is already being
     * exchanged, so cancelling there would leave a session the screen denies.
     */
    fun cancelSignIn() {
        val current = _state.value as? AccountUiState.SigningIn ?: return
        if (current.exchanging) return
        pendingState = null
        _state.value = AccountUiState.SignedOut(AccountError.SIGN_IN_CANCELLED)
    }

    companion object {
        private const val PENDING_STATE = "pendingState"

        /** Builds the production service without leaking Appwrite into the UI. */
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AccountViewModel(
                    AppwriteAuthService(context.applicationContext),
                    createSavedStateHandle(),
                )
            }
        }
    }
}
