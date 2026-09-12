package io.github.mickaelmagniez.windbubble.data.wind

import android.os.SystemClock
import io.github.mickaelmagniez.windbubble.core.di.IoDispatcher
import io.github.mickaelmagniez.windbubble.data.remote.OpenMeteoApi
import io.github.mickaelmagniez.windbubble.data.remote.toDomain
import io.github.mickaelmagniez.windbubble.domain.model.WindObservation
import io.github.mickaelmagniez.windbubble.domain.repository.WindRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class OpenMeteoWindRepository @Inject constructor(
    private val api: OpenMeteoApi,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WindRepository {

    private val mutex = Mutex()
    private var lastFetch: CachedObservation? = null

    /**
     * Open-Meteo publishes on a kilometre-scale grid and refreshes every 15 minutes, while a moving
     * user can cross several 200 m cells per minute. This guard keeps the app responsive without
     * turning GPS noise into a burst of identical requests.
     */
    override suspend fun currentWind(latitude: Double, longitude: Double): WindObservation =
        withContext(ioDispatcher) {
            mutex.withLock {
                val now = SystemClock.elapsedRealtime()
                lastFetch
                    ?.takeIf { now - it.fetchedAtMillis < MIN_FETCH_INTERVAL_MILLIS }
                    ?.let { return@withLock it.observation }

                api.currentWeather(latitude = latitude, longitude = longitude)
                    .toDomain()
                    .also { lastFetch = CachedObservation(it, now) }
            }
        }

    private data class CachedObservation(
        val observation: WindObservation,
        val fetchedAtMillis: Long,
    )

    private companion object {
        const val MIN_FETCH_INTERVAL_MILLIS = 20_000L
    }
}
