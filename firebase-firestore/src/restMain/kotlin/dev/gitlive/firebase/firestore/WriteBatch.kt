package dev.gitlive.firebase.firestore

import kotlinx.serialization.SerializationStrategy
actual class WriteBatch {
    actual inline fun <reified T> set(
        documentRef: DocumentReference,
        data: T,
        encodeDefaults: Boolean,
        merge: Boolean
    ): WriteBatch {
        TODO("Not yet implemented")
    }

    actual inline fun <reified T> set(
        documentRef: DocumentReference,
        data: T,
        encodeDefaults: Boolean,
        vararg mergeFields: String
    ): WriteBatch {
        TODO("Not yet implemented")
    }

    actual inline fun <reified T> set(
        documentRef: DocumentReference,
        data: T,
        encodeDefaults: Boolean,
        vararg mergeFieldPaths: FieldPath
    ): WriteBatch {
        TODO("Not yet implemented")
    }

    actual fun <T> set(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        merge: Boolean
    ): WriteBatch {
        TODO("Not yet implemented")
    }

    actual fun <T> set(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        vararg mergeFields: String
    ): WriteBatch {
        TODO("Not yet implemented")
    }

    actual fun <T> set(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        vararg mergeFieldPaths: FieldPath
    ): WriteBatch {
        TODO("Not yet implemented")
    }

    actual inline fun <reified T> update(
        documentRef: DocumentReference,
        data: T,
        encodeDefaults: Boolean
    ): WriteBatch {
        TODO("Not yet implemented")
    }

    actual fun <T> update(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean
    ): WriteBatch {
        TODO("Not yet implemented")
    }

    actual fun update(
        documentRef: DocumentReference,
        vararg fieldsAndValues: Pair<String, Any?>
    ): WriteBatch {
        TODO("Not yet implemented")
    }

    @JvmName("updateWithPath")
    actual fun update(
        documentRef: DocumentReference,
        vararg fieldsAndValues: Pair<FieldPath, Any?>
    ): WriteBatch {
        TODO("Not yet implemented")
    }

    actual fun delete(documentRef: DocumentReference): WriteBatch {
        TODO("Not yet implemented")
    }

    actual suspend fun commit() {
    }

}