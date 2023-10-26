package dev.gitlive.firebase.firestore

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

internal class FirestorePersistentDataSource(
    private val queries : CacheQueries,
    private val sqlDriver: SqlDriver,
    private val coroutineContext : CoroutineContext = Dispatchers.IO
) : FirestoreDataSource {

    override suspend fun <T> createDocument(
        collectionReference: CollectionReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean
    ): DocumentReference = withContext(coroutineContext) {
        createDocument(
            id = AutoId.next(),
            collectionReference = collectionReference,
            strategy = strategy,
            data = data,
            encodeDefaults = encodeDefaults
        )
    }

    override suspend fun getDocument(documentReference: DocumentReference): DocumentSnapshot {
        return withContext(coroutineContext) {
            val doc = getDocumentInfo(documentReference.path)
                ?: return@withContext DocumentSnapshot(
                    exists = false,
                    id = "",
                    reference = documentReference,
                    body = JsonObject(emptyMap()),
                    metadata = SnapshotMetadata(
                        hasPendingWrites = false,
                        isFromCache = true
                    )
                )

            val fields = queries.getDocumentFields(path = documentReference.path).executeAsList()

            val json = fields.map { it.key to it.encodedFieldValue }.joinToString(
                prefix = "{",
                postfix = "}",
                separator = ","
            ) {
                "\"${it.first}\":${it.second}"
            }

            val obj = Json.decodeFromString<JsonObject>(json).decodeFromFirestore()

            return@withContext DocumentSnapshot(
                exists = fields.isNotEmpty(),
                id = documentReference.id,
                reference = documentReference,
                body = obj.jsonObject,
                metadata = SnapshotMetadata(
                    hasPendingWrites = doc.hasPendingWrites,
                    isFromCache = true
                )
            )
        }
    }

    override suspend fun <T> setDocument(
        documentReference: DocumentReference,
        jsonObject: JsonObject,
        merge: Array<out String>
    ) {
        TODO()
    }

    override suspend fun <T> setDocument(
        documentReference: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        merge: Array<out String>
    ) {
        withContext(coroutineContext) {
            val doc = getDocumentInfo(path = documentReference.path)

            if (doc == null) {
                createDocument(
                    id = documentReference.id,
                    collectionReference = documentReference.parent,
                    strategy = strategy,
                    data = data,
                    encodeDefaults = encodeDefaults
                )
                return@withContext
            }

            val json = if (encodeDefaults) JsonWithDefaults else JsonWithoutDefaults

            if (merge.isEmpty()) {
                queries.deleteDocumentFields(documentReference.path)
            }

            val encodedJson = json
                .encodeToJsonElement(strategy, data)
                .let {
                    if (merge.isEmpty()) {
                        it
                    } else {
                        JsonObject(
                            content = it.jsonObject
                                .filterKeys { it in merge }
                        )

                    }
                }.encodeToFirestore()


            encodedJson.jsonObject.forEach { (k, v) ->
                queries.setField(
                    idDocument = documentReference.id,
                    key = k,
                    dataType = v.jsonObject.keys.first(),
                    fieldValue = v.decodeFromFirestore().toString(),
                    encodedFieldValue = v.toString(),
                )
            }

            val now = Clock.System.now().epochSeconds

            queries.setDocumentUpdated(
                at = now,
                path = documentReference.path
            )
        }
    }

    override suspend fun <T> setDocument(
        documentReference: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        merge: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun runQuery(path: String, query: StructuredQuery): QuerySnapshot {

        val Field = CacheDocumentField::class.simpleName!!
        val Document = CacheDocument::class.simpleName!!
        val IdDocument = CacheDocumentField::idDocument.name
        val Key = CacheDocumentField::key.name
        val Id = CacheDocument::id.name
        val ParentPath = CacheDocument::parentPath.name

        val whereKeysIn = query.select?.fields?.joinToString(",") { "'$it'" }?.let {
            " AND (f.$Key IN ($it))"
        } ?: ""

        val where = if (query.where != null) {
            buildList<String> {
                query.where.unaryFilter?.let {
                    val operator = when (it.op) {
                        UnaryOperator.IS_NULL -> "IS NULL"
                        UnaryOperator.IS_NOT_NULL -> "IS NOT NULL"
                        else -> return@let
                    }
                    add("f.$Field != $it OR f.$Field $operator")
                    UnaryOperator.IS_NULL
                }
            }.filter { it.isNotEmpty() }.joinToString(separator = "AND") { "($it)" }
        }
        else null

        withContext(coroutineContext) {
            val fields = sqlDriver.executeQuery(
                identifier = Random.nextInt(),
                mapper = {
                    QueryResult.Value(
                        buildList {
                            while (it.next().value) {
                                add(
                                    CacheDocumentField(
                                        id = it.getLong(0)!!,
                                        idDocument = it.getString(1)!!,
                                        key = it.getString(2)!!,
                                        dataType = it.getString(3)!!,
                                        fieldValue = it.getString(4),
                                        encodedFieldValue = it.getString(5)
                                    )
                                )
                            }
                        }
                    )
                },
                sql = """SELECT * FROM $Field f WHERE (f.$IdDocument in (SELECT d.$Id from $Document d WHERE d.$ParentPath = ?)) $whereKeysIn""",
                parameters = 1,
                binders = {
                    bindString(0, path)
                }
            ).await()

        }
        TODO()
    }

    override suspend fun deleteDocument(documentReference: DocumentReference) {
        withContext(coroutineContext){
            queries.deleteDocumentAndFields(documentReference.path)
        }
    }

    private fun getDocumentInfo(path: String) : CacheDocument? {
        return queries.getDocument(path = path).executeAsOneOrNull()
    }

    private fun <T> createDocument(
        id : String,
        collectionReference: CollectionReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean
    ): DocumentReference {

        val doc = collectionReference.document(id).apply {
            firestore = collectionReference.firestore
        }

        kotlin.runCatching {

            val now = Clock.System.now().epochSeconds

            queries.createDocument(
                id = id,
                path = doc.path,
                parentPath = collectionReference.path,
                createdAt = now
            )

            val json = if (encodeDefaults) JsonWithDefaults else JsonWithoutDefaults

            val encodedJson = json
                .encodeToJsonElement(strategy, data)
                .encodeToFirestore()

            encodedJson.jsonObject.forEach { (k, v) ->
                queries.setField(
                    idDocument = id,
                    key = k,
                    dataType = v.jsonObject.keys.first(),
                    fieldValue = v.decodeFromFirestore().toString(),
                    encodedFieldValue = v.toString(),
                )
            }
        }

        return doc
    }

}
