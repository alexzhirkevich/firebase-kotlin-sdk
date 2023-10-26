package dev.gitlive.firebase.firestore

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonObject

internal interface FirestoreDataSource {

    suspend fun <T> createDocument(
        collectionReference: CollectionReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean
    ): DocumentReference

    suspend fun getDocument(
        documentReference: DocumentReference
    ) : DocumentSnapshot

    suspend fun <T> setDocument(
        documentReference: DocumentReference,
        jsonObject: JsonObject,
        merge: Array<out String>
    )

    suspend fun <T> setDocument(
        documentReference: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        merge: Array<out String>
    )

    suspend fun <T> setDocument(
        documentReference: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        merge: Boolean
    )

    suspend fun runQuery(
        path: String,
        query: StructuredQuery
    ) : QuerySnapshot

    suspend fun deleteDocument(documentReference: DocumentReference)
}