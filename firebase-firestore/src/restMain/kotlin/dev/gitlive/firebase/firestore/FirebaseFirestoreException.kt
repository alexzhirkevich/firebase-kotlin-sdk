package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.FirebaseException

actual class FirebaseFirestoreException(
    val code : FirestoreExceptionCode
) : FirebaseException()


@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
actual val FirebaseFirestoreException.code: FirestoreExceptionCode
    get() = code

actual enum class FirestoreExceptionCode {
    OK,
    CANCELLED,
    UNKNOWN,
    INVALID_ARGUMENT,
    DEADLINE_EXCEEDED,
    NOT_FOUND,
    ALREADY_EXISTS,
    PERMISSION_DENIED,
    RESOURCE_EXHAUSTED,
    FAILED_PRECONDITION,
    ABORTED,
    OUT_OF_RANGE,
    UNIMPLEMENTED,
    INTERNAL,
    UNAVAILABLE,
    DATA_LOSS,
    UNAUTHENTICATED
}