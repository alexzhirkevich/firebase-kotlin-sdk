@file:JvmName("AuthKt")

package dev.gitlive.firebase.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import dev.gitlive.firebase.FirebaseService
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.clearTokens
import dev.gitlive.firebase.auth.loadTokens
import dev.gitlive.firebase.auth.saveTokens
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

private const val API_URL= "https://identitytoolkit.googleapis.com/v1/accounts"
private const val LOCALE_HEADER = "X-Firebase-Locale"
private const val REDIRECT_URI = "http://localhost"

actual val Firebase.auth: FirebaseAuth
    get() = auth(app)

actual fun Firebase.auth(app: FirebaseApp): FirebaseAuth {

    return app.getOrPut(FirebaseAuth::class) {
        FirebaseAuth(it)
    }
}

actual class FirebaseAuth(
    val app : FirebaseApp,
    val refreshTokenManager: RefreshTokenManager = app.getOrPut(RefreshTokenManager::class) {
        RefreshTokenManager(it)
    },
    val ktorClient: HttpClient = DefaultFirebaseHttpClient(app, refreshTokenManager)
) : FirebaseService {


    private val _currentUserFlow = MutableStateFlow<FirebaseUser?>(null)

    private var cachedCurrentUser: FirebaseUser? = null

    actual val currentUser: FirebaseUser?
        get() = app.loadUser()?.also{
            it.idToken = app.loadTokens()?.idToken
        }


    actual val authStateChanged: Flow<FirebaseUser?> = flow {
        kotlin.runCatching {
            val user = getUserData()
            _currentUserFlow.emit(user)
        }
        _currentUserFlow.collect(this)

    }.distinctUntilChanged().onEach {
        cachedCurrentUser = it
    }

    actual val idTokenChanged: Flow<FirebaseUser?>
        get() = idTokenFlow.map { token ->
            cachedCurrentUser.apply {
                this?.idToken = token
            }
        }

    actual var languageCode: String = "en-US"

//    suspend fun currentUser(): FirebaseUser? {
//        return kotlin.runCatching {
//            getUserData()
//        }.ignoreCancellation()
//            .getOrNull()
//    }

    actual suspend fun applyActionCode(code: String) {
        TODO("Not yet imnplemented")
    }

    actual suspend fun <T : ActionCodeResult> checkActionCode(code: String): T {
        TODO("Not yet imnplemented")
    }

    actual suspend fun confirmPasswordReset(code: String, newPassword: String) {
        performRequest(
            action = "resetPassword"
        ) {
            setBody(
                VerifyPasswordResetRequest(
                    oobCode = code,
                    newPassword = newPassword
                )
            )
        }
    }

    actual suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): AuthResult {
        val resp = performRequest(
            action = "signUp"
        ) {
            setBody(
                CreateUserWithEmailAndPasswordRequest(
                    email = email,
                    password = password
                )
            )
        }.body<SignInResponse>()

        return getAndMapUser(resp.idToken, resp.refreshToken)
    }

    actual suspend fun fetchSignInMethodsForEmail(email: String): List<String> {
        val resp = performRequest("createAuthUri") {
            setBody(
                FetchSignInMethodRequest(
                    identifier = email,
                    continueUri = ""
                )
            )
        }.body<FetchSignInMethodResponse>()

        return resp.allProviders
    }

    actual suspend fun sendPasswordResetEmail(
        email: String,
        actionCodeSettings: ActionCodeSettings?
    ) {
        performRequest(
            action = "sendOobCode"
        ) {
            header(LOCALE_HEADER, languageCode)

            setBody(
                SendPasswordResetEmailRequest(
                    email = email
                )
            )
        }
    }

    actual suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: ActionCodeSettings
    ) {
        TODO("Not yet imnplemented")
    }

    actual fun isSignInWithEmailLink(link: String): Boolean {
        TODO("Not yet imnplemented")
    }

    actual suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult {
        val resp = performRequest(
            action = "signInWithPassword"
        ) {
            setBody(
                CreateUserWithEmailAndPasswordRequest(
                    email = email,
                    password = password
                )
            )
        }.body<SignInResponse>()

        return getAndMapUser(resp.idToken, resp.refreshToken)
    }

    actual suspend fun signInWithCustomToken(token: String): AuthResult {
        val resp = performRequest(
            action = "signInWithCustomToken"
        ) {

            setBody(
                SignInWithCustomTokenRequest(
                    token = token
                )
            )
        }.body<SignInResponse>()

        return getAndMapUser(resp.idToken, resp.refreshToken)
    }

    actual suspend fun signInAnonymously(): AuthResult {
        val resp = performRequest(
            action = "signUp"
        ) {
            setBody(SignInAnonymouslyRequest())
        }.body<SignInResponse>()

        return getAndMapUser(resp.idToken, resp.refreshToken)
    }

    actual suspend fun signInWithCredential(authCredential: AuthCredential): AuthResult {
        val resp = performRequest("signInWithIdp") {
            setBody(
                SignInWithCredentialRequest(
                    requestUri = REDIRECT_URI,
                    postBody = authCredential.postBody,
                )
            )
        }.body<SignInWithCredentialResponse>()

        app.saveTokens(
            FirebaseAuthTokens(
                idToken = resp.idToken,
                refreshToken = resp.refreshToken
            )
        )

        val user = resp.toFirebaseUser().apply {
            idToken = resp.idToken
            auth = this@FirebaseAuth
        }

        return AuthResult(user)
    }

    actual suspend fun signInWithEmailLink(email: String, link: String): AuthResult {
        val resp = performRequest("signInWithEmailLink") {
            setBody(
                SignInWithEmailLinkRequest(
                    email = email,
                    oobCode = link,
                )
            )
        }.body<SignInResponse>()

        return getAndMapUser(
            idToken = resp.idToken,
            refreshToken = resp.refreshToken
        )
    }

