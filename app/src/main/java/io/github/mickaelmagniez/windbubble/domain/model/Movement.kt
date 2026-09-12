package io.github.mickaelmagniez.windbubble.domain.model

/** A GPS fix enriched with the travel direction derived from it. */
data class Movement(
    val latitude: Double,
    val longitude: Double,
    val speedMetersPerSecond: Float,
    /** Course over ground, `null` while the user is too slow for the bearing to be meaningful. */
    val bearingDegrees: Float?,
    val accuracyMeters: Float,
    val timestampMillis: Long,
)
