package io.github.mickaelmagniez.windbubble.core.util

import kotlin.math.abs

/** Normalizes any angle to the `[0, 360)` range. */
fun Float.normalizeDegrees(): Float = ((this % 360f) + 360f) % 360f

/**
 * Signed difference between two compass angles, in `(-180, 180]`.
 * Positive means [to] is clockwise from [from].
 */
fun angleDelta(from: Float, to: Float): Float {
    val delta = (to - from).normalizeDegrees()
    return if (delta > 180f) delta - 360f else delta
}

/** Converts a compass angle to its 16-point cardinal abbreviation (N, NNE, NE, …). */
fun Float.toCardinalPoint(): String {
    val points = CARDINAL_POINTS
    val index = ((normalizeDegrees() / 22.5f) + 0.5f).toInt() % points.size
    return points[index]
}

/** Smoothly interpolates between two compass angles, taking the shortest path. */
fun lerpAngle(from: Float, to: Float, fraction: Float): Float =
    (from + angleDelta(from, to) * fraction).normalizeDegrees()

/** True when both angles are within [toleranceDegrees] of each other. */
fun anglesClose(first: Float, second: Float, toleranceDegrees: Float): Boolean =
    abs(angleDelta(first, second)) <= toleranceDegrees

private val CARDINAL_POINTS = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
)
