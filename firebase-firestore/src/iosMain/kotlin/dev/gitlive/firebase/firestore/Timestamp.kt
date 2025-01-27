package dev.gitlive.firebase.firestore

import cocoapods.FirebaseFirestore.FIRTimestamp
import kotlinx.serialization.Serializable

/** A class representing a platform specific Firebase Timestamp. */
actual typealias NativeTimestamp = FIRTimestamp

/** A base class that could be used to combine [Timestamp] and [Timestamp.ServerTimestamp] in the same field. */
@Serializable(with = BaseTimestampSerializer::class)
actual sealed class BaseTimestamp

/** A class representing a Firebase Timestamp. */
@Serializable(with = TimestampSerializer::class)
actual class Timestamp internal actual constructor(
    internal actual val nativeValue: NativeTimestamp
): BaseTimestamp() {
    actual constructor(seconds: Long, nanoseconds: Int) : this(NativeTimestamp(seconds, nanoseconds))

    actual val seconds: Long = nativeValue.seconds
    actual val nanoseconds: Int = nativeValue.nanoseconds

    override fun equals(other: Any?): Boolean =
        this === other || other is Timestamp && nativeValue == other.nativeValue
    override fun hashCode(): Int = nativeValue.hashCode()
    override fun toString(): String = nativeValue.toString()

    actual companion object {
        actual fun now(): Timestamp = Timestamp(NativeTimestamp.timestamp())
    }

    /** A server time timestamp. */
    @Serializable(with = ServerTimestampSerializer::class)
    actual object ServerTimestamp: BaseTimestamp()
}
