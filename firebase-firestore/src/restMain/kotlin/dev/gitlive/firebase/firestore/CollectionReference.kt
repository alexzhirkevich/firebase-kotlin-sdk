package dev.gitlive.firebase.firestore

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer

actual class CollectionReference(
    actual val path: String,
    internal val firestore: FirebaseFirestore
) : Query(path, firestore) {

    actual val document: DocumentReference
        get() = DocumentReference(
            path = path,
        ).apply {
            firestore = this@CollectionReference.firestore
        }

    actual val parent: DocumentReference?
        get() = path.takeIf {
            PATH_DELIMITER in it.substringAfter(firestore.documentsPath).drop(1)
        }?.let {
            val newPath = path.substringBeforeLast(PATH_DELIMITER)
            DocumentReference(
                path = newPath,
            ).apply {
                firestore = this@CollectionReference.firestore
            }
        }

    actual fun document(documentPath: String): DocumentReference {
        return DocumentReference(
            path = "$path$PATH_DELIMITER$documentPath",
        ).apply {
            firestore = this@CollectionReference.firestore
        }
    }


    actual suspend inline fun <reified T> add(
        data: T,
        encodeDefaults: Boolean
    ): DocumentReference = add(serializer<T>(), data, encodeDefaults)

    @Deprecated("This will be replaced with add(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean = true)")
    actual suspend fun <T> add(
        data: T,
        strategy: SerializationStrategy<T>,
        encodeDefaults: Boolean
    ): DocumentReference = add(strategy, data, encodeDefaults)

    actual suspend fun <T> add(
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean
    ): DocumentReference = firestore.createDocument(
        collectionReference = this,
        strategy = strategy,
        data = data,
        encodeDefaults = encodeDefaults
    )

}