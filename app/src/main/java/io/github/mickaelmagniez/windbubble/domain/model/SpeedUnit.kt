package io.github.mickaelmagniez.windbubble.domain.model

import kotlin.math.roundToInt

/** Speed units offered to the user; the domain always stores speeds in metres per second. */
enum class SpeedUnit(val label: String, private val factorFromMetersPerSecond: Float) {
    KILOMETERS_PER_HOUR("km/h", 3.6f),
    MILES_PER_HOUR("mph", 2.236936f),
    METERS_PER_SECOND("m/s", 1f),
    KNOTS("kn", 1.943844f);

    fun fromMetersPerSecond(metersPerSecond: Float): Float =
        metersPerSecond * factorFromMetersPerSecond

    /** Rounded value, which is what both the bubble and the dashboard display. */
    fun displayValue(metersPerSecond: Float): Int =
        fromMetersPerSecond(metersPerSecond).roundToInt()

    fun format(metersPerSecond: Float): String = "${displayValue(metersPerSecond)} $label"
}
