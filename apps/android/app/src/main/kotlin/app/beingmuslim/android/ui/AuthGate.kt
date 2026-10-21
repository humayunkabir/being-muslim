package app.beingmuslim.android.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beingmuslim.android.R
import app.beingmuslim.android.auth.AccountUiState
import app.beingmuslim.android.auth.AccountViewModel

/**
 * The account gate and the app behind it. The app opens on the account screen:
 * a signed-in user sees the dashboard, a signed-out user sees the login screen
 * and can continue offline. Every core feature stays reachable without an
 * account (`docs/PRODUCT_REQUIREMENTS.md` §1, §10).
 *
 * A deep link can arrive while the app is on screen; it opens the account screen
 * so [AccountViewModel] can finish the exchange and the user sees the result.
 */
@Composable
fun AuthGate(accountViewModel: AccountViewModel) {
    val state by accountViewModel.state.collectAsStateWithLifecycle()
    val redirects by accountViewModel.redirects.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showApp by rememberSaveable { mutableStateOf(false) }
    var showAccount by rememberSaveable { mutableStateOf(false) }
    var lastRedirect by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(redirects) {
        if (redirects > lastRedirect) {
            lastRedirect = redirects
            showApp = false
            showAccount = true
        }
    }

    if (showApp && !showAccount) {
        AppRoot(onOpenAccount = { showAccount = true })
    } else {
        LaunchedEffect(Unit) { accountViewModel.resolve() }
        AccountScreen(
            state = state,
            onSignIn = { provider ->
                accountViewModel.startSignIn(provider)?.let { url ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    }.onFailure {
                        accountViewModel.onBrowserUnavailable()
                        Toast.makeText(
                            context,
                            R.string.account_error_sign_in,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            onSignOut = accountViewModel::signOut,
            onCancel = accountViewModel::cancelSignIn,
            onExit = {
                accountViewModel.cancelSignIn()
                if (showApp) {
                    showAccount = false
                } else {
                    showApp = true
                    showAccount = false
                }
            },
            exitLabel = when {
                !showApp && state is AccountUiState.SignedIn -> R.string.account_open_app
                !showApp -> R.string.account_continue_offline
                else -> R.string.account_back
            },
            onBack = if (showApp) {
                {
                    accountViewModel.cancelSignIn()
                    showAccount = false
                }
            } else {
                null
            },
        )
    }
}
