package io.github.mickaelmagniez.windbubble.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import io.github.mickaelmagniez.windbubble.domain.model.Movement
import io.github.mickaelmagniez.windbubble.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Location source built on the platform [LocationManager] only: no Google Play Services, which
 * keeps the app free of proprietary dependencies (and therefore publishable on F-Droid).
 */
@Singleton
class SystemLocationRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocationRepository {

    private val locationManager = context.getSystemService<LocationManager>()

    override fun hasLocationPermission(): Boolean = LOCATION_PERMISSIONS.any { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override fun movementUpdates(): Flow<Movement> = callbackFlow {
        val manager = locationManager
        if (manager == null) {
            close(IllegalStateException("No location manager on this device"))
            return@callbackFlow
        }
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission is not granted"))
            return@callbackFlow
        }

        // A recent last-known fix lets the dashboard show the wind straight away, without its
        // bearing which would be stale by definition.
        manager.lastKnownFix()?.let { trySend(it.toMovement(trustBearing = false)) }

        val listener = object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
                trySend(location.toMovement())
            }
        }
        val request = LocationRequestCompat.Builder(UPDATE_INTERVAL_MILLIS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MILLIS)
            .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_METERS)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .build()

        runCatching {
            LocationManagerCompat.requestLocationUpdates(
                manager,
                manager.selectProvider(),
                request,
                ContextCompat.getMainExecutor(context),
                listener,
            )
        }.onFailure { close(it) }

        awaitClose { LocationManagerCompat.removeUpdates(manager, listener) }
    }.conflate()

    /** The fused provider is part of AOSP since Android 12 and beats raw GPS when available. */
    private fun LocationManager.selectProvider(): String {
        val providers = allProviders
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                providers.contains(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER

            providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> LocationManager.NETWORK_PROVIDER
        }
    }

    @SuppressLint("MissingPermission")
    private fun LocationManager.lastKnownFix(): Location? = allProviders
        .mapNotNull { provider -> runCatching { getLastKnownLocation(provider) }.getOrNull() }
        .filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos < MAX_FIX_AGE_NANOS }
        .maxByOrNull { it.elapsedRealtimeNanos }

    private fun Location.toMovement(trustBearing: Boolean = true): Movement {
        val usableBearing = trustBearing && hasBearing() && hasSpeed() && speed >= MIN_SPEED_FOR_BEARING
        return Movement(
            latitude = latitude,
            longitude = longitude,
            speedMetersPerSecond = if (hasSpeed()) speed else 0f,
            bearingDegrees = if (usableBearing) bearing else null,
            accuracyMeters = if (hasAccuracy()) accuracy else Float.NaN,
            timestampMillis = time,
        )
    }

    private companion object {
        val LOCATION_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        const val UPDATE_INTERVAL_MILLIS = 3_000L
        const val FASTEST_INTERVAL_MILLIS = 1_000L
        const val MIN_UPDATE_DISTANCE_METERS = 5f

        /** Below ~3.6 km/h the GPS bearing is mostly noise, so we ignore it. */
        const val MIN_SPEED_FOR_BEARING = 1f

        val MAX_FIX_AGE_NANOS = 10.minutes.inWholeNanoseconds
    }
}