//    suspend fun sentPhoneVerificationCode(
//        phoneNumber: String,
//        recaptchaToken : String?
//    ) : SessionInfo {
//        return performRequest("sendVerificationCode") {
//            setBody(
//                SendPhoneVerificationCodeRequest(
//                    phoneNumber = phoneNumber,
//                    recaptchaToken = recaptchaToken,
//                )
//            )
//        }.body()
//    }

//    suspend fun signInWithPhoneNumber(
//        credential: PhoneAuthCredential
//    ): PhoneAuthResult {
//        return signInWithPhoneNumberInternal(credential)
//    }
//
//    private suspend fun signInWithPhoneNumberInternal(
//        credential: PhoneAuthCredential,
//        link: Boolean = false
//    ): PhoneAuthResult {
//        val resp =  performRequest("sendVerificationCode") {
//            setBody(
//                SignInWithPhoneNumberRequest(
//                    idToken = if (link) it else null,
//                    sessionInfo = credential.sessionInfo,
//                    code = credential.verificationCode,
//                    temporaryProof = credential.temporaryProof,
//                    phoneNumber = credential.temporaryProof
//                )
//            )
//        }.body<SignInWithPhoneNumberResponse>()
//
//        return PhoneAuthResultImpl(
//            user = getAndMapUser(resp.idToken, resp.refreshToken).user.also {
//                it?.auth = this
//                it?.idToken = resp.idToken
//            },
//            temporaryProof = if (resp.temporaryProof != null && resp.phoneNumber != null)
//                TemporaryProof(
//                    temporaryProof = resp.temporaryProof,
//                    phoneNumber = resp.phoneNumber
//                ) else null
//        )
//    }

    actual suspend fun signOut() {
        app.clearTokens()
        _currentUserFlow.emit(null)
    }

    actual suspend fun updateCurrentUser(user: FirebaseUser) {
        performRequest(
            action = "update"
        ) { idToken ->
            setBody(
                UpdateUserRequest(
                    idToken = idToken.orEmpty(),
                    displayName = user.displayName.orEmpty(),
                    photoUrl = user.photoURL.orEmpty(),
                    deleteAttribute = buildList {
                        if (user.displayName == null)
                            add("DISPLAY_NAME")
                        if (user.photoURL == null)
                            add("PHOTO_URL")
                    }
                )
            )
        }
    }

    actual suspend fun verifyPasswordResetCode(code: String): String {
        val resp = performRequest(
            action = "resetPassword"
        ) {
            setBody(
                VerifyPasswordResetCodeRequest(
                    oobCode = code
                )
            )
        }.body<VerifyPasswordResetCodeResponse>()

        return resp.email
    }

    internal suspend fun delete(user: FirebaseUser) {
        var refreshed = false
        var token: String = user.getIdToken(false)
            ?: user.getIdToken(true).also { refreshed = true }
            ?: throw Exception("Unauthorized")

        val action = "delete"
        try {
            performRequest(action) {
                setBody(RequestFromIdToken(token))
            }
        } catch (t: Throwable) {
            if (!refreshed) {
                token = user.getIdToken(true) ?: throw Exception("Unauthorized")
                performRequest(action) {
                    setBody(RequestFromIdToken(token))
                }
            }
        }
    }

