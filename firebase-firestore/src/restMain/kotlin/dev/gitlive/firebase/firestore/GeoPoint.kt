package dev.gitlive.firebase.firestore

import kotlinx.serialization.Serializable

@Serializable
actual class NativeGeoPoint(
    val latitude: Double,
    val longitude: Double
)


/** A class representing a Firebase GeoPoint. */
@Serializable
actual class GeoPoint actual constructor(
    actual val latitude: Double,
    actual val longitude: Double
){

    internal actual val nativeValue: NativeGeoPoint
        get() = NativeGeoPoint(latitude, longitude)

    actual constructor(nativeValue: NativeGeoPoint) : this(
        latitude = nativeValue.latitude,
        longitude = nativeValue.longitude
    )


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GeoPoint

        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        return result
    }

    override fun toString(): String {
        return "GeoPoint(latitude=$latitude, longitude=$longitude)"
    }
}
