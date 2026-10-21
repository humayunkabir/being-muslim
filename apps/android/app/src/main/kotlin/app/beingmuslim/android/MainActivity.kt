package app.beingmuslim.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import app.beingmuslim.android.auth.AccountViewModel
import app.beingmuslim.android.ui.AuthGate
import app.beingmuslim.android.ui.theme.BeingMuslimTheme

// AppCompatActivity rather than ComponentActivity: it is what applies the
// user's chosen app language on API 32 and below (issue #14).
class MainActivity : AppCompatActivity() {

    private val accountViewModel: AccountViewModel by viewModels {
        AccountViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthIntent(intent)
        setContent {
            BeingMuslimTheme {
                AuthGate(accountViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val data = intent?.dataString ?: return
        // Consume it so a configuration change does not exchange the one-time
        // token a second time.
        intent.data = null
        accountViewModel.handleRedirect(data)
    }
}
