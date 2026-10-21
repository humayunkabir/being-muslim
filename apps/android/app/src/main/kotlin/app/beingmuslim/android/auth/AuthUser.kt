package app.beingmuslim.android.auth

/** The signed-in user, reduced to what the account screen shows. */
data class AuthUser(
    val id: String,
    val name: String?,
    val email: String?,
) {
    /** The name, then the email, then the opaque id: never a blank line. */
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: email?.takeIf { it.isNotBlank() }
            ?: id
}
