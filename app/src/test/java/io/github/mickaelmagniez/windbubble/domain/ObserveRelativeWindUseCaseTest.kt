package io.github.mickaelmagniez.windbubble.domain

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.mickaelmagniez.windbubble.domain.model.HeadingSource
import io.github.mickaelmagniez.windbubble.domain.model.Movement
import io.github.mickaelmagniez.windbubble.domain.model.SpeedUnit
import io.github.mickaelmagniez.windbubble.domain.model.UserPreferences
import io.github.mickaelmagniez.windbubble.domain.model.WindObservation
import io.github.mickaelmagniez.windbubble.domain.model.WindSessionState
import io.github.mickaelmagniez.windbubble.domain.repository.CompassHeading
import io.github.mickaelmagniez.windbubble.domain.repository.CompassRepository
import io.github.mickaelmagniez.windbubble.domain.repository.LocationRepository
import io.github.mickaelmagniez.windbubble.domain.repository.SettingsRepository
import io.github.mickaelmagniez.windbubble.domain.repository.WindRepository
import io.github.mickaelmagniez.windbubble.domain.usecase.CalculateRelativeWind
import io.github.mickaelmagniez.windbubble.domain.usecase.ObserveRelativeWindUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveRelativeWindUseCaseTest {

    private val movements = MutableSharedFlow<Movement>(replay = 1)
    private val locationRepository = FakeLocationRepository(movements)
    private val windRepository = FakeWindRepository()
    private val settingsRepository = FakeSettingsRepository()
    private val compassRepository = FakeCompassRepository()

    private val useCase = ObserveRelativeWindUseCase(
        locationRepository = locationRepository,
        windRepository = windRepository,
        settingsRepository = settingsRepository,
        compassRepository = compassRepository,
        calculateRelativeWind = CalculateRelativeWind(),
    )

    @Test
    fun `starts by waiting for a location fix`() = runTest {
        useCase().test {
            assertThat(awaitItem().status).isEqualTo(WindSessionState.Status.WAITING_FOR_LOCATION)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a standing user without a compass gets the wind but no relative angle`() = runTest {
        compassRepository.available = false

        useCase().test {
            skipItems(1)
            movements.emit(movement(bearing = null))

            val state = awaitUntil { it.wind != null }

            assertThat(state.status).isEqualTo(WindSessionState.Status.WAITING_FOR_HEADING)
            assertThat(state.relativeWind).isNull()
            assertThat(state.wind?.speedMetersPerSecond).isEqualTo(10f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the compass provides the heading while standing still`() = runTest {
        // Phone pointing east, and the declination turns the magnetic reading into a true one.
        compassRepository.azimuth = MutableStateFlow(CompassHeading(88f, CompassHeading.Accuracy.HIGH))
        compassRepository.declination = 2f

        useCase().test {
            skipItems(1)
            movements.emit(movement(bearing = null))

            val state = awaitUntil { it.relativeWind != null }
            val relativeWind = state.relativeWind!!

            assertThat(state.status).isEqualTo(WindSessionState.Status.LIVE)
            assertThat(relativeWind.headingSource).isEqualTo(HeadingSource.COMPASS)
            assertThat(relativeWind.headingDegrees).isWithin(0.01f).of(90f)
            // Wind from 90°, heading 90° -> straight headwind.
            assertThat(relativeWind.relativeAngleDegrees).isWithin(0.01f).of(0f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the GPS course wins over the compass as soon as the user moves`() = runTest {
        compassRepository.azimuth = MutableStateFlow(CompassHeading(0f, CompassHeading.Accuracy.HIGH))

        useCase().test {
            skipItems(1)
            movements.emit(movement(bearing = 180f))

            val state = awaitUntil { it.relativeWind?.headingSource == HeadingSource.MOVEMENT }

            assertThat(state.relativeWind?.headingDegrees).isWithin(0.01f).of(180f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `once moving the wind is projected onto the heading`() = runTest {
        useCase().test {
            skipItems(1)
            movements.emit(movement(bearing = 90f))

            val state = awaitUntil { it.relativeWind != null }

            assertThat(state.status).isEqualTo(WindSessionState.Status.LIVE)
            // Wind from 90°, heading 90° -> straight headwind.
            assertThat(state.relativeWind?.relativeAngleDegrees).isWithin(0.01f).of(0f)
            assertThat(state.relativeWind?.headwindMetersPerSecond).isWithin(0.01f).of(10f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a missing permission is reported instead of a silent stall`() = runTest {
        locationRepository.permissionGranted = false

        useCase().test {
            val state = awaitItem()
            assertThat(state.status).isEqualTo(WindSessionState.Status.WAITING_FOR_LOCATION)
            assertThat(state.error).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<WindSessionState>.awaitUntil(
        predicate: (WindSessionState) -> Boolean,
    ): WindSessionState {
        repeat(MAX_EMISSIONS) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
        error("No state matched after $MAX_EMISSIONS emissions")
    }

    private fun movement(bearing: Float?) = Movement(
        latitude = 48.0,
        longitude = 2.0,
        speedMetersPerSecond = if (bearing == null) 0f else 6f,
        bearingDegrees = bearing,
        accuracyMeters = 5f,
        timestampMillis = 0L,
    )

    private class FakeLocationRepository(
        private val movements: Flow<Movement>,
        var permissionGranted: Boolean = true,
    ) : LocationRepository {
        override fun hasLocationPermission() = permissionGranted
        override fun movementUpdates() = movements
    }

    private class FakeWindRepository : WindRepository {
        override suspend fun currentWind(latitude: Double, longitude: Double) = WindObservation(
            speedMetersPerSecond = 10f,
            gustMetersPerSecond = 14f,
            directionFromDegrees = 90f,
            temperatureCelsius = 18f,
            observedAtEpochSeconds = 0L,
            latitude = latitude,
            longitude = longitude,
        )
    }

    private class FakeCompassRepository(
        var available: Boolean = true,
        var azimuth: MutableStateFlow<CompassHeading> =
            MutableStateFlow(CompassHeading(0f, CompassHeading.Accuracy.HIGH)),
        var declination: Float = 0f,
    ) : CompassRepository {
        override fun isAvailable() = available
        override fun headingUpdates(): Flow<CompassHeading> = azimuth
        override fun magneticDeclination(latitude: Double, longitude: Double) = declination
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val state = MutableStateFlow(UserPreferences())
        override val preferences: Flow<UserPreferences> = state

        override suspend fun setSpeedUnit(unit: SpeedUnit) {
            state.value = state.value.copy(speedUnit = unit)
        }

        override suspend fun setRefreshIntervalMinutes(minutes: Int) {
            state.value = state.value.copy(refreshIntervalMinutes = minutes)
        }

        override suspend fun setBubbleOpacity(opacity: Float) = Unit
        override suspend fun setBubbleScale(scale: Float) = Unit
        override suspend fun setShowGusts(show: Boolean) = Unit
        override suspend fun setBubblePosition(x: Int, y: Int) = Unit
        override suspend fun resetBubblePosition() = Unit
    }

    private companion object {
        const val MAX_EMISSIONS = 10
    }
}
