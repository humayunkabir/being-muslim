package app.beingmuslim.android.auth

/** What the account screen is showing, after auth has been resolved. */
sealed interface AccountUiState {

    /** Checking the stored session before either screen may render. */
    data object Loading : AccountUiState

    /** No usable session; the login screen is shown. */
    data class SignedOut(val error: AccountError? = null) : AccountUiState

    /**
     * The browser is open, waiting for the chosen provider, or the returned
     * token is being exchanged ([exchanging]).
     */
    data class SigningIn(val exchanging: Boolean = false) : AccountUiState

    /** A verified session; the dashboard is shown. */
    data class SignedIn(
        val user: AuthUser,
        val error: AccountError? = null,
    ) : AccountUiState

    /** The sign-out call is in flight. */
    data object SigningOut : AccountUiState
}

/** A failure the account screen can explain without carrying a server message. */
enum class AccountError {
    CHECK_FAILED,
    SIGN_IN_FAILED,
    SIGN_IN_CANCELLED,
    SIGN_OUT_FAILED,
}
