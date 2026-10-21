package app.beingmuslim.android.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import app.beingmuslim.android.MainActivity

/**
 * Receives the token flow's custom-scheme redirect and hands it to the app.
 *
 * The SDK's `io.appwrite.views.CallbackActivity` belongs to
 * `createOAuth2Session`: it expects the session flow's `key`/`secret` cookie.
 * This flow returns `userId`/`secret` instead, which the app exchanges with
 * `Account.createSession`, so the app owns this callback.
 */
class AuthCallbackActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val redirect = intent?.data
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                data = redirect
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
            },
        )
        finish()
    }
}
