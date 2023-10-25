package dev.gitlive.firebase.auth

import kotlinx.serialization.json.JsonPrimitive

private const val ID_TOKEN = "id_token"
private const val ACCESS_TOKEN = "access_token"
private const val SECRET = "oauth_token_secret"
private const val PROVIDER_ID = "providerId"
private const val SIGN_IN_METHOD = "signInMethod"
private const val GOOGLE_ID = "google.com"
private const val FACEBOOK_ID = "facebook.com"
private const val GITHUB_ID = "github.com"
private const val TWITTER_ID = "twitter.com"

actual open class AuthCredential (
    actual val providerId: String,
    vararg args : Pair<String, String?>

) {
    open val postBody: String = args
        .toMap()
        .mapValues { JsonPrimitive(it.value) }
        .plus(PROVIDER_ID to providerId)
        .toString()
}

actual class PhoneAuthCredential(
    val verificationCode : String?,
    val sessionInfo : String?,
    val temporaryProof : String?,
    val phoneNumber : String?
) : AuthCredential("phone") {


    companion object {
        fun credentialWithVerificationCode(
            verificationCode : String,
            sessionInfo: String
        ) : PhoneAuthCredential {
            return PhoneAuthCredential(
                verificationCode = verificationCode,
                sessionInfo = sessionInfo,
                temporaryProof = null,
                phoneNumber = null
            )
        }

        fun credentialWithTemporaryProof(
            temporaryProof : String,
            phoneNumber: String
        ) : PhoneAuthCredential {
            return PhoneAuthCredential(
                verificationCode = null,
                sessionInfo = null,
                temporaryProof = temporaryProof,
                phoneNumber = phoneNumber
            )
        }
    }
}

actual class OAuthCredential : AuthCredential("oauth")

actual object EmailAuthProvider {
    actual fun credential(email: String, password: String): AuthCredential {
        TODO("Not yet implemented")
    }
    actual fun credentialWithLink(email: String, emailLink: String): AuthCredential {
        TODO("Not yet implemented")
    }
}

actual object FacebookAuthProvider {
    actual fun credential(accessToken: String): AuthCredential {
        return AuthCredential(
            providerId = FACEBOOK_ID,
            ACCESS_TOKEN to accessToken,
        )
    }
}
//
actual object GithubAuthProvider {
    actual fun credential(token: String): AuthCredential {
        return AuthCredential(
            providerId = GITHUB_ID,
            ACCESS_TOKEN to token,
        )
    }
}

actual object GoogleAuthProvider {
    actual fun credential(idToken: String?, accessToken: String?): AuthCredential {

        require(idToken != null || accessToken != null) {
            "Both parameters are optional but at least one must be present."
        }

        return AuthCredential(
            providerId = GOOGLE_ID,
            ID_TOKEN to idToken,
            ACCESS_TOKEN to accessToken,
        )
    }
}

actual object TwitterAuthProvider {
    actual fun credential(token: String, secret: String): AuthCredential {
        return AuthCredential(
            providerId = TWITTER_ID,
            ACCESS_TOKEN to token,
            SECRET to secret
        )
    }
}

actual class OAuthProvider actual constructor(
    provider: String,
    scopes: List<String>,
    customParameters: Map<String, String>,
    auth: FirebaseAuth
) {
    actual companion object {
        actual fun credential(providerId: String, accessToken: String?, idToken: String?, rawNonce: String?): OAuthCredential{
            TODO("Not yet implemented")
        }
    }
}

actual class PhoneAuthProvider actual constructor(auth: FirebaseAuth) {
    actual fun credential(verificationId: String, smsCode: String): PhoneAuthCredential {
        TODO("Not yet implemented")
    }
    actual suspend fun verifyPhoneNumber(phoneNumber: String, verificationProvider: PhoneVerificationProvider): AuthCredential {
        TODO("Not yet implemented")
    }
}
//
actual interface PhoneVerificationProvider
//
