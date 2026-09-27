package com.example.telemetry

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class TelemetrySnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val altitudeMeters: Double = 18.0,
    val speedKmh: Float = 42.5f,
    val bearingDegrees: Float = 65.0f,
    val isGpsLocked: Boolean = false,
    val verticalGForce: Float = 1.0f,
    val peakGForceWindow: Float = 1.0f,
    val rawZAcceleration: Float = 9.81f,
    val linearZAcceleration: Float = 0.0f,
    val roughnessIri: Float = 2.1f, // IRI in m/km (1.5 = good asphalt, 4.0 = degraded, 8+ = severely ruined)
    val roadQualityScore: Int = 86, // 0-100 scale
    val vibrationHistory: List<Float> = emptyList() // last 40 samples for seismograph oscilloscope
)

class SensorTelemetryEngine(
    private val context: Context,
    private val onSevereBumpTriggered: (peakG: Float, timestamp: Long) -> Unit
) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _telemetryState = MutableStateFlow(TelemetrySnapshot())
    val telemetryState: StateFlow<TelemetrySnapshot> = _telemetryState.asStateFlow()

    // Sensor filtering
    private var gravityZ = 9.80665f
    private val alpha = 0.8f // Low pass filter factor
    private var lastBumpTimestamp: Long = 0L

    // Live vibration waveform history (sparkline/oscilloscope)
    private val liveWaveform = ArrayDeque<Float>(40).apply {
        repeat(40) { add(1.0f) }
    }

    // Bump sensitivity threshold in Gs (configurable)
    var bumpThresholdG: Float = 1.65f

    // Simulated trajectory state for indoor / emulator testing
    private var simLat = 37.774929
    private var simLng = -122.419416
    private var simSpeedKmh = 45f
    private var simHeading = 48f
    private var isSimulatedNavActive = true

    // Rolling IRI calculator
    private var cumulativeVerticalDisplacement = 0f
    private var sampleCountInWindow = 0
    private var currentIri = 2.1f

    fun startListening() {
        // Accelerometer
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        // Linear Acceleration (gravity already removed by sensor hardware if present)
        sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.also { linearAcc ->
            sensorManager.registerListener(this, linearAcc, SensorManager.SENSOR_DELAY_GAME)
        }

        // GPS
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                1f,
                this
            )
            // Also try Network provider as fallback
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1500L,
                2f,
                this
            )
        } catch (e: SecurityException) {
            // Permission not yet granted, fallback to kinematic simulation
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        locationManager?.removeUpdates(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val z = event.values[2]
                // Separate gravity with low-pass filter
                gravityZ = alpha * gravityZ + (1 - alpha) * z
                val linearZ = z - gravityZ
                val currentG = abs(z) / 9.80665f

                processZAcceleration(currentG, z, linearZ)
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val linearZ = event.values[2]
                val currentG = (abs(linearZ) + 9.80665f) / 9.80665f
                processZAcceleration(currentG, linearZ + 9.80665f, linearZ)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun processZAcceleration(currentG: Float, rawZ: Float, linearZ: Float) {
        synchronized(liveWaveform) {
            if (liveWaveform.size >= 40) {
                liveWaveform.removeFirst()
            }
            liveWaveform.addLast(currentG)
        }

        // IRI estimation accumulator
        cumulativeVerticalDisplacement += abs(linearZ)
        sampleCountInWindow++
        if (sampleCountInWindow >= 50) {
            // Compute roughness IRI estimate: scaled to realistic 1.5 - 6.5 m/km
            val avgDev = cumulativeVerticalDisplacement / sampleCountInWindow
            currentIri = (1.2f + avgDev * 0.9f).coerceIn(1.1f, 8.5f)
            cumulativeVerticalDisplacement = 0f
            sampleCountInWindow = 0
        }

        val roadQualityScore = (100 - (currentIri - 1.2f) * 16f).toInt().coerceIn(10, 99)

        // Peak trigger check
        val now = System.currentTimeMillis()
        if (currentG >= bumpThresholdG && (now - lastBumpTimestamp) > 600L) {
            lastBumpTimestamp = now
            onSevereBumpTriggered(currentG, now)
        }

        val current = _telemetryState.value
        val historyList = synchronized(liveWaveform) { liveWaveform.toList() }
        val maxGInWindow = historyList.maxOrNull() ?: 1.0f

        _telemetryState.value = current.copy(
            verticalGForce = currentG,
            peakGForceWindow = maxGInWindow,
            rawZAcceleration = rawZ,
            linearZAcceleration = linearZ,
            roughnessIri = currentIri,
            roadQualityScore = roadQualityScore,
            vibrationHistory = historyList
        )
    }

    /**
     * Inject synthetic bump spike (e.g. when simulated car hits a pothole)
     */
    fun injectBumpSpike(gForce: Float) {
        processZAcceleration(gForce, gForce * 9.81f, (gForce - 1f) * 9.81f)
    }

    /**
     * Simulates GPS vehicle movement forward along route at current speed
     */
    fun tickSimulatedTrajectory(speedKmh: Float, dtSec: Float = 0.5f) {
        simSpeedKmh = speedKmh
        val speedMps = (speedKmh * 1000f) / 3600f
        val distanceMeters = speedMps * dtSec

        // Convert distance and heading into delta lat / lng
        // 1 degree latitude ≈ 111,000 meters
        val rad = Math.toRadians(simHeading.toDouble())
        val deltaLat = (distanceMeters * kotlin.math.cos(rad)) / 111000.0
        val deltaLng = (distanceMeters * kotlin.math.sin(rad)) / (111000.0 * kotlin.math.cos(Math.toRadians(simLat)))

        simLat += deltaLat
        simLng += deltaLng

        // Gentle street curve
        simHeading = (simHeading + (Math.sin(System.currentTimeMillis() / 8000.0) * 0.4f).toFloat()) % 360f

        val current = _telemetryState.value
        _telemetryState.value = current.copy(
            latitude = simLat,
            longitude = simLng,
            speedKmh = simSpeedKmh,
            bearingDegrees = simHeading,
            isGpsLocked = true,
            altitudeMeters = 24.5 + Math.sin(System.currentTimeMillis() / 15000.0) * 3.0
        )
    }

    // LocationListener callbacks
    override fun onLocationChanged(location: Location) {
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else simSpeedKmh
        val current = _telemetryState.value
        _telemetryState.value = current.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = location.altitude,
            speedKmh = speedKmh,
            bearingDegrees = if (location.hasBearing()) location.bearing else current.bearingDegrees,
            isGpsLocked = true
        )
    }

    /**
     * Updates telemetry state from FusedLocationProviderClient coordinates
     */
    fun updateLocationFromGps(
        latitude: Double,
        longitude: Double,
        speedKmh: Float,
        bearingDegrees: Float,
        altitudeMeters: Double = 0.0
    ) {
        simLat = latitude
        simLng = longitude
        val current = _telemetryState.value
        _telemetryState.value = current.copy(
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = altitudeMeters,
            speedKmh = if (speedKmh > 0f) speedKmh else current.speedKmh,
            bearingDegrees = bearingDegrees,
            isGpsLocked = true
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
