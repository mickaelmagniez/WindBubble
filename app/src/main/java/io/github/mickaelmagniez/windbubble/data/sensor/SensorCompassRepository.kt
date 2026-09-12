package io.github.mickaelmagniez.windbubble.data.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import io.github.mickaelmagniez.windbubble.core.util.angleDelta
import io.github.mickaelmagniez.windbubble.core.util.lerpAngle
import io.github.mickaelmagniez.windbubble.core.util.normalizeDegrees
import io.github.mickaelmagniez.windbubble.domain.repository.CompassHeading
import io.github.mickaelmagniez.windbubble.domain.repository.CompassRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Compass built on the fused rotation vector, which already blends the magnetometer with the
 * gyroscope and is far steadier than raw magnetic readings.
 */
@Singleton
class SensorCompassRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CompassRepository {

    private val sensorManager = context.getSystemService<SensorManager>()

    @Volatile
    private var cachedDeclination: Pair<DeclinationCell, Float>? = null

    override fun isAvailable(): Boolean =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

    override fun headingUpdates(): Flow<CompassHeading> = callbackFlow {
        val manager = sensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || sensor == null) {
            close(IllegalStateException("This device has no compass"))
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(MATRIX_SIZE)
        val remappedMatrix = FloatArray(MATRIX_SIZE)
        val orientation = FloatArray(3)
        var smoothedAzimuth: Float? = null
        var accuracy = CompassHeading.Accuracy.MEDIUM

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)

                // Held upright (phone facing you), "forward" is where the back camera looks; lying
                // flat on a handlebar mount, it is the top edge of the screen.
                val pitchDegrees = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val matrix = if (abs(pitchDegrees) > UPRIGHT_PITCH_THRESHOLD) {
                    SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remappedMatrix,
                    )
                    remappedMatrix
                } else {
                    rotationMatrix
                }
                SensorManager.getOrientation(matrix, orientation)

                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat().normalizeDegrees()
                val previous = smoothedAzimuth
                val smoothed = if (previous == null) {
                    azimuth
                } else {
                    lerpAngle(previous, azimuth, SMOOTHING)
                }
                // Magnetometers are noisy: only publish once the needle really moved.
                if (previous == null || abs(angleDelta(previous, smoothed)) >= MIN_CHANGE_DEGREES) {
                    smoothedAzimuth = smoothed
                    trySend(CompassHeading(smoothed, accuracy))
                } else {
                    smoothedAzimuth = smoothed
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, newAccuracy: Int) {
                accuracy = newAccuracy.toAccuracy()
                smoothedAzimuth?.let { trySend(CompassHeading(it, accuracy)) }
            }
        }

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { manager.unregisterListener(listener) }
    }.conflate()

    /** Declination varies over hundreds of kilometres, so one value per ~50 km cell is plenty. */
    override fun magneticDeclination(latitude: Double, longitude: Double): Float {
        val cell = DeclinationCell((latitude / CELL_DEGREES).toInt(), (longitude / CELL_DEGREES).toInt())
        cachedDeclination?.takeIf { it.first == cell }?.let { return it.second }
        val declination = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            0f,
            System.currentTimeMillis(),
        ).declination
        cachedDeclination = cell to declination
        return declination
    }

    private data class DeclinationCell(val latitude: Int, val longitude: Int)

    private fun Int.toAccuracy(): CompassHeading.Accuracy = when (this) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassHeading.Accuracy.HIGH
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassHeading.Accuracy.MEDIUM
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassHeading.Accuracy.LOW
        else -> CompassHeading.Accuracy.UNRELIABLE
    }

    private companion object {
        const val MATRIX_SIZE = 9
        const val SMOOTHING = 0.2f
        const val MIN_CHANGE_DEGREES = 1f

        /** Beyond this pitch the phone is considered held upright rather than lying flat. */
        const val UPRIGHT_PITCH_THRESHOLD = 45f
        const val CELL_DEGREES = 0.5
    }
}
