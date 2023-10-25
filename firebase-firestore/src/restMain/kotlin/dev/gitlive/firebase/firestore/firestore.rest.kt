/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.DefaultFirebaseHttpClient
import dev.gitlive.firebase.auth.RefreshTokenManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseValidator
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.cancel
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

internal const val PATH_DELIMITER = '/'

private const val BASE_URL = "https://firestore.googleapis.com/v1beta1"
private const val PARAM_UPDATE_MASK = "updateMask.fieldPaths"

/** Returns the [FirebaseFirestore] instance of the default [FirebaseApp]. */
actual val Firebase.firestore: FirebaseFirestore
    get() = firestore(app)

/** Returns the [FirebaseFirestore] instance of a given [FirebaseApp]. */
actual fun Firebase.firestore(app: FirebaseApp): FirebaseFirestore =
    app.getOrPut(FirebaseFirestore::class){
        FirebaseFirestore(it)
    }

internal val JsonWithDefaults = Json {
    encodeDefaults = false
}

internal val JsonWithoutDefaults = Json {
    encodeDefaults = false
}

actual class FirebaseFirestore(
    private val app : FirebaseApp,
    private val databaseId : String = "(default)",
    private val refreshTokenManager: RefreshTokenManager =
        app.getOrPut(RefreshTokenManager::class) {
            RefreshTokenManager(it)
        },
    private val httpClient: HttpClient = DefaultFirebaseHttpClient(
        app = app,
        refreshTokenManager = refreshTokenManager,
    )
) : FirebaseService {

    internal val documentsPath = "projects/${app.options.projectId}/databases/$databaseId/documents"

    actual fun collection(collectionPath: String): CollectionReference {
        return CollectionReference(
            path = "${documentsPath}$PATH_DELIMITER$collectionPath",
            firestore = this
        )
    }

    actual fun collectionGroup(collectionId: String): Query {
        TODO("Not yet implemented")
    }

    actual fun document(documentPath: String): DocumentReference {
        return DocumentReference(
            path = documentPath,
        ).apply {
            firestore = this@FirebaseFirestore
        }
    }

    actual fun batch(): WriteBatch {
        TODO("Not yet implemented")
    }

    internal suspend fun getDocument(documentReference: DocumentReference) : DocumentSnapshot {
        val resp = performRequest(
            url = documentUrl(documentReference)
        ){
            method = HttpMethod.Get
        }.body<Document>()

        return DocumentSnapshot(
            reference = documentReference,
            id = requireNotNull(resp.name){
                "Received unnamed document"
            }.substringAfterLast(PATH_DELIMITER),
            body = resp.fields.decodeFromFirestore().jsonObject,
            exists = resp.fields.isEmpty()
        )
    }

    internal suspend fun <T> setDocument(
        documentReference: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        merge: Boolean
    ) {
//        val element = encode(
//            strategy = strategy,
//            value = data,
//            shouldEncodeElementDefault = encodeDefaults
//        )

        val json = if (encodeDefaults) JsonWithDefaults else JsonWithoutDefaults

        val encoded = json
            .encodeToJsonElement(strategy, data)
            .encodeToFirestore()


        println(encoded.toString())

        val mergeFields = encoded.takeIf { merge }
            ?.let { it as? Map<*,*>}
            ?.keys
            ?.map { it.toString() }
            .orEmpty()
            .toTypedArray()

        setDocument<T>(
            documentReference = documentReference,
            jsonObject = encoded.jsonObject,
            merge = mergeFields
        )
    }

    internal suspend fun <T> setDocument(
        documentReference: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        merge: Array<out String>
    ) {

        val json = if (encodeDefaults) JsonWithDefaults else JsonWithoutDefaults

        setDocument<T>(
            documentReference = documentReference,
            jsonObject = json
                .encodeToJsonElement(strategy, data)
                .encodeToFirestore()
                .jsonObject,
            merge = merge
        )
    }

    private suspend fun <T> setDocument(
        documentReference: DocumentReference,
        jsonObject: JsonObject,
        merge: Array<out String>
    ) {
        performRequest(documentUrl(documentReference)){
            method = HttpMethod.Patch
            merge.forEach {
                parameter(PARAM_UPDATE_MASK, it)
            }
            setBody(Document(
                name = documentReference.path,
                fields = jsonObject,
            ))
        }
    }

    internal suspend fun update(
        documentReference: DocumentReference,
        fieldsAndValues: Map<String, Any?>
    ) {
        setDocument<JsonElement>(
            documentReference = documentReference,
            jsonObject = JsonWithoutDefaults
                .encodeToJsonElement(fieldsAndValues)
                .encodeToFirestore()
                .jsonObject,
            merge = emptyArray()
        )
    }


    internal suspend fun <T> createDocument(
        collectionReference: CollectionReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean
    ): DocumentReference {

        val json = if (encodeDefaults) JsonWithDefaults else JsonWithoutDefaults

        val doc = json
            .encodeToJsonElement(strategy, data)
            .encodeToFirestore()
            .jsonObject

        val resp = performRequest(documentUrl(collectionReference.document)) {
            method = HttpMethod.Post
            setBody(
                Document(
                    fields = doc,
                )
            )
        }.body<Document>()

        return DocumentReference(
            path = requireNotNull(resp.name) {
                "Received unnamed document"
            }
        ).apply {
            firestore = this@FirebaseFirestore
        }
    }

    internal suspend fun runQuery(
        path: String,
        query: StructuredQuery
    ) : QuerySnapshot {
        val resp = performRequest(documentUrl(DocumentReference(path).parent.document) + ":runQuery") {
            method = HttpMethod.Post
            setBody(
                RunQueryRequest(
                    structuredQuery = query
                )
            )
        }.body<List<RunQueryResponse>>()


        val docs = resp.filter { it.document != null }.map {

            val ref = requireNotNull(it.document!!.name){
                "Received a document withoud name"
            }
            DocumentSnapshot(
                exists = true,
                id = ref.substringAfterLast(PATH_DELIMITER),
                reference = DocumentReference(ref),
                body = it.document.fields
                    .decodeFromFirestore().jsonObject
            )
        }

        return QuerySnapshot(
            documents = docs,
            documentChanges = docs.mapIndexed { index, it ->
                DocumentChange(
                    document = it,
                    newIndex = index,
                    oldIndex = index,
                    type = ChangeType.ADDED
                )
            },
            metadata = SnapshotMetadata(false, false) // TODO
        )
    }

    internal suspend fun deleteDocument(documentReference: DocumentReference) {
        performRequest(documentUrl(documentReference)){
            method = HttpMethod.Delete
        }
    }

    private fun documentUrl(documentReference: DocumentReference) =
        "$BASE_URL$PATH_DELIMITER${documentReference.path}"

    private suspend fun performRequest(
        url: String,
        builder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return httpClient.request(url, builder)
    }

    actual fun setLoggingEnabled(loggingEnabled: Boolean) {

    }

    actual suspend fun clearPersistence() {
    }

    actual suspend fun <T> runTransaction(func: suspend Transaction.() -> T): T {
        TODO("Not yet implemented")
    }

    actual fun useEmulator(host: String, port: Int) {
        TODO("Not yet implemented")
    }

    actual fun setSettings(
        persistenceEnabled: Boolean?,
        sslEnabled: Boolean?,
        host: String?,
        cacheSizeBytes: Long?
    ) {
        TODO("Not yet implemented")
    }

    actual suspend fun disableNetwork() {
        TODO("Not yet implemented")
    }

    actual suspend fun enableNetwork() {
        TODO("Not yet implemented")
    }

    override fun release() {
        httpClient.cancel()
    }
}

@Serializable
private class RunQueryRequest(
    val structuredQuery: StructuredQuery,
    val transaction: String? = null,
    val newTransaction : TransactionOptions? = null,
    val readTime : String? = null
)

@Serializable
private class RunQueryResponse(
    val transaction: String?,
    val document: Document?,
    val readTime: String,
    val skippedResults: Int?,
    val done : Boolean?
)