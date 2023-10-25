package dev.gitlive.firebase.firestore

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy

actual class Transaction {
    actual fun set(
        documentRef: DocumentReference,
        data: Any,
        encodeDefaults: Boolean,
        merge: Boolean
    ): Transaction {
        TODO("Not yet implemented")
    }

    actual fun set(
        documentRef: DocumentReference,
        data: Any,
        encodeDefaults: Boolean,
        vararg mergeFields: String
    ): Transaction {
        TODO("Not yet implemented")
    }

    actual fun set(
        documentRef: DocumentReference,
        data: Any,
        encodeDefaults: Boolean,
        vararg mergeFieldPaths: FieldPath
    ): Transaction {
        TODO("Not yet implemented")
    }

    actual fun <T> set(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        merge: Boolean
    ): Transaction {
        TODO("Not yet implemented")
    }

    actual fun <T> set(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        vararg mergeFields: String
    ): Transaction {
        TODO("Not yet implemented")
    }

    actual fun <T> set(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        vararg mergeFieldPaths: FieldPath
    ): Transaction {
        TODO("Not yet implemented")
    }

    actual fun update(
        documentRef: DocumentReference,
        data: Any,
        encodeDefaults: Boolean
    ): Transaction {
        TODO("Not yet implemented")
    }

    actual fun <T> update(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean
    ): Transaction {
        TODO("Not yet implemented")
    }

    actual fun update(
        documentRef: DocumentReference,
        vararg fieldsAndValues: Pair<String, Any?>
    ): Transaction {
        TODO("Not yet implemented")
    }

    @JvmName("updateWithPath")
    actual fun update(
        documentRef: DocumentReference,
        vararg fieldsAndValues: Pair<FieldPath, Any?>
    ): Transaction {
        TODO("Not yet implemented")
    }

    actual fun delete(documentRef: DocumentReference): Transaction {
        TODO("Not yet implemented")
    }

    actual suspend fun get(documentRef: DocumentReference): DocumentSnapshot {
        TODO("Not yet implemented")
    }
}

@Serializable
internal class TransactionOptions(
    val readOnly : ReadOnly? = null,
    val readWrite: ReadWrite? = null
)

@Serializable
internal class ReadOnly(
    val readTime : String? = null
)
@Serializable
internal class ReadWrite(
    val retryTransaction : String? = null
)