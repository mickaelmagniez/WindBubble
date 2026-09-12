package io.github.mickaelmagniez.windbubble.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo forecast API: free for non-commercial use and, unlike most weather APIs, it needs
 * no key at all. See https://open-meteo.com/en/docs
 */
interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun currentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("wind_speed_unit") windSpeedUnit: String = "ms",
        @Query("timeformat") timeFormat: String = "unixtime",
    ): OpenMeteoResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
        private const val CURRENT_FIELDS =
            "wind_speed_10m,wind_direction_10m,wind_gusts_10m,temperature_2m"
    }
}