//    override fun useEmulator(host: String, port: Int) {
//        TODO("Not yet imnplemented")
//    }

    private suspend fun getAndMapUser(idToken: String, refreshToken: String): AuthResult {
        app.saveTokens(
            FirebaseAuthTokens(
                idToken = idToken,
                refreshToken = refreshToken
            )
        )

        val user = getUserData()

        app.saveUser(user)

        _currentUserFlow.emit(user)

        return AuthResult(user)
    }

    internal suspend fun getUserData(): FirebaseUser {

        var idTkn : String? = null
        val resp = performRequest(
            action = "lookup"
        ) { idToken ->
            setBody(RequestFromIdToken(idToken = idToken.orEmpty()))
            idTkn = idToken
        }.body<GetUserDataResponse>()


        return resp.users.first().toFirebaseUser().apply {
            this.idToken = idTkn
            this.auth = this@FirebaseAuth
        }
    }

    internal suspend fun changeEmail(
        email: String
    ) {
        performRequest("update") { idToken ->
            header(LOCALE_HEADER, languageCode)

            setBody(
                ChangeEmailRequest(
                    email = email,
                    idToken = idToken.orEmpty()
                )
            )
        }
    }

    internal suspend fun changePassword(
        password: String
    ) {
        performRequest("update") { idToken ->
            header(LOCALE_HEADER, languageCode)

            setBody(
                ChangePasswordRequest(
                    password = password,
                    idToken = idToken.orEmpty()
                )
            )
        }
    }

    internal suspend fun unlinkProvider(
        providerId: String
    ) {
        performRequest("update") { idToken ->
            setBody(
                UnlinkProviderRequest(
                    deleteProvider = providerId,
                    idToken = idToken.orEmpty()
                )
            )
        }
    }

    internal suspend fun linkProvider(
        credential: AuthCredential
    ): AuthResult {

//        if (credential is PhoneAuthCredential){
//            return signInWithPhoneNumberInternal(credential, true)
//        }

        val res = performRequest("signInWithIdp") { idToken ->
            setBody(
                LinkCredentialRequest(
                    idToken = idToken.orEmpty(),
                    requestUri = REDIRECT_URI,
                    postBody = credential.postBody
                )
            )
        }.body<SignInWithCredentialResponse>()

        app.saveTokens(
            FirebaseAuthTokens(
                idToken = res.idToken,
                refreshToken = res.refreshToken
            )
        )
        val user = res.toFirebaseUser().apply {
            this.auth = this@FirebaseAuth
            this.idToken = res.idToken
        }
        return AuthResult(user)
    }

    private suspend fun performRequest(
        action: String,
        builder: HttpRequestBuilder.(idToken : String?) -> Unit = {},
    ): HttpResponse {

        runCatching {
            refreshTokenManager.awaitRefreshEnd()
        }.ignoreCancellation()

        val tokens = app.loadTokens()

        val perform : suspend (tokens : FirebaseAuthTokens?) -> HttpResponse = {
            ktorClient.post(
                urlString = "$API_URL:$action"
            ) {
                parameter(KEY_PARAM_NAME, app.options.apiKey)
                builder.invoke(this, it?.idToken)
            }
        }

        return runCatching {
            perform.invoke(tokens)
        }.onFailure {
            if (it is ClientRequestException &&
                it.response.status == HttpStatusCode.BadRequest &&
                "INVALID_ID_TOKEN" in it.message
            ) {
                val newTokens = refreshTokenManager
                    .refresh(ktorClient)

                if (newTokens != null)
                    return perform(newTokens)
            }
        }.getOrThrow()
    }

    actual fun useEmulator(host: String, port: Int) {
        TODO("Not yet implemented")
    }

    override fun release() {
        ktorClient.cancel()
    }
}

actual class AuthResult(
    actual val user : FirebaseUser?
)

actual class AuthTokenResult(
    actual val claims: Map<String, Any>,
    actual val signInProvider: String?,
    actual val token: String?,
)

actual open class FirebaseAuthException : FirebaseException()
actual class FirebaseAuthActionCodeException : FirebaseAuthException()
actual class FirebaseAuthEmailException : FirebaseAuthException()
actual open class FirebaseAuthInvalidCredentialsException : FirebaseAuthException()
actual class FirebaseAuthWeakPasswordException: FirebaseAuthInvalidCredentialsException()
actual class FirebaseAuthInvalidUserException : FirebaseAuthException()
actual class FirebaseAuthMultiFactorException: FirebaseAuthException()
actual class FirebaseAuthRecentLoginRequiredException : FirebaseAuthException()
actual class FirebaseAuthUserCollisionException : FirebaseAuthException()
actual class FirebaseAuthWebException : FirebaseAuthException()


