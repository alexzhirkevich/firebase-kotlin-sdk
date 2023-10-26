package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.auth.DefaultFirebaseJson
import dev.gitlive.firebase.decode
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

actual class SnapshotMetadata(
    actual val hasPendingWrites: Boolean,
    actual val isFromCache: Boolean
)

actual class DocumentSnapshot(
    actual val exists: Boolean,
    actual val id: String,
    actual val reference: DocumentReference,
    private val body : JsonObject,
    actual val metadata: SnapshotMetadata = SnapshotMetadata(false,false)
)  {

    actual fun <T> get(
        field: String,
        strategy: DeserializationStrategy<T>,
        serverTimestampBehavior: ServerTimestampBehavior
    ): T =  JsonWithoutDefaults.decodeFromJsonElement(
        element = requireNotNull(body[field]){
            "Key '$field' is not present in object ${reference.path}"
        },
        deserializer = strategy
    )

    actual fun contains(field: String): Boolean = body.contains(field) == true

    actual fun <T> data(
        strategy: DeserializationStrategy<T>,
        serverTimestampBehavior: ServerTimestampBehavior
    ): T = JsonWithoutDefaults.decodeFromJsonElement(strategy, body)

    actual inline fun <reified T> get(
        field: String,
        serverTimestampBehavior: ServerTimestampBehavior
    ): T = get(
            field = field,
            strategy = serializer<T>(),
            serverTimestampBehavior = serverTimestampBehavior
        )

    actual inline fun <reified T : Any> data(serverTimestampBehavior: ServerTimestampBehavior): T =
        data(
            strategy = serializer<T>(),
            serverTimestampBehavior = serverTimestampBehavior
        )
}