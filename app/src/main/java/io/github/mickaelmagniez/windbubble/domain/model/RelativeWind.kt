package io.github.mickaelmagniez.windbubble.domain.model

/** The wind expressed in the frame of reference of the moving user. */
data class RelativeWind(
    /** 0° = straight in your face, 90° = from your right, 180° = pushing you forward. */
    val relativeAngleDegrees: Float,
    /** Positive when slowing you down, negative when helping you. */
    val headwindMetersPerSecond: Float,
    /** Positive when blowing from the right-hand side. */
    val crosswindMetersPerSecond: Float,
    val windSpeedMetersPerSecond: Float,
    val gustMetersPerSecond: Float?,
    val windDirectionFromDegrees: Float,
    val headingDegrees: Float,
    val category: WindCategory,
    val headingSource: HeadingSource,
)
