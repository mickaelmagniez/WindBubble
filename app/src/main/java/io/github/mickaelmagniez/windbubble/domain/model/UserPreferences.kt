package io.github.mickaelmagniez.windbubble.domain.model

/** Everything the user can tune, persisted in DataStore. */
data class UserPreferences(
    val speedUnit: SpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
    val refreshIntervalMinutes: Int = DEFAULT_REFRESH_INTERVAL_MINUTES,
    val bubbleOpacity: Float = DEFAULT_BUBBLE_OPACITY,
    val bubbleScale: Float = DEFAULT_BUBBLE_SCALE,
    val showGusts: Boolean = true,
    val bubblePositionX: Int = DEFAULT_BUBBLE_POSITION,
    val bubblePositionY: Int = DEFAULT_BUBBLE_POSITION,
) {
    companion object {
        const val DEFAULT_REFRESH_INTERVAL_MINUTES = 5
        const val MIN_REFRESH_INTERVAL_MINUTES = 1
        const val MAX_REFRESH_INTERVAL_MINUTES = 30
        const val DEFAULT_BUBBLE_OPACITY = 0.9f
        const val DEFAULT_BUBBLE_SCALE = 1f
        const val MIN_BUBBLE_SCALE = 0.7f
        const val MAX_BUBBLE_SCALE = 1.5f
        /** Sentinel meaning "never moved by the user", so the bubble falls back to a default spot. */
        const val DEFAULT_BUBBLE_POSITION = -1
    }
}
