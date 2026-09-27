package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.GpsLocationDao
import com.example.data.GpsLocationEntity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * LocationManager responsible for streaming continuous GPS coordinates
 * via FusedLocationProviderClient and persisting them into Room database
 * with precise timestamps.
 */
class LocationManager(
    private val context: Context,
    private val gpsLocationDao: GpsLocationDao,
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context),
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "LocationManager"
        const val DEFAULT_INTERVAL_MILLIS = 1000L
        const val DEFAULT_FASTEST_INTERVAL_MILLIS = 500L
        const val DEFAULT_MIN_DISTANCE_METERS = 0f
    }

    private val _latestLocation = MutableStateFlow<GpsLocationEntity?>(null)
    val latestLocation: StateFlow<GpsLocationEntity?> = _latestLocation.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private var activeStreamingJob: Job? = null

    /**
     * Checks if location permissions are granted.
     */
    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    /**
     * Streams continuous GPS coordinates using FusedLocationProviderClient as a Flow.
     * Each received coordinate is automatically persisted in the Room database
     * along with its timestamp.
     */
    @SuppressLint("MissingPermission")
    fun streamLocationUpdates(
        intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
        minDistanceMeters: Float = DEFAULT_MIN_DISTANCE_METERS,
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    ): Flow<GpsLocationEntity> = callbackFlow {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted. Cannot stream GPS coordinates.")
            close(SecurityException("ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permission not granted"))
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(priority, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .setWaitForAccurateLocation(false)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val entity = locationToEntity(location)

                    // 1. Update StateFlow
                    _latestLocation.value = entity

                    // 2. Persist in Room Database with timestamp
                    coroutineScope.launch {
                        try {
                            gpsLocationDao.insertLocation(entity)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error persisting GPS coordinate to Room: ${e.message}", e)
                        }
                    }

                    // 3. Emit downstream to active Flow collectors
                    trySend(entity)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            _isStreaming.value = true
            Log.i(TAG, "Continuous GPS streaming started with interval: ${intervalMillis}ms")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to request location updates due to SecurityException", e)
            close(e)
            return@callbackFlow
        }

        awaitClose {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                _isStreaming.value = false
                Log.i(TAG, "GPS streaming stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing location updates: ${e.message}", e)
            }
        }
    }

    /**
     * Starts continuous tracking and automatically persists every GPS location.
     */
    fun startContinuousTracking(
        intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
        minDistanceMeters: Float = DEFAULT_MIN_DISTANCE_METERS
    ) {
        stopContinuousTracking()
        activeStreamingJob = coroutineScope.launch {
            streamLocationUpdates(intervalMillis, minDistanceMeters).collect { location ->
                Log.d(TAG, "Received & Persisted GPS: (${location.latitude}, ${location.longitude}) at ${location.timestamp}")
            }
        }
    }

    /**
     * Stops continuous tracking if running.
     */
    fun stopContinuousTracking() {
        activeStreamingJob?.cancel()
        activeStreamingJob = null
        _isStreaming.value = false
    }

    /**
     * Persists a GPS coordinate record manually into the Room database.
     * Useful for telemetry sensor fusion, breadcrumbs, or offline simulation.
     */
    suspend fun persistLocation(
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double = 0.0,
        accuracyMeters: Float = 0.0f,
        speedKmh: Float = 0.0f,
        bearingDegrees: Float = 0.0f,
        timestamp: Long = System.currentTimeMillis(),
        provider: String = "manual_gps"
    ): Long {
        val entity = GpsLocationEntity(
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = altitudeMeters,
            accuracyMeters = accuracyMeters,
            speedKmh = speedKmh,
            bearingDegrees = bearingDegrees,
            provider = provider
        )
        _latestLocation.value = entity
        return gpsLocationDao.insertLocation(entity)
    }

    /**
     * Returns a Flow of all persisted GPS coordinates stored in Room, ordered by newest first.
     */
    fun getAllPersistedLocations(): Flow<List<GpsLocationEntity>> = gpsLocationDao.getAllLocations()

    /**
     * Returns a Flow of the latest N persisted GPS coordinates.
     */
    fun getRecentPersistedLocations(limit: Int = 100): Flow<List<GpsLocationEntity>> =
        gpsLocationDao.getRecentLocations(limit)

    /**
     * Returns the total count of persisted GPS coordinates.
     */
    fun getPersistedLocationCount(): Flow<Int> = gpsLocationDao.getLocationCount()

    /**
     * Clears all persisted GPS locations in the database.
     */
    suspend fun clearPersistedLocations() {
        gpsLocationDao.clearLocations()
    }

    /**
     * Converts Android Location object to Room Entity with speed in km/h and timestamp.
     */
    private fun locationToEntity(location: Location): GpsLocationEntity {
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
        val timestamp = if (location.time > 0) location.time else System.currentTimeMillis()

        return GpsLocationEntity(
            timestamp = timestamp,
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = if (location.hasAltitude()) location.altitude else 0.0,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else 0.0f,
            speedKmh = speedKmh,
            bearingDegrees = if (location.hasBearing()) location.bearing else 0.0f,
            provider = location.provider ?: "fused_gps"
        )
    }
}
