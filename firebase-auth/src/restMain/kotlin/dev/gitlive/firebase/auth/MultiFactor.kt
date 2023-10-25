package dev.gitlive.firebase.auth

import kotlinx.serialization.Serializable


@Serializable
actual class MultiFactor {
    actual val enrolledFactors: List<MultiFactorInfo>  get() =
        TODO("Not yet implemented")

    actual suspend fun enroll(multiFactorAssertion: MultiFactorAssertion, displayName: String?) {
        TODO("Not yet implemented")
    }
    actual suspend fun getSession(): MultiFactorSession {
        TODO("Not yet implemented")
    }
    actual suspend fun unenroll(multiFactorInfo: MultiFactorInfo) {
        TODO("Not yet implemented")
    }
    actual suspend fun unenroll(factorUid: String) {
        TODO("Not yet implemented")
    }
}

actual class MultiFactorInfo(
    actual val displayName: String?,
    actual val enrollmentTime: Double,
    actual val factorId: String,
    actual val uid: String,
)

actual class MultiFactorAssertion(
    actual val factorId: String
)

actual class MultiFactorSession

actual class MultiFactorResolver(
    actual val auth: FirebaseAuth,
    actual val hints: List<MultiFactorInfo>,
    actual val session: MultiFactorSession,
) {
    actual suspend fun resolveSignIn(assertion: MultiFactorAssertion): AuthResult {
        TODO("Not yet implemented")
    }
}