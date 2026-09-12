package io.github.mickaelmagniez.windbubble.domain.usecase

import io.github.mickaelmagniez.windbubble.core.util.lerpAngle
import io.github.mickaelmagniez.windbubble.core.util.normalizeDegrees
import io.github.mickaelmagniez.windbubble.domain.model.HeadingSource
import io.github.mickaelmagniez.windbubble.domain.model.Movement
import io.github.mickaelmagniez.windbubble.domain.model.UserPreferences
import io.github.mickaelmagniez.windbubble.domain.model.WindObservation
import io.github.mickaelmagniez.windbubble.domain.model.WindSessionError
import io.github.mickaelmagniez.windbubble.domain.model.WindSessionState
import io.github.mickaelmagniez.windbubble.domain.repository.CompassHeading
import io.github.mickaelmagniez.windbubble.domain.repository.CompassRepository
import io.github.mickaelmagniez.windbubble.domain.repository.LocationRepository
import io.github.mickaelmagniez.windbubble.domain.repository.SettingsRepository
import io.github.mickaelmagniez.windbubble.domain.repository.WindRepository
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive

/**
 * Merges the GPS stream, the polled wind forecast and the user preferences into the single
 * [WindSessionState] consumed by both the dashboard and the floating bubble.
 *
 * The wind is only re-fetched when the user leaves the current ~2 km grid cell or when the
 * refresh interval elapses, which keeps the network (and the battery) cost negligible.
 */
class ObserveRelativeWindUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val windRepository: WindRepository,
    private val settingsRepository: SettingsRepository,
    private val compassRepository: CompassRepository,
    private val calculateRelativeWind: CalculateRelativeWind,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<WindSessionState> = channelFlow {
        if (!locationRepository.hasLocationPermission()) {
            settingsRepository.preferences.collect { preferences ->
                send(
                    WindSessionState(
                        status = WindSessionState.Status.WAITING_FOR_LOCATION,
                        error = WindSessionError.MissingLocationPermission,
                        preferences = preferences,
                    ),
                )
            }
            return@channelFlow
        }

        val movements = locationRepository.movementUpdates()
            .map<Movement, MovementUpdate> { MovementUpdate.Fix(it) }
            .catch { emit(MovementUpdate.Unavailable) }
            .shareIn(scope = this, started = SharingStarted.Eagerly, replay = 1)

        val trackedMovements = movements.scan(TrackedMovement.NONE) { previous, update ->
            trackHeading(previous, update)
        }

        val winds = movements
            .filterIsInstance<MovementUpdate.Fix>()
            .map { it.movement.toGridCell() }
            .distinctUntilChanged()
            .combine(refreshIntervals()) { cell, interval -> cell to interval }
            .flatMapLatest { (cell, interval) -> pollWind(cell, interval) }
            .onStart { emit(WindUpdate.Pending) }

        combine(
            trackedMovements,
            winds,
            settingsRepository.preferences,
            compassHeadings(movements),
        ) { tracked, wind, preferences, compassAzimuth ->
            reduce(tracked, wind, preferences, compassAzimuth)
        }
            .distinctUntilChanged()
            .collect { send(it) }
    }

    /**
     * Magnetic azimuth, collected only while the GPS gives no course: the sensor is registered when
     * the user stops and unregistered as soon as a bearing comes back.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun compassHeadings(movements: Flow<MovementUpdate>): Flow<Float?> {
        if (!compassRepository.isAvailable()) return flowOf(null)
        return movements
            .map { it is MovementUpdate.Fix && it.movement.bearingDegrees != null }
            .onStart { emit(false) }
            .distinctUntilChanged()
            .flatMapLatest { hasCourseOverGround ->
                if (hasCourseOverGround) {
                    flowOf(null)
                } else {
                    compassRepository.headingUpdates()
                        .map<CompassHeading, Float?> { it.magneticAzimuthDegrees }
                        .catch { emit(null) }
                }
            }
            .onStart { emit(null) }
    }

    private fun refreshIntervals(): Flow<Duration> = settingsRepository.preferences
        .map { it.refreshIntervalMinutes.minutes }
        .distinctUntilChanged()

    /** Keeps the last trustworthy heading so the bubble stays readable at traffic lights. */
    private fun trackHeading(previous: TrackedMovement, update: MovementUpdate): TrackedMovement =
        when (update) {
            MovementUpdate.Unavailable -> previous.copy(isUnavailable = true)
            is MovementUpdate.Fix -> {
                val bearing = update.movement.bearingDegrees
                val heading = when {
                    bearing == null -> previous.headingDegrees
                    previous.headingDegrees == null -> bearing
                    else -> lerpAngle(previous.headingDegrees, bearing, HEADING_SMOOTHING)
                }
                TrackedMovement(
                    movement = update.movement,
                    headingDegrees = heading,
                    hasCourseOverGround = bearing != null,
                    isUnavailable = false,
                )
            }
        }

    private fun pollWind(cell: GridCell, interval: Duration): Flow<WindUpdate> = flow {
        var lastKnown: WindObservation? = null
        while (currentCoroutineContext().isActive) {
            val result = runCatching { windRepository.currentWind(cell.latitude, cell.longitude) }
            result.fold(
                onSuccess = {
                    lastKnown = it
                    emit(WindUpdate.Available(it))
                },
                onFailure = { throwable ->
                    emit(
                        WindUpdate.Failed(
                            reason = throwable.message ?: throwable::class.simpleName.orEmpty(),
                            lastKnown = lastKnown,
                        ),
                    )
                },
            )
            delay(if (result.isSuccess) interval else RETRY_INTERVAL)
        }
    }

    private fun reduce(
        tracked: TrackedMovement,
        wind: WindUpdate,
        preferences: UserPreferences,
        compassAzimuthDegrees: Float?,
    ): WindSessionState {
        val observation = wind.observationOrNull()
        val heading = resolveHeading(tracked, compassAzimuthDegrees)
        val relativeWind = if (observation != null && heading != null) {
            calculateRelativeWind(observation, heading.degrees, heading.source)
        } else {
            null
        }
        val status = when {
            relativeWind != null -> WindSessionState.Status.LIVE
            observation != null -> WindSessionState.Status.WAITING_FOR_HEADING
            tracked.movement != null -> WindSessionState.Status.WAITING_FOR_WIND
            else -> WindSessionState.Status.WAITING_FOR_LOCATION
        }
        val error = when {
            tracked.isUnavailable -> WindSessionError.LocationUnavailable
            wind is WindUpdate.Failed -> WindSessionError.WindFetchFailed(wind.reason)
            else -> null
        }
        return WindSessionState(
            status = status,
            relativeWind = relativeWind,
            movement = tracked.movement,
            wind = observation,
            error = error,
            preferences = preferences,
        )
    }

    /**
     * The GPS course wins whenever it exists; the compass only fills the gap, and the last known
     * course is the final fallback on devices without a magnetometer.
     */
    private fun resolveHeading(
        tracked: TrackedMovement,
        compassAzimuthDegrees: Float?,
    ): Heading? {
        val courseOverGround = tracked.headingDegrees
        return when {
            tracked.hasCourseOverGround && courseOverGround != null ->
                Heading(courseOverGround, HeadingSource.MOVEMENT)

            compassAzimuthDegrees != null ->
                Heading(compassAzimuthDegrees.toTrueNorth(tracked.movement), HeadingSource.COMPASS)

            courseOverGround != null -> Heading(courseOverGround, HeadingSource.LAST_KNOWN)
            else -> null
        }
    }

    /** Wind directions are given against true north, compasses read magnetic north. */
    private fun Float.toTrueNorth(movement: Movement?): Float {
        val declination = movement
            ?.let { compassRepository.magneticDeclination(it.latitude, it.longitude) }
            ?: 0f
        return (this + declination).normalizeDegrees()
    }

    private data class Heading(val degrees: Float, val source: HeadingSource)

    private data class TrackedMovement(
        val movement: Movement?,
        val headingDegrees: Float?,
        val hasCourseOverGround: Boolean,
        val isUnavailable: Boolean,
    ) {
        companion object {
            val NONE = TrackedMovement(
                movement = null,
                headingDegrees = null,
                hasCourseOverGround = false,
                isUnavailable = false,
            )
        }
    }

    private sealed interface MovementUpdate {
        data class Fix(val movement: Movement) : MovementUpdate
        data object Unavailable : MovementUpdate
    }

    private sealed interface WindUpdate {
        data object Pending : WindUpdate
        data class Available(val observation: WindObservation) : WindUpdate
        data class Failed(val reason: String, val lastKnown: WindObservation?) : WindUpdate

        fun observationOrNull(): WindObservation? = when (this) {
            Pending -> null
            is Available -> observation
            is Failed -> lastKnown
        }
    }

    /** Coordinates snapped to a ~200 m grid, used to avoid refetching on every GPS tick. */
    private data class GridCell(val latitude: Double, val longitude: Double)

    private fun Movement.toGridCell(): GridCell = GridCell(
        latitude = latitude.snapTo(GRID_DEGREES),
        longitude = longitude.snapTo(GRID_DEGREES),
    )

    private fun Double.snapTo(step: Double): Double = (this / step).roundToInt() * step

    private companion object {
        const val HEADING_SMOOTHING = 0.4f
        const val GRID_DEGREES = 0.002
        val RETRY_INTERVAL = 30.seconds
    }
}
