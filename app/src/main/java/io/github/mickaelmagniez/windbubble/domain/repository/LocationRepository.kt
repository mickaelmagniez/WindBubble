package io.github.mickaelmagniez.windbubble.domain.repository

import io.github.mickaelmagniez.windbubble.domain.model.Movement
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    /** True when [movementUpdates] can actually emit. */
    fun hasLocationPermission(): Boolean

    /**
     * Cold stream of GPS fixes. Collection stops the underlying location request, so the flow is
     * safe to leave running only while something observes it.
     */
    fun movementUpdates(): Flow<Movement>
}
