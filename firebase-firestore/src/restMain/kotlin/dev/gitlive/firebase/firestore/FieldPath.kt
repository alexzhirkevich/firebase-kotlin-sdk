package dev.gitlive.firebase.firestore

import kotlinx.serialization.Serializable

actual class FieldPath actual constructor(vararg fieldNames: String) {
    actual val documentId: FieldPath get() = TODO()
}

/** Represents a Firebase FieldValue. */
@Serializable(with = FieldValueSerializer::class)
actual class FieldValue internal actual constructor(nativeValue: Any) {
    internal actual val nativeValue: Any get() = TODO()

    actual companion object {
        actual val serverTimestamp: FieldValue = TODO()
        actual val delete: FieldValue = TODO()
        actual fun increment(value: Int): FieldValue = TODO()
        actual fun arrayUnion(vararg elements: Any): FieldValue = TODO()
        actual fun arrayRemove(vararg elements: Any): FieldValue = TODO()
    }
}