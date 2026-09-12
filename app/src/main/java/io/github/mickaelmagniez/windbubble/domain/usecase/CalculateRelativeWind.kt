package io.github.mickaelmagniez.windbubble.domain.usecase

import io.github.mickaelmagniez.windbubble.core.util.normalizeDegrees
import io.github.mickaelmagniez.windbubble.domain.model.HeadingSource
import io.github.mickaelmagniez.windbubble.domain.model.RelativeWind
import io.github.mickaelmagniez.windbubble.domain.model.WindCategory
import io.github.mickaelmagniez.windbubble.domain.model.WindObservation
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

/**
 * Projects a meteorological wind reading onto the user's direction of travel.
 *
 * Both the wind direction and the heading are compass angles, so the relative angle is simply
 * their difference: when the wind comes *from* the exact direction you are heading to, you get 0°,
 * a pure headwind.
 */
class CalculateRelativeWind @Inject constructor() {

    operator fun invoke(
        wind: WindObservation,
        headingDegrees: Float,
        headingSource: HeadingSource = HeadingSource.MOVEMENT,
    ): RelativeWind {
        val relativeAngle = (wind.directionFromDegrees - headingDegrees).normalizeDegrees()
        val radians = Math.toRadians(relativeAngle.toDouble())
        return RelativeWind(
            relativeAngleDegrees = relativeAngle,
            headwindMetersPerSecond = (wind.speedMetersPerSecond * cos(radians)).toFloat(),
            crosswindMetersPerSecond = (wind.speedMetersPerSecond * sin(radians)).toFloat(),
            windSpeedMetersPerSecond = wind.speedMetersPerSecond,
            gustMetersPerSecond = wind.gustMetersPerSecond,
            windDirectionFromDegrees = wind.directionFromDegrees,
            headingDegrees = headingDegrees.normalizeDegrees(),
            category = WindCategory.fromRelativeAngle(relativeAngle),
            headingSource = headingSource,
        )
    }
}
