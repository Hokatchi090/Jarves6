package com.jarvis.assistant

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// \u0628\u0648\u0635\u0644\u0629 \u062D\u0642\u064A\u0642\u064A\u0629 \u062A\u0639\u062A\u0645\u062F \u0639\u0644\u0649 \u062D\u0633\u0627\u0633\u0627\u062A \u0627\u0644\u062C\u0647\u0627\u0632 \u0627\u0644\u0641\u0639\u0644\u064A\u0629 (\u0627\u0644\u062A\u0633\u0627\u0631\u0639/\u0627\u0644\u0645\u063A\u0646\u0627\u0637\u064A\u0633\u064A)\u060C \u0645\u0641\u064A\u062F\u0629 \u0644\u0642\u064A\u0627\u0633\u0627\u062A \u0627\u062A\u062C\u0627\u0647 \u0637\u0628\u0642\u0627\u062A \u0627\u0644\u0635\u062E\u0648\u0631 \u0627\u0644\u0645\u064A\u062F\u0627\u0646\u064A\u0629 (Strike)
class GeoCompassHelper(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravityData = FloatArray(3)
    private val geomagneticData = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var currentBearing = 0f
    private var currentTiltDegrees = 0f

    fun isAvailable(): Boolean = accelerometer != null && magnetometer != null

    fun start() {
        if (!isAvailable()) return
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    // \u0627\u0644\u0627\u062A\u062C\u0627\u0647 \u0627\u0644\u062D\u0627\u0644\u064A \u0628\u0627\u0644\u062F\u0631\u062C\u0627\u062A (0-360)\u060C 0 = \u0634\u0645\u0627\u0644
    fun getBearing(): Float = currentBearing

    // \u0645\u064A\u0644 \u0627\u0644\u062C\u0647\u0627\u0632 \u0639\u0646 \u0627\u0644\u0623\u0641\u0642\u064A (\u062A\u0642\u0631\u064A\u0628\u064A \u0644\u0632\u0627\u0648\u064A\u0629 \u0627\u0644\u0645\u064A\u0644/Dip \u0625\u0630\u0627 \u0627\u0644\u0647\u0627\u062A\u0641 \u0645\u0648\u0636\u0648\u0639 \u0645\u0633\u0637\u062D \u0639\u0644\u0649 \u0627\u0644\u0635\u062E\u0631)
    fun getTiltDegrees(): Float = currentTiltDegrees

    fun getCompassDirectionLabel(): String {
        val directions = arrayOf(
            "\u0634\u0645\u0627\u0644", "\u0634\u0645\u0627\u0644 \u0634\u0631\u0642\u064A", "\u0634\u0631\u0642", "\u062C\u0646\u0648\u0628 \u0634\u0631\u0642\u064A",
            "\u062C\u0646\u0648\u0628", "\u062C\u0646\u0648\u0628 \u063A\u0631\u0628\u064A", "\u063A\u0631\u0628", "\u0634\u0645\u0627\u0644 \u063A\u0631\u0628\u064A"
        )
        val index = (((currentBearing + 22.5f) / 45f).toInt()) % 8
        return directions[if (index < 0) index + 8 else index]
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityData, 0, 3)
                hasGravity = true
                // \u062A\u0642\u062F\u064A\u0631 \u0645\u064A\u0644 \u0627\u0644\u062C\u0647\u0627\u0632 \u0639\u0646 \u0627\u0644\u0623\u0641\u0642\u064A \u0645\u0646 \u0645\u062D\u0648\u0631 Z \u0644\u0644\u062A\u0633\u0627\u0631\u0639
                val z = event.values[2]
                val magnitude = Math.sqrt(
                    (event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]).toDouble()
                ).toFloat()
                if (magnitude > 0.1f) {
                    val cosAngle = (z / magnitude).coerceIn(-1f, 1f)
                    currentTiltDegrees = Math.toDegrees(Math.acos(cosAngle.toDouble())).toFloat()
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagneticData, 0, 3)
                hasGeomagnetic = true
            }
        }

        if (hasGravity && hasGeomagnetic) {
            val rotationMatrix = FloatArray(9)
            val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravityData, geomagneticData)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                var bearing = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (bearing < 0) bearing += 360f
                currentBearing = bearing
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // \u0644\u0627 \u062D\u0627\u062C\u0629 \u0644\u0645\u0639\u0627\u0644\u062C\u0629 \u062E\u0627\u0635\u0629 \u0647\u0646\u0627
    }
}
