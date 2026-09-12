package io.github.mickaelmagniez.windbubble.data.remote

import io.github.mickaelmagniez.windbubble.domain.model.WindObservation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeatherDto,
)

@Serializable
data class CurrentWeatherDto(
    val time: Long,
    @SerialName("wind_speed_10m") val windSpeedMetersPerSecond: Float,
    @SerialName("wind_direction_10m") val windDirectionDegrees: Float,
    @SerialName("wind_gusts_10m") val windGustsMetersPerSecond: Float? = null,
    @SerialName("temperature_2m") val temperatureCelsius: Float? = null,
)

fun OpenMeteoResponse.toDomain(): WindObservation = WindObservation(
    speedMetersPerSecond = current.windSpeedMetersPerSecond,
    gustMetersPerSecond = current.windGustsMetersPerSecond,
    directionFromDegrees = current.windDirectionDegrees,
    temperatureCelsius = current.temperatureCelsius,
    observedAtEpochSeconds = current.time,
    latitude = latitude,
    longitude = longitude,
)
