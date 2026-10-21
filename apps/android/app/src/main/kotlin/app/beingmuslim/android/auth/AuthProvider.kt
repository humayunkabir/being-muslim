package app.beingmuslim.android.auth

/** The OAuth2 providers this app offers; the id is used in the token endpoint. */
enum class AuthProvider(val id: String) {
    GOOGLE("google"),
    APPLE("apple"),
}
