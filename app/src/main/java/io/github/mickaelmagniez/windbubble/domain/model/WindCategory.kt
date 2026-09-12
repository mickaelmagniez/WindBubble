package io.github.mickaelmagniez.windbubble.domain.model

import kotlin.math.abs

/** How the wind is experienced relative to the direction of travel. */
enum class WindCategory {
    HEADWIND,
    HEAD_CROSSWIND,
    CROSSWIND,
    TAIL_CROSSWIND,
    TAILWIND;

    companion object {
        /** [relativeAngleDegrees]: 0° means the wind hits you head on, 180° means it pushes you. */
        fun fromRelativeAngle(relativeAngleDegrees: Float): WindCategory {
            val angle = abs(if (relativeAngleDegrees > 180f) relativeAngleDegrees - 360f else relativeAngleDegrees)
            return when {
                angle < 30f -> HEADWIND
                angle < 75f -> HEAD_CROSSWIND
                angle < 105f -> CROSSWIND
                angle < 150f -> TAIL_CROSSWIND
                else -> TAILWIND
            }
        }
    }
}
