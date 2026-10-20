package app.beingmuslim.android.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** How far the platform trusts the compass right now. */
enum class CompassAccuracy { UNRELIABLE, LOW, MEDIUM, HIGH }

/**
 * A live compass reading: the direction the top of the phone faces, corrected to
 * true north.
 *
 * @param trueHeadingDegrees degrees clockwise from true north (`0.0..<360.0`).
 */
data class CompassReading(
    val trueHeadingDegrees: Double,
    val accuracy: CompassAccuracy,
)

/**
 * The platform compass: the rotation-vector sensor, with magnetic declination
 * applied so readings are true-north relative (issue #10).
 *
 * This is a thin adapter. It holds no policy: the screen decides what to show
 * for each [CompassAccuracy], and the bearing-only direction is always available
 * without it (`docs/PRODUCT_REQUIREMENTS.md` §6).
 *
 * Readings are collected only while the screen is started, and the listener is
 * removed when collection stops.
 */
class CompassSensor(context: Context) {

    private val sensorManager: SensorManager? =
        context.applicationContext.getSystemService(SensorManager::class.java)

    private val rotationSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

    /** Whether this device can provide a live heading at all. */
    fun isAvailable(): Boolean = sensorManager != null && rotationSensor != null

    /**
     * Readings for a device at [latitude]/[longitude]. The position is used only
     * to look up the local magnetic declination; it is never stored.
     *
     * The flow finishes when collection stops, or immediately on a device with
     * no usable sensor.
     */
    fun readings(latitude: Double, longitude: Double): Flow<CompassReading> = callbackFlow {
        val manager = sensorManager
        val sensor = rotationSensor
        if (manager == null || sensor == null) {
            close()
            return@callbackFlow
        }

        // Declination changes slowly with position and time, so it is fixed for
        // the lifetime of a collection.
        val declination = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            SEA_LEVEL_METRES,
            System.currentTimeMillis(),
        ).declination.toDouble()

        val listener = object : SensorEventListener {
            // Until the platform reports accuracy, a reading is treated as
            // approximate rather than trusted: some devices never send
            // onAccuracyChanged, and a permanent calibration trap would hide a
            // working compass.
            private var accuracy = CompassAccuracy.MEDIUM
            private var heading: Double? = null

            override fun onSensorChanged(event: SensorEvent) {
                val rotation = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotation, orientation)

                val magneticHeading = Math.toDegrees(orientation[0].toDouble())
                val trueHeading = QiblaBearing.trueHeading(magneticHeading, declination)
                heading = trueHeading
                trySend(CompassReading(trueHeading, accuracy))
            }

            override fun onAccuracyChanged(sensor: Sensor?, newAccuracy: Int) {
                accuracy = accuracyOf(newAccuracy)
                // A device may report accuracy before the first reading; send
                // the last heading again so the screen reflects it at once.
                heading?.let { trySend(CompassReading(it, accuracy)) }
            }
        }

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { manager.unregisterListener(listener) }
    }

    private companion object {
        const val SEA_LEVEL_METRES = 0f

        fun accuracyOf(status: Int): CompassAccuracy = when (status) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
            else -> CompassAccuracy.UNRELIABLE
        }
    }
}