@Serializable
private class SignInAnonymouslyRequest(
    val returnSecureToken: Boolean = true
)


@Serializable
private class RequestFromIdToken(
    val idToken: String,
)

@Serializable
private class GetUserDataResponse(
    val users : List<GetUserDataInfo>
)

private fun GetUserDataInfo.toFirebaseUser() : FirebaseUser {
    return FirebaseUser(
        uid = localId,
        displayName = displayName,
        email = email,
        phoneNumber = null, //TODO
        photoURL = photoUrl,
        isAnonymous = true,  //TODO
        isEmailVerified = emailVerified,
        metaData = null,  //TODO
        multiFactor = MultiFactor(),  //TODO
        providerData = providerUserInfo,
        providerId = providerUserInfo.first().providerId
    )
}

@Serializable
private class GetUserDataInfo(
    val localId: String,
    val email: String? = null,
    val emailVerified: Boolean,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val providerUserInfo: List<UserInfo> = emptyList()
)


@Serializable
private class SignInWithCustomTokenRequest(
    val token: String,
    val returnSecureToken: Boolean = true
)


@Serializable
private class SendPasswordResetEmailRequest(
    val email: String,
    val requestType: String = "PASSWORD_RESET"
)

@Serializable
private class VerifyPasswordResetCodeRequest(
    val oobCode: String,
)


@Serializable
private class VerifyPasswordResetRequest(
    val oobCode: String,
    val newPassword: String
)


@Serializable
private class VerifyPasswordResetCodeResponse(
    val email: String,
)

@Serializable
private class UpdateUserRequest(
    val idToken: String,
    val displayName: String,
    val photoUrl: String,
    val deleteAttribute : List<String>
)

@Serializable
private class FetchSignInMethodRequest(
    val identifier: String,
    val continueUri: String,
)

@Serializable
private class FetchSignInMethodResponse(
    val allProviders: List<String>,
    val registered: Boolean,
)


@Serializable
private class SignInWithCredentialRequest(
    val requestUri: String,
    val postBody: String,
    val returnSecureToken : Boolean = true,
    val returnIdpCredential : Boolean = true
)

@Serializable
private class LinkCredentialRequest(
    val idToken: String,
    val requestUri: String,
    val postBody: String,
    val returnSecureToken : Boolean = true,
    val returnIdpCredential : Boolean = true
)


@Serializable
private class SignInWithCredentialResponse(
    val localId: String,
    val email: String? = null,
    val emailVerified: Boolean,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val providerId : String,
    val idToken: String,
    val refreshToken: String
)

private fun SignInWithCredentialResponse.toFirebaseUser(): FirebaseUser {
    return FirebaseUser(
        uid = localId,
        displayName = displayName,
        email = email,
        phoneNumber = null, //TODO
        photoURL = photoUrl,
        isAnonymous = true,  //TODO
        isEmailVerified = emailVerified,
        metaData = null,  //TODO
        multiFactor = MultiFactor(),  //TODO
        providerData = listOf(
            UserInfo(
                providerId = providerId,
                displayName = displayName,
                photoURL = photoUrl,
                email = email,
                phoneNumber =  null, //TODO,
                uid = localId
            )
        ),  //TODO
        providerId = providerId //TODO
    )
}


@Serializable
private class SignInResponse(
    val idToken: String,
    val refreshToken : String,
)


@Serializable
private class CreateUserWithEmailAndPasswordRequest(
    val email: String,
    val password : String,
    val returnSecureToken: Boolean = true
)

@Serializable
private class ChangeEmailRequest(
    val email: String,
    val idToken : String,
    val returnSecureToken: Boolean = true
)

@Serializable
private class ChangePasswordRequest(
    val password: String,
    val idToken : String,
    val returnSecureToken: Boolean = true
)

@Serializable
private class UnlinkProviderRequest(
    val deleteProvider: String,
    val idToken : String,
    val returnSecureToken: Boolean = true
)

@Serializable
private class SignInWithEmailLinkRequest(
    val email : String,
    val oobCode : String
)

@Serializable
private class SendPhoneVerificationCodeRequest(
    val phoneNumber : String,
    val recaptchaToken : String?,
    val tenantId : String? = null
)

@Serializable
private class SignInWithPhoneNumberRequest(
    val idToken: String? = null,
    val sessionInfo : String?,
    val code : String?,
    val temporaryProof: String?,
    val phoneNumber: String?
)

@Serializable
private class SignInWithPhoneNumberResponse(
    val idToken : String,
    val refreshToken: String,
    val temporaryProof : String? = null,
    val phoneNumber : String? = null,
)