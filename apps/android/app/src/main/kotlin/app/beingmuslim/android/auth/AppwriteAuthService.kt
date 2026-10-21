package app.beingmuslim.android.auth

import android.content.Context
import app.beingmuslim.android.BuildConfig
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.User
import io.appwrite.services.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The Appwrite implementation behind [AuthService]. */
class AppwriteAuthService(context: Context) : AuthService {

    private val appContext = context.applicationContext

    // Keystore-backed preferences are read and decrypted when the client is
    // built, so build it on the first call rather than on the UI thread. A
    // broken Keystore then reaches the caller as a normal auth failure.
    private val account by lazy {
        Account(
            Client(SecureCookieContext(appContext))
                .setEndpoint(BuildConfig.APPWRITE_ENDPOINT)
                .setProject(BuildConfig.APPWRITE_PROJECT_ID),
        )
    }

    override suspend fun currentUser(): AuthUser? = withContext(Dispatchers.IO) {
        try {
            account.get().toAuthUser()
        } catch (exception: AppwriteException) {
            // 401 is the ordinary "no session" answer; anything else is a real
            // failure and must not silently look like a signed-out user.
            if (exception.code == 401) null else throw exception
        }
    }

    override suspend fun createSession(userId: String, secret: String): AuthUser =
        withContext(Dispatchers.IO) {
            account.createSession(userId, secret)
            account.get().toAuthUser()
        }

    override suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        account.deleteSession("current")
    }
}

private fun User<*>.toAuthUser(): AuthUser = AuthUser(
    id = id,
    name = name.takeIf { it.isNotBlank() },
    email = email.takeIf { it.isNotBlank() },
)
