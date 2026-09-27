package com.example.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RoadDefectEntity
import com.example.data.RoadTelemetryRepository
import com.example.data.RoadVisionDatabase
import com.example.data.SurveySessionEntity
import com.example.data.TelemetryBreadcrumbEntity
import com.example.edgeai.DefectSeverity
import com.example.edgeai.DefectType
import com.example.edgeai.DetectedRoadDefect
import com.example.edgeai.DetectionMode
import com.example.edgeai.EdgeRoadDetector
import com.example.edgeai.InferenceMetrics
import com.example.location.LocationManager
import com.example.data.GpsLocationEntity
import com.example.sync.MappingBackendClient
import com.example.sync.SyncState
import com.example.telemetry.SensorTelemetryEngine
import com.example.telemetry.TelemetrySnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UiAlert(
    val defect: DetectedRoadDefect,
    val expiresAt: Long = System.currentTimeMillis() + 3500L
)

class RoadVisionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RoadVisionDatabase.getInstance(application)
    val repository = RoadTelemetryRepository(
        db.roadDefectDao(),
        db.telemetryDao(),
        db.surveySessionDao(),
        db.gpsLocationDao()
    )

    val backendClient = MappingBackendClient()

    // Fused Location Provider Client Manager
    val locationManager = LocationManager(
        context = application,
        gpsLocationDao = db.gpsLocationDao()
    )

    // Sensor & Edge AI Engines
    val edgeDetector: EdgeRoadDetector = EdgeRoadDetector(
        onDefectDetected = { defect -> onDefectIdentifiedByVision(defect) }
    )

    val sensorEngine: SensorTelemetryEngine = SensorTelemetryEngine(
        application,
        onSevereBumpTriggered = { peakG, timestamp -> onSevereBumpEvent(peakG, timestamp) }
    )

    // UI States
    private val _detectionMode = MutableStateFlow(DetectionMode.SIMULATED_DASHCAM_DRIVE)
    val detectionMode: StateFlow<DetectionMode> = _detectionMode.asStateFlow()

    private val _isSurveying = MutableStateFlow(false)
    val isSurveying: StateFlow<Boolean> = _isSurveying.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _sessionDistanceMeters = MutableStateFlow(0f)
    val sessionDistanceMeters: StateFlow<Float> = _sessionDistanceMeters.asStateFlow()

    private val _sessionDurationSeconds = MutableStateFlow(0L)
    val sessionDurationSeconds: StateFlow<Long> = _sessionDurationSeconds.asStateFlow()

    private val _simulatedSpeedKmh = MutableStateFlow(48f)
    val simulatedSpeedKmh: StateFlow<Float> = _simulatedSpeedKmh.asStateFlow()

    private val _currentAlert = MutableStateFlow<UiAlert?>(null)
    val currentAlert: StateFlow<UiAlert?> = _currentAlert.asStateFlow()

    private val _selectedDefect = MutableStateFlow<RoadDefectEntity?>(null)
    val selectedDefect: StateFlow<RoadDefectEntity?> = _selectedDefect.asStateFlow()

    private val _exportContent = MutableStateFlow<String?>(null)
    val exportContent: StateFlow<String?> = _exportContent.asStateFlow()

    // Filter states
    val filterType = MutableStateFlow<String?>("ALL")
    val filterSeverity = MutableStateFlow<String?>("ALL")

    // State from Repository
    val allDefects: StateFlow<List<RoadDefectEntity>> = repository.allDefects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val potholeCount: StateFlow<Int> = repository.potholeCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalDefectCount: StateFlow<Int> = repository.totalDefectCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allBreadcrumbs: StateFlow<List<TelemetryBreadcrumbEntity>> = repository.allBreadcrumbs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<SurveySessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val persistedGpsCount: StateFlow<Int> = repository.gpsLocationCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val latestGpsLocation: StateFlow<GpsLocationEntity?> = locationManager.latestLocation
    val isGpsStreaming: StateFlow<Boolean> = locationManager.isStreaming

    // Engines StateFlows
    val inferenceMetrics: StateFlow<InferenceMetrics> = edgeDetector.inferenceMetrics
    val currentDetections: StateFlow<List<DetectedRoadDefect>> = edgeDetector.currentDetections
    val telemetryState: StateFlow<TelemetrySnapshot> = sensorEngine.telemetryState
    val syncState: StateFlow<SyncState> = backendClient.syncState

    private var simulationLoopJob: Job? = null
    private var telemetryLoggingJob: Job? = null
    private var sessionTimerJob: Job? = null
    private var sessionPotholes = 0
    private var sessionCracks = 0

    init {
        sensorEngine.startListening()

        // Forward real FusedLocationProviderClient updates to SensorTelemetryEngine
        viewModelScope.launch {
            locationManager.latestLocation.collect { gps ->
                if (gps != null) {
                    sensorEngine.updateLocationFromGps(
                        latitude = gps.latitude,
                        longitude = gps.longitude,
                        speedKmh = gps.speedKmh,
                        bearingDegrees = gps.bearingDegrees,
                        altitudeMeters = gps.altitudeMeters
                    )
                }
            }
        }

        // Seed realistic demo data around current lat/lng
        viewModelScope.launch {
            repository.seedDemoDataIfEmpty(37.7749, -122.4194)
        }

        // Start default high-performance simulation loop
        startSimulationEngine()
    }

    override fun onCleared() {
        super.onCleared()
        sensorEngine.stopListening()
        locationManager.stopContinuousTracking()
        simulationLoopJob?.cancel()
        telemetryLoggingJob?.cancel()
        sessionTimerJob?.cancel()
    }

    fun setDetectionMode(mode: DetectionMode) {
        _detectionMode.value = mode
        if (mode == DetectionMode.SIMULATED_DASHCAM_DRIVE) {
            startSimulationEngine()
        } else {
            simulationLoopJob?.cancel()
        }
    }

    fun setSimulatedSpeed(speed: Float) {
        _simulatedSpeedKmh.value = speed.coerceIn(10f, 110f)
    }

    fun selectDefect(defect: RoadDefectEntity?) {
        _selectedDefect.value = defect
    }

    fun clearExport() {
        _exportContent.value = null
    }

    fun exportGeoJson() {
        viewModelScope.launch {
            val defects = allDefects.value
            val breadcrumbs = allBreadcrumbs.value
            val geoJson = backendClient.exportGeoJson(defects, breadcrumbs)
            _exportContent.value = geoJson
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val defects = allDefects.value
            val csv = backendClient.exportCsv(defects)
            _exportContent.value = csv
        }
    }

    fun triggerBackendSync() {
        viewModelScope.launch {
            val pending = repository.getPendingDefects()
            val breadcrumbs = allBreadcrumbs.value
            backendClient.syncBatchToBackend(
                defects = pending,
                breadcrumbs = breadcrumbs,
                onSuccess = { syncedIds ->
                    repository.markDefectsSynced(syncedIds)
                }
            )
        }
    }

    fun deleteDefect(id: Long) {
        viewModelScope.launch {
            repository.deleteDefect(id)
            if (_selectedDefect.value?.id == id) {
                _selectedDefect.value = null
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _selectedDefect.value = null
        }
    }

    fun toggleSurveySession() {
        if (_isSurveying.value) {
            stopSurveySession()
        } else {
            startSurveySession()
        }
    }

    private fun startSurveySession() {
        viewModelScope.launch {
            val sessionId = repository.startNewSession("Survey Run")
            _currentSessionId.value = sessionId
            _isSurveying.value = true
            _sessionDistanceMeters.value = 0f
            _sessionDurationSeconds.value = 0L
            sessionPotholes = 0
            sessionCracks = 0

            // Start continuous GPS tracking via FusedLocationProviderClient if permissions granted
            if (locationManager.hasLocationPermission()) {
                locationManager.startContinuousTracking(intervalMillis = 1000L)
            }

            // Start session breadcrumb periodic logging
            telemetryLoggingJob = launch {
                while (isActive) {
                    delay(1500)
                    val tele = telemetryState.value
                    repository.logBreadcrumb(
                        sessionId = sessionId,
                        latitude = tele.latitude,
                        longitude = tele.longitude,
                        speedKmh = tele.speedKmh,
                        verticalG = tele.verticalGForce,
                        roughnessIri = tele.roughnessIri,
                        roadQualityScore = tele.roadQualityScore
                    )
                }
            }

            // Start session duration timer
            sessionTimerJob = launch {
                while (isActive) {
                    delay(1000)
                    _sessionDurationSeconds.value += 1
                    val currentSpeedMps = (simulatedSpeedKmh.value * 1000f) / 3600f
                    _sessionDistanceMeters.value += currentSpeedMps
                }
            }
        }
    }

    private fun stopSurveySession() {
        viewModelScope.launch {
            val distanceKm = _sessionDistanceMeters.value / 1000f
            repository.stopActiveSession(
                distanceKm = distanceKm,
                potholes = sessionPotholes,
                cracks = sessionCracks,
                avgSpeed = simulatedSpeedKmh.value
            )
            _isSurveying.value = false
            locationManager.stopContinuousTracking()
            telemetryLoggingJob?.cancel()
            sessionTimerJob?.cancel()
        }
    }

    private fun startSimulationEngine() {
        simulationLoopJob?.cancel()
        simulationLoopJob = viewModelScope.launch {
            // Sustained 30+ FPS tick loop (~30ms intervals)
            while (isActive) {
                val speed = _simulatedSpeedKmh.value

                // Advance GPS kinematics
                sensorEngine.tickSimulatedTrajectory(speed, dtSec = 0.033f)

                // Advance Edge AI video frame detector
                edgeDetector.tickSimulatedFrame(speed) { impactBumpG ->
                    // Impact physically felt!
                    sensorEngine.injectBumpSpike(impactBumpG)
                }

                // Check alert banner expiration
                _currentAlert.value?.let { alert ->
                    if (System.currentTimeMillis() > alert.expiresAt) {
                        _currentAlert.value = null
                    }
                }

                delay(33) // ~30 FPS
            }
        }
    }

    private fun onDefectIdentifiedByVision(defect: DetectedRoadDefect) {
        val tele = telemetryState.value
        val sessionId = _currentSessionId.value ?: "DRIVE-SESSION"

        if (defect.severity == DefectSeverity.SEVERE || defect.severity == DefectSeverity.CRITICAL) {
            _currentAlert.value = UiAlert(defect)
        }

        if (defect.type == DefectType.POTHOLE) sessionPotholes++ else sessionCracks++

        viewModelScope.launch {
            val streetNames = listOf(
                "Market St Expressway",
                "Grand Avenue Parkway",
                "Harbor Industrial Way",
                "Mission Boulevard",
                "Civic Center Loop"
            )
            val street = streetNames[(tele.latitude * 1000).toInt().coerceAtLeast(0) % streetNames.size]

            repository.logDefect(
                sessionId = sessionId,
                latitude = tele.latitude,
                longitude = tele.longitude,
                defectType = defect.type.name,
                severity = defect.severity.name,
                confidence = defect.confidence,
                depthCm = defect.estimatedDepthCm,
                surfaceAreaCm2 = defect.estimatedSurfaceAreaCm2,
                peakGForce = tele.peakGForceWindow,
                vehicleSpeedKmh = tele.speedKmh,
                streetName = street,
                distanceMeters = defect.estimatedDistanceMeters,
                isConfirmed = defect.isConfirmedByGForce || tele.peakGForceWindow > 1.6f,
                notes = "Identified via Edge AI frame analysis at ${tele.speedKmh.toInt()} km/h"
            )
        }
    }

    private fun onSevereBumpEvent(peakG: Float, timestamp: Long) {
        // Accelerometer felt severe road impact! Fuse with vision
        val tele = telemetryState.value
        val sessionId = _currentSessionId.value ?: "DRIVE-SESSION"

        viewModelScope.launch {
            repository.logDefect(
                sessionId = sessionId,
                latitude = tele.latitude,
                longitude = tele.longitude,
                defectType = "POTHOLE",
                severity = if (peakG > 2.5f) "CRITICAL" else "SEVERE",
                confidence = 0.96f,
                depthCm = (peakG * 3.2f).coerceIn(3.5f, 12.0f),
                surfaceAreaCm2 = (peakG * 210f).coerceIn(200f, 800f),
                peakGForce = peakG,
                vehicleSpeedKmh = tele.speedKmh,
                streetName = "Dynamic Impact Location",
                distanceMeters = 0.5f,
                isConfirmed = true,
                notes = "Physical suspension impact: Z-acceleration peak ${String.format("%.2f", peakG)}G"
            )
        }
    }
}
