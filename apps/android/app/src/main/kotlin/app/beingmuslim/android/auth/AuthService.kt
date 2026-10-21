package app.beingmuslim.android.auth

/**
 * The account operations the screens need. The Appwrite SDK is hidden behind
 * this seam so the guard and session transitions can be tested with a fake.
 */
interface AuthService {

    /** The signed-in user, or null when there is no usable session. */
    suspend fun currentUser(): AuthUser?

    /** Exchanges the token flow's one-time secret for a session. */
    suspend fun createSession(userId: String, secret: String): AuthUser

    /** Ends the current session. */
    suspend fun signOut()
}
