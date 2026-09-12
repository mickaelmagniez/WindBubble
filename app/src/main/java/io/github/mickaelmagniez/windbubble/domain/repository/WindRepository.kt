package io.github.mickaelmagniez.windbubble.domain.repository

import io.github.mickaelmagniez.windbubble.domain.model.WindObservation

interface WindRepository {
    /** Fetches the current wind for a coordinate, throwing on network or parsing failures. */
    suspend fun currentWind(latitude: Double, longitude: Double): WindObservation
}
