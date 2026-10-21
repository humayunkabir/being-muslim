package app.beingmuslim.android.auth

import android.content.Context
import app.beingmuslim.android.BuildConfig
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.User
import io.appwrite.services.Account

/** The Appwrite implementation behind [AuthService]. */
class AppwriteAuthService(context: Context) : AuthService {

    private val account = Account(
        Client(context.applicationContext)
            .setEndpoint(BuildConfig.APPWRITE_ENDPOINT)
            .setProject(BuildConfig.APPWRITE_PROJECT_ID),
    )

    override suspend fun currentUser(): AuthUser? =
        try {
            account.get().toAuthUser()
        } catch (exception: AppwriteException) {
            // 401 is the ordinary "no session" answer; anything else is a real
            // failure and must not silently look like a signed-out user.
            if (exception.code == 401) null else throw exception
        }

    override suspend fun createSession(userId: String, secret: String): AuthUser {
        account.createSession(userId, secret)
        return account.get().toAuthUser()
    }

    override suspend fun signOut() {
        account.deleteSession("current")
    }
}

private fun User<*>.toAuthUser(): AuthUser = AuthUser(
    id = id,
    name = name.takeIf { it.isNotBlank() },
    email = email.takeIf { it.isNotBlank() },
)
