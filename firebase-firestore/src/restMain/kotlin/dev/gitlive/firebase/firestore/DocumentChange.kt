package dev.gitlive.firebase.firestore

actual enum class ChangeType {
    ADDED ,
    MODIFIED,
    REMOVED
}

actual class DocumentChange internal constructor(
    actual val document: DocumentSnapshot,
    actual val newIndex: Int,
    actual val oldIndex: Int,
    actual val type: ChangeType
)