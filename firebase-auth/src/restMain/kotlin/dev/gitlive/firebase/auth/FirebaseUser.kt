package dev.gitlive.firebase.auth

import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebasePlatform
import dev.gitlive.firebase.key
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


private const val NotLinkedError = "This user is not linked to a FirebaseAuth instance"

@Serializable
actual class FirebaseUser internal constructor(
    actual val uid: String,
    actual val displayName: String?,
    actual val email: String?,
    actual val phoneNumber: String?,
    actual val photoURL: String?,
    actual val isAnonymous: Boolean,
    actual val isEmailVerified: Boolean,
    actual val metaData: UserMetaData?,
    actual val multiFactor: MultiFactor,
    actual val providerData: List<UserInfo>,
    actual val providerId: String,
){

    @Transient
    internal var idToken : String? = null

    @Transient
    internal var auth : FirebaseAuth? = null

    actual suspend fun delete() {
        requireNotNull(auth){
            NotLinkedError
        }.delete(this)
    }


    actual suspend fun getIdToken(forceRefresh: Boolean): String? {
        if (!forceRefresh)
            return idToken

        return auth?.currentUser?.idToken.also {
            this.idToken = it
        }
    }

    actual suspend fun getIdTokenResult(forceRefresh: Boolean): AuthTokenResult {
        TODO("Not yet implemented")
    }

    actual suspend fun linkWithCredential(credential: AuthCredential): AuthResult {
        return requireNotNull(auth) {
            NotLinkedError
        }.run {
            linkProvider(
                credential = credential
            )
            signOut()
            signInWithCredential(credential)
        }
    }

    actual suspend fun reauthenticate(credential: AuthCredential) {
        reauthenticateAndRetrieveData(credential)
    }

    actual suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult {
        return requireNotNull(auth){
            NotLinkedError
        }.run {
            signOut()
            signInWithCredential(credential)
        }
    }
    actual suspend fun sendEmailVerification(actionCodeSettings: ActionCodeSettings?) {
        TODO("Not yet implemented")
    }

    actual suspend fun unlink(provider: String) : FirebaseUser? {
        requireNotNull(auth) {
            NotLinkedError
        }.unlinkProvider(
//            idToken = requireNotNull(getIdToken(true)) {
//                "Failed to get id token"
//            },
            providerId = provider
        )

        return this.copy(
            providerData = providerData.filter { it.providerId != providerId }
        )
    }
    actual suspend fun updateEmail(email: String) {
        requireNotNull(auth) {
            NotLinkedError
        }.changeEmail(
            email = email
        )
    }
    actual suspend fun updatePassword(password: String) {
        requireNotNull(auth) {
            NotLinkedError
        }.changePassword(
            password = password
        )
    }
//    suspend fun updatePhoneNumber(credential: PhoneAuthCredential) {
//        TODO("Not yet implemented")
//    }

    actual suspend fun updateProfile(displayName: String?, photoUrl: String?) {
        requireNotNull(auth).updateCurrentUser(
            this.copy(displayName = displayName, photoURL = photoUrl)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FirebaseUser

        if (uid != other.uid) return false
        if (displayName != other.displayName) return false
        if (email != other.email) return false
        if (phoneNumber != other.phoneNumber) return false
        if (photoURL != other.photoURL) return false
        if (isAnonymous != other.isAnonymous) return false
        if (isEmailVerified != other.isEmailVerified) return false
        if (metaData != other.metaData) return false
        if (multiFactor != other.multiFactor) return false
        if (providerData != other.providerData) return false
        if (providerId != other.providerId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (phoneNumber?.hashCode() ?: 0)
        result = 31 * result + (photoURL?.hashCode() ?: 0)
        result = 31 * result + isAnonymous.hashCode()
        result = 31 * result + isEmailVerified.hashCode()
        result = 31 * result + (metaData?.hashCode() ?: 0)
        result = 31 * result + multiFactor.hashCode()
        result = 31 * result + providerData.hashCode()
        result = 31 * result + providerId.hashCode()
        return result
    }

    private fun copy(
        uid: String = this.uid,
        displayName: String? = this.displayName,
        email: String? = this.email,
        phoneNumber: String? = this.phoneNumber,
        photoURL: String? = this.photoURL,
        isAnonymous: Boolean = this.isAnonymous,
        isEmailVerified: Boolean = this.isEmailVerified,
        metaData: UserMetaData? = this.metaData,
        multiFactor: MultiFactor = this.multiFactor,
        providerData: List<UserInfo> = this.providerData,
        providerId: String = this.providerId,
    ) = FirebaseUser(
        uid = uid,
        displayName = displayName,
        email = email,
        phoneNumber = phoneNumber,
        photoURL = photoURL,
        isAnonymous = isAnonymous,
        isEmailVerified = isEmailVerified,
        metaData = metaData,
        multiFactor = multiFactor,
        providerData = providerData,
        providerId = providerId
    ).apply {
        idToken = this@FirebaseUser.idToken
        auth = this@FirebaseUser.auth
    }

    //TODO set other properties
    actual suspend fun reload() {
        auth?.getUserData()?.let {
            idToken = it.idToken
        }
    }

    actual suspend fun updatePhoneNumber(credential: PhoneAuthCredential) {
        TODO("Not yet implemented")
    }

    actual suspend fun verifyBeforeUpdateEmail(
        newEmail: String,
        actionCodeSettings: ActionCodeSettings?
    ) {
        TODO("Not yet implemented")
    }
}

@Serializable
actual class UserInfo internal constructor(
    actual val providerId: String,
    actual val displayName: String?,
    actual val photoURL: String?,
    actual val email: String?,
    actual val phoneNumber: String?,
    @SerialName("localId")
    actual val uid: String = "",
)

@Serializable
actual class UserMetaData internal constructor(
    actual val creationTime: Double?,
    actual val lastSignInTime: Double?,
)

private const val USER_KEY = "dev.gitlive.firebase.auth.FirebaseUser"

internal fun FirebaseApp.saveUser(user : FirebaseUser) {
    platform.store(key(USER_KEY), Json.encodeToString(user))
}
internal fun FirebaseApp.loadUser() : FirebaseUser? {
    return platform.retrieve(key(USER_KEY))?.let {
        Json.decodeFromString(it)
    }
}
