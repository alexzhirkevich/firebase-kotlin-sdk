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


internal class RestFirestoreSerializer2<T : Any>(
    private val default: KSerializer<T>
): JsonTransformingSerializer<T>(default) {

    override fun transformSerialize(element: JsonElement): JsonElement {
        return element.encodeToFirestore()
    }

    override fun transformDeserialize(element: JsonElement): JsonElement {
        return element.decodeFromFirestore()
    }
}

internal fun JsonElement.decodeFromFirestore() : JsonElement {

    if (this is JsonObject && size == 1) {
        when (keys.first()) {
            "nullValue" ->
                return JsonNull
            "stringValue", "integerValue", "doubleValue", "booleanValue" ->
                return values.first().jsonPrimitive
            "arrayValue" ->
                return jsonObject
                    .values.first().jsonObject
                    .values.first().jsonArray.decodeFromFirestore()
            "mapValue" ->
                return jsonObject
                    .values.first().jsonObject
                    .values.first().decodeFromFirestore()
            "timestampValue" ->
                return values.first().jsonPrimitive
            "geoPointValue" ->
                return values.first().jsonObject
            "referenceValue" ->
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
        element is JsonNull -> "nullValue"
        element.isString -> "stringValue"
        element.doubleOrNull != null -> "doubleValue"
        element.longOrNull != null -> "integerValue"
        element.booleanOrNull != null -> "booleanValue"
        else -> throw IllegalStateException("Unknown json primitive : $element")
    }
    return JsonObject(mapOf(key to element))
}

private fun JsonArray.arrayToFirestore() = JsonObject(
    mapOf(
        "arrayValue" to JsonObject(
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
                "mapValue" to JsonObject(
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
                    "referenceValue" to JsonPrimitive(el.path)
                )
            )
            is NativeTimestamp -> JsonObject(
                mapOf(
                    "timestampValue" to this
                )
            )
            is NativeGeoPoint -> JsonObject(
                mapOf(
                    "geoPointValue" to this
                )
            )
            else -> throw IllegalStateException("Unknown native type: $el")
        }
    }
}
