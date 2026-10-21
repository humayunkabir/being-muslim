package app.beingmuslim.android.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.beingmuslim.android.R
import app.beingmuslim.android.auth.AccountError
import app.beingmuslim.android.auth.AccountUiState

/**
 * The signed-out login screen and the signed-in dashboard, plus their loading
 * and failure states (`docs/PRODUCT_REQUIREMENTS.md` §10). The screen is
 * stateless: every action is reported to the caller, which is the only place
 * that talks to Appwrite.
 *
 * The Google mark comes from the supplied SVG. It is decorative because the
 * button already names the provider.
 */
@Composable
fun AccountScreen(
    state: AccountUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onExit: () -> Unit,
    @StringRes exitLabel: Int,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = onBack != null) { onBack?.invoke() }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(state.titleRes()),
                style = MaterialTheme.typography.headlineSmall,
            )

            when (state) {
                AccountUiState.Loading -> {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.account_checking),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onExit) {
                        Text(stringResource(exitLabel))
                    }
                }

                is AccountUiState.SignedOut -> {
                    Text(
                        text = stringResource(R.string.account_intro),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    state.error?.let { ErrorText(it) }
                    Button(
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_google),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.account_sign_in_google))
                    }
                    TextButton(onClick = onExit) {
                        Text(stringResource(exitLabel))
                    }
                }

                AccountUiState.SigningIn -> {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.account_signing_in),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onExit) {
                        Text(stringResource(exitLabel))
                    }
                }

                is AccountUiState.SignedIn -> {
                    Text(
                        text = stringResource(R.string.account_signed_in_as, state.user.displayName),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val name = state.user.name
                    val email = state.user.email
                    if (!name.isNullOrBlank() && !email.isNullOrBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.error?.let { ErrorText(it) }
                    Button(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(exitLabel))
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.account_sign_out))
                    }
                }

                AccountUiState.SigningOut -> {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.account_signing_out),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorText(error: AccountError) {
    Text(
        text = stringResource(
            when (error) {
                AccountError.CHECK_FAILED -> R.string.account_error_check
                AccountError.SIGN_IN_FAILED -> R.string.account_error_sign_in
                AccountError.SIGN_OUT_FAILED -> R.string.account_error_sign_out
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

@StringRes
private fun AccountUiState.titleRes(): Int = when (this) {
    is AccountUiState.SignedOut, AccountUiState.SigningIn -> R.string.account_login_title
    is AccountUiState.SignedIn -> R.string.account_dashboard_title
    AccountUiState.Loading, AccountUiState.SigningOut -> R.string.account_title
}
