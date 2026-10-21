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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.beingmuslim.android.R
import app.beingmuslim.android.auth.AccountError
import app.beingmuslim.android.auth.AccountUiState
import app.beingmuslim.android.auth.AuthProvider

/**
 * The signed-out login screen and the signed-in dashboard, plus their loading
 * and failure states (`docs/PRODUCT_REQUIREMENTS.md` §10). Every account action
 * is reported to the caller, which is the only place that talks to Appwrite;
 * only the delete confirmation's visibility is local transient state.
 *
 * The Google mark comes from the supplied SVG. It is decorative because the
 * button already names the provider.
 */
@Composable
fun AccountScreen(
    state: AccountUiState,
    onSignIn: (AuthProvider) -> Unit,
    onSignOut: () -> Unit,
    onExportData: () -> Unit,
    onDeleteAccount: () -> Unit,
    onCancel: () -> Unit,
    onExit: () -> Unit,
    @StringRes exitLabel: Int,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = onBack != null) { onBack?.invoke() }

    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    if (confirmDelete && state is AccountUiState.SignedIn) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.account_delete_title)) },
            text = { Text(stringResource(R.string.account_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.account_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.account_delete_cancel))
                }
            },
        )
    }

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
                        onClick = { onSignIn(AuthProvider.GOOGLE) },
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
                    OutlinedButton(
                        onClick = { onSignIn(AuthProvider.APPLE) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.account_sign_in_apple))
                    }
                    TextButton(onClick = onExit) {
                        Text(stringResource(exitLabel))
                    }
                }

                is AccountUiState.SigningIn -> {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.account_signing_in),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!state.exchanging) {
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.account_cancel_sign_in))
                        }
                        TextButton(onClick = onExit) {
                            Text(stringResource(exitLabel))
                        }
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
                    if (state.exporting) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.account_exporting),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        OutlinedButton(
                            onClick = onExportData,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.account_export_data))
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.account_sign_out))
                    }
                    TextButton(
                        onClick = { confirmDelete = true },
                        enabled = !state.exporting,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.account_delete_account))
                    }
                }

                AccountUiState.SigningOut -> {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.account_signing_out),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                AccountUiState.Deleting -> {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.account_deleting),
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
                AccountError.SIGN_IN_CANCELLED -> R.string.account_error_cancelled
                AccountError.SIGN_OUT_FAILED -> R.string.account_error_sign_out
                AccountError.EXPORT_FAILED -> R.string.account_error_export
                AccountError.DELETE_FAILED -> R.string.account_error_delete
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

@StringRes
private fun AccountUiState.titleRes(): Int = when (this) {
    is AccountUiState.SignedOut, is AccountUiState.SigningIn -> R.string.account_login_title
    is AccountUiState.SignedIn -> R.string.account_dashboard_title
    AccountUiState.Loading, AccountUiState.SigningOut, AccountUiState.Deleting ->
        R.string.account_title
}
