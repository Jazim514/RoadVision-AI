package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class RoadTelemetryRepository(
    private val defectDao: RoadDefectDao,
    private val telemetryDao: TelemetryDao,
    private val sessionDao: SurveySessionDao,
    val gpsLocationDao: GpsLocationDao
) {
    val allDefects: Flow<List<RoadDefectEntity>> = defectDao.getAllDefects()
    val pendingSyncCount: Flow<Int> = defectDao.getPendingCount()
    val potholeCount: Flow<Int> = defectDao.getPotholeCount()
    val totalDefectCount: Flow<Int> = defectDao.getTotalDefectCount()
    val allBreadcrumbs: Flow<List<TelemetryBreadcrumbEntity>> = telemetryDao.getAllBreadcrumbs()
    val allSessions: Flow<List<SurveySessionEntity>> = sessionDao.getAllSessions()
    val allGpsLocations: Flow<List<GpsLocationEntity>> = gpsLocationDao.getAllLocations()
    val gpsLocationCount: Flow<Int> = gpsLocationDao.getLocationCount()

    suspend fun logDefect(
        sessionId: String,
        latitude: Double,
        longitude: Double,
        defectType: String,
        severity: String,
        confidence: Float,
        depthCm: Float,
        surfaceAreaCm2: Float,
        peakGForce: Float,
        vehicleSpeedKmh: Float,
        streetName: String,
        distanceMeters: Float,
        isConfirmed: Boolean,
        notes: String = ""
    ): Long {
        val entity = RoadDefectEntity(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude,
            defectType = defectType,
            severity = severity,
            confidence = confidence,
            depthCm = depthCm,
            surfaceAreaCm2 = surfaceAreaCm2,
            peakGForce = peakGForce,
            vehicleSpeedKmh = vehicleSpeedKmh,
            streetName = streetName,
            distanceMeters = distanceMeters,
            isConfirmed = isConfirmed,
            notes = notes,
            syncStatus = "PENDING"
        )
        return defectDao.insertDefect(entity)
    }

    suspend fun logBreadcrumb(
        sessionId: String,
        latitude: Double,
        longitude: Double,
        speedKmh: Float,
        verticalG: Float,
        roughnessIri: Float,
        roadQualityScore: Int
    ) {
        val breadcrumb = TelemetryBreadcrumbEntity(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude,
            speedKmh = speedKmh,
            verticalG = verticalG,
            roughnessIri = roughnessIri,
            roadQualityScore = roadQualityScore
        )
        telemetryDao.insertBreadcrumb(breadcrumb)
    }

    suspend fun startNewSession(title: String = "Road Inspection Survey"): String {
        val sessionId = UUID.randomUUID().toString().take(8)
        val session = SurveySessionEntity(
            sessionId = sessionId,
            title = "$title #$sessionId",
            startTime = System.currentTimeMillis(),
            isActive = true
        )
        sessionDao.insertOrUpdate(session)
        return sessionId
    }

    suspend fun stopActiveSession(
        distanceKm: Float,
        potholes: Int,
        cracks: Int,
        avgSpeed: Float
    ) {
        sessionDao.stopActiveSession(
            endTime = System.currentTimeMillis(),
            distanceKm = distanceKm,
            potholeCount = potholes,
            crackCount = cracks,
            avgSpeed = avgSpeed
        )
    }

    suspend fun getPendingDefects(): List<RoadDefectEntity> {
        return defectDao.getPendingSyncDefects()
    }

    suspend fun markDefectsSynced(ids: List<Long>) {
        defectDao.updateSyncStatus(ids, "SYNCED")
    }

    suspend fun deleteDefect(id: Long) {
        defectDao.deleteDefect(id)
    }

    suspend fun clearAllData() {
        defectDao.deleteAllDefects()
        telemetryDao.clearBreadcrumbs()
        gpsLocationDao.clearLocations()
    }

    suspend fun seedDemoDataIfEmpty(baseLat: Double, baseLng: Double) {
        val count = defectDao.getTotalDefectCount().first()
        if (count > 0) return

        val demoSessionId = "SURV-DEMO"
        val sampleDefects = listOf(
            RoadDefectEntity(
                sessionId = demoSessionId,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 18,
                latitude = baseLat + 0.0034,
                longitude = baseLng + 0.0021,
                defectType = "POTHOLE",
                severity = "SEVERE",
                confidence = 0.94f,
                depthCm = 6.8f,
                surfaceAreaCm2 = 420f,
                peakGForce = 2.45f,
                vehicleSpeedKmh = 42f,
                streetName = "Grand Boulevard (Northbound Lane)",
                distanceMeters = 8.5f,
                isConfirmed = true,
                syncStatus = "SYNCED",
                notes = "High-impact edge AI detection correlated with 2.45g vertical accelerometer spike"
            ),
            RoadDefectEntity(
                sessionId = demoSessionId,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 12,
                latitude = baseLat - 0.0028,
                longitude = baseLng + 0.0042,
                defectType = "ALLIGATOR_CRACK",
                severity = "MEDIUM",
                confidence = 0.88f,
                depthCm = 2.1f,
                surfaceAreaCm2 = 950f,
                peakGForce = 1.35f,
                vehicleSpeedKmh = 50f,
                streetName = "Central Expressway Corridor",
                distanceMeters = 14.0f,
                isConfirmed = false,
                syncStatus = "SYNCED",
                notes = "Fatigue cracking pattern detected by multi-scale edge segmentation"
            ),
            RoadDefectEntity(
                sessionId = demoSessionId,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 6,
                latitude = baseLat + 0.0015,
                longitude = baseLng - 0.0031,
                defectType = "POTHOLE",
                severity = "CRITICAL",
                confidence = 0.97f,
                depthCm = 9.2f,
                surfaceAreaCm2 = 680f,
                peakGForce = 3.12f,
                vehicleSpeedKmh = 38f,
                streetName = "7th Avenue & Industrial Pkwy",
                distanceMeters = 5.2f,
                isConfirmed = true,
                syncStatus = "PENDING",
                notes = "Severe structural crater hazard. Rim damage risk. Telemetry logged."
            ),
            RoadDefectEntity(
                sessionId = demoSessionId,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 2,
                latitude = baseLat - 0.0019,
                longitude = baseLng - 0.0025,
                defectType = "MANHOLE_OFFSET",
                severity = "MEDIUM",
                confidence = 0.89f,
                depthCm = 3.4f,
                surfaceAreaCm2 = 310f,
                peakGForce = 1.62f,
                vehicleSpeedKmh = 45f,
                streetName = "Metro Transit Loop",
                distanceMeters = 11.0f,
                isConfirmed = true,
                syncStatus = "PENDING",
                notes = "Sunken iron utility cover offset 3.4cm below road grade"
            )
        )

        sampleDefects.forEach { defectDao.insertDefect(it) }

        // Also add breadcrumb trajectory
        for (i in 0..20) {
            val stepFraction = i / 20.0
            telemetryDao.insertBreadcrumb(
                TelemetryBreadcrumbEntity(
                    sessionId = demoSessionId,
                    timestamp = System.currentTimeMillis() - (20 - i) * 60 * 1000,
                    latitude = baseLat - 0.004 + 0.008 * stepFraction,
                    longitude = baseLng - 0.004 + 0.007 * stepFraction + (if (i % 3 == 0) 0.001 else 0.0),
                    speedKmh = (40f + (i % 5) * 2.5f),
                    verticalG = if (i == 5 || i == 14) 2.2f else 1.05f,
                    roughnessIri = if (i in 4..7) 4.2f else 1.8f,
                    roadQualityScore = if (i in 4..7) 54 else 88
                )
            )
        }
    }
}
