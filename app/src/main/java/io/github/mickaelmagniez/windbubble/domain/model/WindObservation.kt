package io.github.mickaelmagniez.windbubble.domain.model

/**
 * A weather-station style wind reading.
 *
 * [directionFromDegrees] follows the meteorological convention: it is the direction the wind
 * blows *from*, clockwise from true north (270° means a westerly wind).
 */
data class WindObservation(
    val speedMetersPerSecond: Float,
    val gustMetersPerSecond: Float?,
    val directionFromDegrees: Float,
    val temperatureCelsius: Float?,
    val observedAtEpochSeconds: Long,
    val latitude: Double,
    val longitude: Double,
)
