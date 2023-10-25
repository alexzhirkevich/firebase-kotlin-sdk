package dev.gitlive.firebase.firestore

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject


actual class QuerySnapshot internal constructor(
    actual val documents: List<DocumentSnapshot>,
    actual val documentChanges: List<DocumentChange>,
    actual val metadata: SnapshotMetadata
)

actual enum class Direction {
    ASCENDING,
    DESCENDING
}

private fun defaultQuery(path : String, firestore : FirebaseFirestore) = StructuredQuery(
    from = listOf(CollectionSelector(
        collectionId = path
            .substringAfter(firestore.documentsPath)
            .drop(1),
        allDescendants = false
    ))
)

actual open class Query private constructor(
    private val path: String,
    private val firestore: FirebaseFirestore,
    private val query : StructuredQuery = defaultQuery(path, firestore)
) {

    constructor(path: String, firestore: FirebaseFirestore) : this(
        path = path,
        firestore = firestore,
        query = defaultQuery(path, firestore)
    )

    actual val snapshots: Flow<QuerySnapshot>
        get() = TODO("Not yet implemented")

    actual fun snapshots(includeMetadataChanges: Boolean): Flow<QuerySnapshot> {
        TODO("Not yet implemented")
    }

    actual fun limit(limit: Number): Query = copy(
        query.copy(limit = limit.toInt())
    )

    actual suspend fun get(): QuerySnapshot =
        firestore.runQuery(path, query)


    internal actual fun _where(
        field: String,
        equalTo: Any?
    ): Query = addFilter(
        Filter(
            fieldFilter = FieldFilter(
                field = FieldReference(field),
                op = FieldOperator.EQUAL,
                value = equalTo.toJsonElement()
            )
        )
    )

    internal actual fun _where(
        path: FieldPath,
        equalTo: Any?
    ): Query {
        TODO("Not yet implemented")
    }

    internal actual fun _where(
        field: String,
        equalTo: DocumentReference
    ): Query = _where(field, equalTo as Any?)

    internal actual fun _where(
        path: FieldPath,
        equalTo: DocumentReference
    ): Query {
        TODO("Not yet implemented")
    }

    internal actual fun _where(
        field: String,
        lessThan: Any?,
        greaterThan: Any?,
        arrayContains: Any?
    ): Query {

        var query = this

        if (lessThan != null && lessThan == greaterThan){
            return query.addFilter(
                Filter(
                    fieldFilter = FieldFilter(
                        field = FieldReference(field),
                        op = FieldOperator.NOT_EQUAL,
                        value = lessThan.toJsonElement()
                    )
                )
            )
        }

        if (lessThan != null) {
            query = query.addFilter(
                Filter(
                    fieldFilter = FieldFilter(
                        field = FieldReference(field),
                        op = FieldOperator.LESS_THAN,
                        value = lessThan.toJsonElement()
                    )
                )
            )
        }

        if (greaterThan != null) {
            query = query.addFilter(
                Filter(
                    fieldFilter = FieldFilter(
                        field = FieldReference(field),
                        op = FieldOperator.GREATER_THAN,
                        value = lessThan.toJsonElement()
                    )
                )
            )
        }

        if (arrayContains != null) {
            query = query.addFilter(
                Filter(
                    fieldFilter = FieldFilter(
                        field = FieldReference(field),
                        op = FieldOperator.ARRAY_CONTAINS,
                        value = lessThan.toJsonElement()
                    )
                )
            )
        }

        return query
    }

    internal actual fun _where(
        path: FieldPath,
        lessThan: Any?,
        greaterThan: Any?,
        arrayContains: Any?
    ): Query {
        TODO("Not yet implemented")
    }

    internal actual fun _where(
        field: String,
        inArray: List<Any>?,
        arrayContainsAny: List<Any>?
    ): Query {
        var query = this

        if (inArray != null) {
            query = query.addFilter(
                Filter(
                    fieldFilter = FieldFilter(
                        field = FieldReference(field),
                        op = FieldOperator.IN,
                        value = inArray.toJsonElement()
                    )
                )
            )
        }

        if (arrayContainsAny != null) {
            query = query.addFilter(
                Filter(
                    fieldFilter = FieldFilter(
                        field = FieldReference(field),
                        op = FieldOperator.ARRAY_CONTAINS_ANY,
                        value = arrayContainsAny.toJsonElement()
                    )
                )
            )
        }

        return query
    }

    internal actual fun _where(
        path: FieldPath,
        inArray: List<Any>?,
        arrayContainsAny: List<Any>?
    ): Query {
        TODO("Not yet implemented")
    }

    internal actual fun _orderBy(
        field: String,
        direction: Direction
    ): Query = copy(
        query = query.copy(
            orderBy = query.orderBy.orEmpty() + Order(
                field = FieldReference(field),
                direction = direction
            )
        )
    )

    internal actual fun _orderBy(
        field: FieldPath,
        direction: Direction
    ): Query {
        TODO("Not yet implemented")
    }

    internal actual fun _startAfter(document: DocumentSnapshot): Query =
        addStartAt(document.reference,before = true)

    internal actual fun _startAfter(vararg fieldValues: Any): Query =
        addStartAt(fieldValues,before = true)

    internal actual fun _startAt(document: DocumentSnapshot): Query =
        addStartAt(document.reference, before = false)

    internal actual fun _startAt(vararg fieldValues: Any): Query =
        addStartAt(fieldValues, before = false)

    internal actual fun _endBefore(document: DocumentSnapshot): Query =
        addStartAt(document.reference, before = true)

    internal actual fun _endBefore(vararg fieldValues: Any): Query =
        addEndAt(fieldValues, before = true)

    internal actual fun _endAt(document: DocumentSnapshot): Query =
        addEndAt(document, before = false)

    internal actual fun _endAt(vararg fieldValues: Any): Query =
        addEndAt(fieldValues,before = true)


    private fun addStartAt(vararg elements : Any?, before : Boolean) =  copy(
        query = query.copy(
            startAt = Cursor(
                values =  query.startAt?.values.orEmpty() + elements.map {it.toJsonElement()},
                before = before
            )
        )
    )

    private fun addEndAt(vararg elements : Any?, before : Boolean) =  copy(
        query = query.copy(
            startAt = Cursor(
                values =  query.endAt?.values.orEmpty() + elements.map {it.toJsonElement()},
                before = before
            )
        )
    )

    private fun copy(query: StructuredQuery) = Query(path, firestore, query)

    private fun addFilter(filter: Filter) = copy(
        query.copy(
            where = when {
                query.where?.compositeFilter != null -> Filter(
                    compositeFilter = CompositeFilter(
                        filters = query.where.compositeFilter.filters + filter
                    )
                )

                query.where?.fieldFilter != null -> Filter(
                    compositeFilter = CompositeFilter(
                        filters = listOf(query.where, filter)
                    )
                )

                else -> filter
            }
        )
    )

}

