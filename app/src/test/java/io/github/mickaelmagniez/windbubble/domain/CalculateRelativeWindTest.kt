package io.github.mickaelmagniez.windbubble.domain

import com.google.common.truth.Truth.assertThat
import io.github.mickaelmagniez.windbubble.domain.model.HeadingSource
import io.github.mickaelmagniez.windbubble.domain.model.WindCategory
import io.github.mickaelmagniez.windbubble.domain.model.WindObservation
import io.github.mickaelmagniez.windbubble.domain.usecase.CalculateRelativeWind
import org.junit.Test

class CalculateRelativeWindTest {

    private val calculate = CalculateRelativeWind()

    @Test
    fun `wind blowing from the direction of travel is a headwind`() {
        val relative = calculate(windFrom(90f, speed = 10f), headingDegrees = 90f)

        assertThat(relative.relativeAngleDegrees).isWithin(TOLERANCE).of(0f)
        assertThat(relative.headwindMetersPerSecond).isWithin(TOLERANCE).of(10f)
        assertThat(relative.crosswindMetersPerSecond).isWithin(TOLERANCE).of(0f)
        assertThat(relative.category).isEqualTo(WindCategory.HEADWIND)
    }

    @Test
    fun `wind blowing towards the direction of travel is a tailwind`() {
        val relative = calculate(windFrom(270f, speed = 8f), headingDegrees = 90f)

        assertThat(relative.relativeAngleDegrees).isWithin(TOLERANCE).of(180f)
        assertThat(relative.headwindMetersPerSecond).isWithin(TOLERANCE).of(-8f)
        assertThat(relative.category).isEqualTo(WindCategory.TAILWIND)
    }

    @Test
    fun `wind coming from the right gives a positive crosswind`() {
        val relative = calculate(windFrom(180f, speed = 6f), headingDegrees = 90f)

        assertThat(relative.relativeAngleDegrees).isWithin(TOLERANCE).of(90f)
        assertThat(relative.crosswindMetersPerSecond).isWithin(TOLERANCE).of(6f)
        assertThat(relative.headwindMetersPerSecond).isWithin(TOLERANCE).of(0f)
        assertThat(relative.category).isEqualTo(WindCategory.CROSSWIND)
    }

    @Test
    fun `wind coming from the left gives a negative crosswind`() {
        val relative = calculate(windFrom(0f, speed = 6f), headingDegrees = 90f)

        assertThat(relative.relativeAngleDegrees).isWithin(TOLERANCE).of(270f)
        assertThat(relative.crosswindMetersPerSecond).isWithin(TOLERANCE).of(-6f)
        assertThat(relative.category).isEqualTo(WindCategory.CROSSWIND)
    }

    @Test
    fun `relative angle wraps around north`() {
        val relative = calculate(windFrom(10f, speed = 5f), headingDegrees = 350f)

        assertThat(relative.relativeAngleDegrees).isWithin(TOLERANCE).of(20f)
        assertThat(relative.category).isEqualTo(WindCategory.HEADWIND)
    }

    @Test
    fun `a diagonal wind is split between head and cross components`() {
        val relative = calculate(windFrom(45f, speed = 10f), headingDegrees = 0f)

        assertThat(relative.headwindMetersPerSecond).isWithin(0.01f).of(7.07f)
        assertThat(relative.crosswindMetersPerSecond).isWithin(0.01f).of(7.07f)
        assertThat(relative.category).isEqualTo(WindCategory.HEAD_CROSSWIND)
    }

    @Test
    fun `heading is normalised and its source is carried over`() {
        val relative = calculate(
            windFrom(30f, speed = 4f),
            headingDegrees = 390f,
            headingSource = HeadingSource.COMPASS,
        )

        assertThat(relative.headingDegrees).isWithin(TOLERANCE).of(30f)
        assertThat(relative.headingSource).isEqualTo(HeadingSource.COMPASS)
    }

    private fun windFrom(directionDegrees: Float, speed: Float) = WindObservation(
        speedMetersPerSecond = speed,
        gustMetersPerSecond = null,
        directionFromDegrees = directionDegrees,
        temperatureCelsius = null,
        observedAtEpochSeconds = 0L,
        latitude = 0.0,
        longitude = 0.0,
    )

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
