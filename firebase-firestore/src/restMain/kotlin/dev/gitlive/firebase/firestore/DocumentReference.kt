package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.Transient
import kotlinx.serialization.serializer

@Serializable
actual class NativeDocumentReference(
    val path: String
)

@Serializable
actual class DocumentReference internal constructor(
    actual val path: String,
) {

    @Transient
    actual val id: String = path.substringAfterLast(PATH_DELIMITER)

    internal actual val nativeValue: NativeDocumentReference
        get() = NativeDocumentReference(path)

    actual val snapshots: Flow<DocumentSnapshot>
        get() = TODO()

    actual val parent: CollectionReference
        get() = CollectionReference(
            path = path.substringBeforeLast(PATH_DELIMITER),
            firestore = _firestore
        )

    @Transient
    internal var firestore: FirebaseFirestore? = null

    private val _firestore by lazy {
        firestore ?: Firebase.firestore
    }

    internal actual constructor(nativeValue: NativeDocumentReference) : this(nativeValue.path)

    override fun equals(other: Any?): Boolean {
        return path == (other as? DocumentReference)?.path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    override fun toString(): String {
        return "DocumentReference(path=$path)"
    }

    actual fun snapshots(includeMetadataChanges: Boolean): Flow<DocumentSnapshot> {
        TODO("Not yet implemented")
    }

    actual fun collection(collectionPath: String): CollectionReference {
        return CollectionReference(
            path = "$path${PATH_DELIMITER}collectionPath",
            firestore = _firestore
        )
    }

    actual suspend fun get(): DocumentSnapshot {
        return _firestore.getDocument(this)
    }

    actual suspend fun <T> set(
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        merge: Boolean
    ) {
        _firestore.setDocument(
            documentReference = this,
            strategy = strategy,
            data = data,
            encodeDefaults = encodeDefaults,
            merge = merge
        )
    }

    actual suspend fun <T> set(
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        vararg mergeFields: String
    ) {
        _firestore.setDocument(
            documentReference = this,
            strategy = strategy,
            data = data,
            encodeDefaults = encodeDefaults,
            merge = mergeFields
        )
    }

    actual suspend inline fun <reified T> set(
        data: T,
        encodeDefaults: Boolean,
        merge: Boolean
    ) {
        set(
            strategy = serializer<T>(),
            data = data,
            encodeDefaults = encodeDefaults,
            merge = merge
        )
    }

    actual suspend inline fun <reified T> set(
        data: T,
        encodeDefaults: Boolean,
        vararg mergeFields: String
    ) {
        set(
            strategy = serializer<T>(),
            data = data,
            encodeDefaults = encodeDefaults,
            mergeFields = mergeFields
        )
    }

    actual suspend fun <T> update(
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean
    ) {
        set(strategy, data, encodeDefaults, merge = true)
    }

    actual suspend inline fun <reified T> update(data: T, encodeDefaults: Boolean) {
        update(serializer<T>(), data, encodeDefaults)
    }

    actual suspend fun update(vararg fieldsAndValues: Pair<String, Any?>) {
        _firestore.update(this, fieldsAndValues.toMap())
    }

    @JvmName("updateWithPath")
    actual suspend fun update(vararg fieldsAndValues: Pair<FieldPath, Any?>) {
//        set(fieldsAndValues, merge = true)
        TODO()
    }

    actual suspend fun delete() {
        _firestore.deleteDocument(this)
    }

    actual suspend inline fun <reified T> set(
        data: T,
        encodeDefaults: Boolean,
        vararg mergeFieldPaths: FieldPath
    ) {
        TODO()
    }

    actual suspend fun <T> set(
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        vararg mergeFieldPaths: FieldPath
    ) {
        TODO()
    }
}