private fun Any?.toJsonElement()= when (this){
        is Nothing? -> JsonNull
        is Number ->  JsonPrimitive(this)
        is String ->  JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is GeoPoint -> Json.encodeToJsonElement(nativeValue)
        is DocumentReference -> Json.encodeToJsonElement(nativeValue)
        is Timestamp -> Json.encodeToJsonElement(nativeValue)
        else -> throw IllegalStateException("Valued $this cannot participate in query")
    }.encodeToFirestore().jsonObject


@Serializable
internal data class StructuredQuery(
    val select : Projection? = null,// Projection(emptyList()),
    val from : List<CollectionSelector>,
    val where : Filter? = null,
    val orderBy : List<Order>? = null,
    val startAt : Cursor? = null,
    val endAt : Cursor? = null,
    val offset : Int? = null,
    val limit : Int? = null,
)

@Serializable
internal data class Projection(
    val fields : List<FieldReference>
)

@Serializable
internal data class FieldReference(
    val fieldPath : String
)

@Serializable
internal data class CollectionSelector(
    val collectionId : String,
    val allDescendants: Boolean
)

@Serializable
internal data class Filter(
    val compositeFilter : CompositeFilter? = null,
    val fieldFilter : FieldFilter? = null,
    val unaryFilter : UnaryFilter? = null,
)

@Serializable
internal data class CompositeFilter(
    val op : CompositeOperator = CompositeOperator.AND,
    val filters : List<Filter>
)
@Serializable
internal data class FieldFilter(
    val field : FieldReference,
    val op : FieldOperator,
    val value: JsonObject
)
@Serializable
internal data class UnaryFilter(
    val op : UnaryOperator,
    val field : FieldReference
)

internal enum class CompositeOperator {
    AND, OPERATOR_UNSPECIFIED
}

internal enum class FieldOperator {
    OPERATOR_UNSPECIFIED,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    EQUAL,
    NOT_EQUAL,
    ARRAY_CONTAINS,
    IN,
    ARRAY_CONTAINS_ANY,
    NOT_IN
}

internal enum class UnaryOperator {
    OPERATOR_UNSPECIFIED,
    IS_NAN,
    IS_NULL,
    IS_NOT_NAN,
    IS_NOT_NULL
}

@Serializable
internal data class Order(
    val field : FieldReference,
    val direction: Direction
)

@Serializable
internal data class Cursor(
    val values : List<@Contextual Any?>,
    val before : Boolean
)

