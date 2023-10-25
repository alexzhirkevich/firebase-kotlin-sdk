package dev.gitlive.firebase.auth

import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.key

class FirebaseAuthTokens(
    val idToken : String,
    val refreshToken : String
)

private const val ID_TOKEN_KEY = "dev.gitlive.firebase.auth.IdToken"
private const val REFRESH_TOKEN_KEY = "dev.gitlive.firebase.auth.RefreshToken"

internal fun FirebaseApp.loadTokens() : FirebaseAuthTokens? {
    val idToken = platform.retrieve("$ID_TOKEN_KEY-$name")
    val refreshToken = platform.retrieve("$REFRESH_TOKEN_KEY-$name")

    if (idToken == null || refreshToken == null)
        return null

    return FirebaseAuthTokens(idToken = idToken, refreshToken = refreshToken)
}

internal fun FirebaseApp.saveTokens(tokens: FirebaseAuthTokens) {
    platform.store(key(ID_TOKEN_KEY), tokens.idToken)
    platform.store(key(REFRESH_TOKEN_KEY), tokens.refreshToken)
}

internal fun FirebaseApp.clearTokens() {
    platform.clear(key(ID_TOKEN_KEY))
    platform.clear(key(REFRESH_TOKEN_KEY))
}
