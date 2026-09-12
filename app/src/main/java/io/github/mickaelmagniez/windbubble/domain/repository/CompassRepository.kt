package io.github.mickaelmagniez.windbubble.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Device compass, used as a fallback heading while the user is not moving fast enough for the GPS
 * to report a course.
 */
interface CompassRepository {

    /** False on devices without a magnetometer, where the fallback simply does not apply. */
    fun isAvailable(): Boolean

    /**
     * Azimuth of the phone, in degrees clockwise from **magnetic** north. Collection registers the
     * sensor, so the flow must only be collected while the fallback is actually needed.
     */
    fun headingUpdates(): Flow<CompassHeading>

    /**
     * Angle to add to a magnetic azimuth to obtain a true-north one, for a given position.
     * Wind directions are expressed against true north, so this correction is required.
     */
    fun magneticDeclination(latitude: Double, longitude: Double): Float
}

data class CompassHeading(
    val magneticAzimuthDegrees: Float,
    val accuracy: Accuracy,
) {
    /** Mirrors the sensor accuracy, so the UI can warn when the compass needs calibrating. */
    enum class Accuracy { UNRELIABLE, LOW, MEDIUM, HIGH }
}
