package io.github.mickaelmagniez.windbubble.data

import com.google.common.truth.Truth.assertThat
import io.github.mickaelmagniez.windbubble.data.remote.OpenMeteoResponse
import io.github.mickaelmagniez.windbubble.data.remote.toDomain
import kotlinx.serialization.json.Json
import org.junit.Test

class OpenMeteoResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a real Open-Meteo payload maps to the domain model`() {
        val payload = """
            {
              "latitude": 48.86,
              "longitude": 2.36,
              "generationtime_ms": 0.08,
              "timezone": "GMT",
              "current_units": {"wind_speed_10m": "m/s"},
              "current": {
                "time": 1789137000,
                "interval": 900,
                "wind_speed_10m": 2.67,
                "wind_direction_10m": 283,
                "wind_gusts_10m": 5.6,
                "temperature_2m": 25.6
              }
            }
        """.trimIndent()

        val observation = json.decodeFromString<OpenMeteoResponse>(payload).toDomain()

        assertThat(observation.speedMetersPerSecond).isWithin(0.001f).of(2.67f)
        assertThat(observation.directionFromDegrees).isWithin(0.001f).of(283f)
        assertThat(observation.gustMetersPerSecond).isWithin(0.001f).of(5.6f)
        assertThat(observation.temperatureCelsius).isWithin(0.001f).of(25.6f)
        assertThat(observation.observedAtEpochSeconds).isEqualTo(1789137000L)
        assertThat(observation.latitude).isWithin(0.001).of(48.86)
    }

    @Test
    fun `optional fields may be absent`() {
        val payload = """
            {
              "latitude": 10.0,
              "longitude": 20.0,
              "current": {"time": 1, "wind_speed_10m": 4.0, "wind_direction_10m": 12.5}
            }
        """.trimIndent()

        val observation = json.decodeFromString<OpenMeteoResponse>(payload).toDomain()

        assertThat(observation.gustMetersPerSecond).isNull()
        assertThat(observation.temperatureCelsius).isNull()
    }
}
