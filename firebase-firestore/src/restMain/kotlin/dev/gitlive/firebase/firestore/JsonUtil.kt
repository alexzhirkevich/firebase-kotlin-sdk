package dev.gitlive.firebase.firestore

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

internal const val DT_NULL = "nullValue"
internal const val DT_INT = "integerValue"
internal const val DT_STRING = "stringValue"
internal const val DT_DOUBLE = "doubleValue"
internal const val DT_BOOLEAN = "booleanValue"
internal const val DT_ARRAY = "arrayValue"
internal const val DT_MAP = "mapValue"
internal const val DT_TIMESTAMP = "timestampValue"
internal const val TD_GEOPOINT = "geoPointValue"
internal const val TD_REF = "referenceValue"


internal fun JsonElement.decodeFromFirestore() : JsonElement {

    if (this is JsonObject && size == 1) {
        when (keys.first()) {
            DT_NULL ->
                return JsonNull
            DT_STRING, DT_INT, DT_DOUBLE, DT_BOOLEAN ->
                return values.first().jsonPrimitive
            DT_ARRAY ->
                return jsonObject
                    .values.first().jsonObject
                    .values.first().jsonArray.decodeFromFirestore()
            DT_MAP ->
                return jsonObject
                    .values.first().jsonObject
                    .values.first().decodeFromFirestore()
            DT_TIMESTAMP ->
                return values.first().jsonPrimitive
            TD_GEOPOINT ->
                return values.first().jsonObject
            TD_REF ->
                return Json.encodeToJsonElement(
                    DocumentReference(
                        path = jsonObject.values.first().jsonPrimitive.content
                    ),
                )
        }
    }
    return when (this) {
        is JsonNull, is JsonPrimitive -> this
        is JsonArray -> JsonArray(map(JsonElement::decodeFromFirestore))
        is JsonObject -> {
            JsonObject(mapValues {
                it.value.decodeFromFirestore()
            })
        }
    }
}

internal fun JsonElement.encodeToFirestore() : JsonElement {
    return encodeToFirestoreRecursive()
}

private fun JsonElement.encodeToFirestoreRecursive(isFirst : Boolean = true) : JsonElement {
    return when (this) {
        is JsonPrimitive -> primitiveToFirestore()
        is JsonArray -> if (isFirst) JsonArray(map {
            it.encodeToFirestoreRecursive(false)
        }) else arrayToFirestore()
        is JsonObject -> if (isFirst) JsonObject(mapValues {
            it.value.encodeToFirestoreRecursive(false)
        }) else objectToFirestore()
    }
}

internal fun JsonObject.decodeToMap() : Map<String,Any?> {
    return decodeFromFirestore().jsonObject.mapValues { it.value.toKotlinObject() }
}

private fun JsonElement.toKotlinObject() : Any? {
    return when (this) {
        is JsonArray -> map { it.toKotlinObject() }
        is JsonObject -> decodeToNativeOrNull() ?: mapValues {
            it.value.toKotlinObject()
        }
        JsonNull -> null
        is JsonPrimitive -> jsonPrimitive.doubleOrNull
            ?: jsonPrimitive.longOrNull
            ?: jsonPrimitive.booleanOrNull
            ?: content
    }
}

private fun JsonObject.decodeToNativeOrNull() : Any? {
    val serializers = listOf(
        NativeGeoPoint.serializer(),
        NativeTimestamp.serializer(),
        NativeDocumentReference.serializer(),
    )

    return serializers.firstNotNullOfOrNull {
        kotlin.runCatching {
            Json.decodeFromJsonElement(it, this)
        }.getOrNull()
    }
}

private fun JsonPrimitive.primitiveToFirestore() : JsonObject {
    val element = this@primitiveToFirestore

    val key = when {
        element is JsonNull -> DT_NULL
        element.isString -> DT_STRING
        element.doubleOrNull != null -> DT_DOUBLE
        element.longOrNull != null -> DT_INT
        element.booleanOrNull != null -> DT_BOOLEAN
        else -> throw IllegalStateException("Unknown json primitive : $element")
    }
    return JsonObject(mapOf(key to element))
}

private fun JsonArray.arrayToFirestore() = JsonObject(
    mapOf(
        DT_ARRAY to JsonObject(
            mapOf(
                "values" to JsonArray(
                    map { it.encodeToFirestore() }
                )
            )
        )
    )
)

private fun JsonObject.objectToFirestore() : JsonObject {
    val el = decodeToNativeOrNull()

    return if (el == null) {
        JsonObject(
            mapOf(
                DT_MAP to JsonObject(
                    mapOf(
                        "fields" to JsonObject(
                            mapValues {
                                it.value.encodeToFirestore()
                            }
                        )
                    )
                )
            )
        )
    } else {
        when(el){
            is NativeDocumentReference -> JsonObject(
                mapOf(
                    TD_REF to JsonPrimitive(el.path)
                )
            )
            is NativeTimestamp -> JsonObject(
                mapOf(
                    DT_TIMESTAMP to this
                )
            )
            is NativeGeoPoint -> JsonObject(
                mapOf(
                    TD_GEOPOINT to this
                )
            )
            else -> throw IllegalStateException("Unknown native type: $el")
        }
    }
}
