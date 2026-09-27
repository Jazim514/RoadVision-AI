package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "road_defects")
data class RoadDefectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val defectType: String,
    val severity: String,
    val confidence: Float,
    val depthCm: Float,
    val surfaceAreaCm2: Float,
    val peakGForce: Float,
    val vehicleSpeedKmh: Float,
    val syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED
    val streetName: String = "Main Arterial Way",
    val distanceMeters: Float = 0f,
    val isConfirmed: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "telemetry_breadcrumbs")
data class TelemetryBreadcrumbEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val verticalG: Float,
    val roughnessIri: Float,
    val roadQualityScore: Int // 0-100
)

@Entity(tableName = "survey_sessions")
data class SurveySessionEntity(
    @PrimaryKey
    val sessionId: String,
    val title: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0L,
    val distanceKm: Float = 0f,
    val potholeCount: Int = 0,
    val crackCount: Int = 0,
    val avgSpeedKmh: Float = 0f,
    val avgIri: Float = 2.4f,
    val isActive: Boolean = true
)

@Entity(tableName = "gps_locations")
data class GpsLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double = 0.0,
    val accuracyMeters: Float = 0.0f,
    val speedKmh: Float = 0.0f,
    val bearingDegrees: Float = 0.0f,
    val provider: String = "fused_gps"
)
