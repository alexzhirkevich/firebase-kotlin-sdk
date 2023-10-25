package dev.gitlive.firebase.firestore

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** A base class that could be used to combine [Timestamp] and [Timestamp.ServerTimestamp] in the same field. */
@Serializable(with = BaseTimestampSerializer::class)
actual sealed class BaseTimestamp

@Serializable
actual class NativeTimestamp(
    val seconds: Long,
    val nanoseconds: Int
)

/** A class representing a Firebase Timestamp. */
@Serializable
actual class Timestamp actual constructor(
    actual val seconds: Long,
    actual val nanoseconds: Int
): BaseTimestamp() {

    internal actual constructor(nativeValue : NativeTimestamp)  : this(
        seconds = nativeValue.seconds,
        nanoseconds = nativeValue.nanoseconds
    )

    internal actual val nativeValue : NativeTimestamp
        get() = NativeTimestamp(
            seconds = seconds,
            nanoseconds = nanoseconds
        )

    actual companion object {
        /** @return a local time timestamp. */
        actual fun now(): Timestamp = TODO()
    }
    /** A server time timestamp. */
    @Serializable(with = ServerTimestampSerializer::class)
    actual object ServerTimestamp: BaseTimestamp()
}

//fun Timestamp.Companion.fromDuration(duration: Duration): Timestamp =
//    duration.toComponents { seconds, nanoseconds ->
//        Timestamp(seconds, nanoseconds)
//    }
//fun Timestamp.toDuration(): Duration = seconds.seconds + nanoseconds.nanoseconds
//
//fun Timestamp.Companion.fromMilliseconds(milliseconds: Double): Timestamp = fromDuration(milliseconds.milliseconds)
//fun Timestamp.toMilliseconds(): Double = toDuration().toDouble(DurationUnit.MILLISECONDS)

///** A serializer for [BaseTimestamp]. Must be used with [FirebaseEncoder]/[FirebaseDecoder]. */
//object BaseTimestampSerializer : KSerializer<BaseTimestamp> by SpecialValueSerializer(
//    serialName = "Timestamp",
//    toNativeValue = { value ->
//        when (value) {
//            Timestamp.ServerTimestamp -> FieldValue.serverTimestamp.nativeValue
//            is Timestamp -> value.nativeValue
//            else -> throw SerializationException("Cannot serialize $value")
//        }
//    },
//    fromNativeValue = { value ->
//        when (value) {
//            FieldValue.serverTimestamp.nativeValue -> Timestamp.ServerTimestamp
//            else -> throw SerializationException("Cannot deserialize $value")
//        }
//    }
//)
//
///** A serializer for [Timestamp]. Must be used with [FirebaseEncoder]/[FirebaseDecoder]. */
//object TimestampSerializer : KSerializer<Timestamp> by SpecialValueSerializer(
//    serialName = "Timestamp",
//    toNativeValue = Timestamp::nativeValue,
//    fromNativeValue = { value ->
//        when (value) {
//            is NativeTimestamp -> Timestamp(value)
//            else -> throw SerializationException("Cannot deserialize $value")
//        }
//    }
//)
//
///** A serializer for [Timestamp.ServerTimestamp]. Must be used with [FirebaseEncoder]/[FirebaseDecoder]. */
//object ServerTimestampSerializer : KSerializer<Timestamp.ServerTimestamp> by SpecialValueSerializer(
//    serialName = "Timestamp",
//    toNativeValue = { FieldValue.serverTimestamp.nativeValue },
//    fromNativeValue = { value ->
//        when (value) {
//            FieldValue.serverTimestamp.nativeValue -> Timestamp.ServerTimestamp
//            else -> throw SerializationException("Cannot deserialize $value")
//        }
//    }
//)
//
///** A serializer for a Double field which is stored as a Timestamp. */
//object DoubleAsTimestampSerializer : KSerializer<Double> by SpecialValueSerializer(
//    serialName = "Timestamp",
//    toNativeValue = { value ->
//        when(value) {
//            serverTimestamp -> FieldValue.serverTimestamp.nativeValue
//            else -> Timestamp.fromMilliseconds(value).nativeValue
//        }
//    },
//    fromNativeValue = { value ->
//        when(value) {
//            FieldValue.serverTimestamp.nativeValue -> serverTimestamp
//            is NativeTimestamp -> Timestamp(value).toMilliseconds()
//            is Double -> value
//            else -> throw SerializationException("Cannot deserialize $value")
//        }
//    }
//) {
//    const val serverTimestamp = Double.POSITIVE_INFINITY
//}