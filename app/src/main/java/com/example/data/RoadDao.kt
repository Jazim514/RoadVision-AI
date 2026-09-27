package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoadDefectDao {
    @Query("SELECT * FROM road_defects ORDER BY timestamp DESC")
    fun getAllDefects(): Flow<List<RoadDefectEntity>>

    @Query("SELECT * FROM road_defects WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getDefectsForSession(sessionId: String): Flow<List<RoadDefectEntity>>

    @Query("SELECT * FROM road_defects WHERE syncStatus = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingSyncDefects(): List<RoadDefectEntity>

    @Query("SELECT COUNT(*) FROM road_defects WHERE syncStatus = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM road_defects WHERE defectType = 'POTHOLE'")
    fun getPotholeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM road_defects")
    fun getTotalDefectCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefect(defect: RoadDefectEntity): Long

    @Query("UPDATE road_defects SET syncStatus = :status WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<Long>, status: String)

    @Query("UPDATE road_defects SET syncStatus = :status")
    suspend fun markAllSyncStatus(status: String)

    @Query("DELETE FROM road_defects WHERE id = :id")
    suspend fun deleteDefect(id: Long)

    @Query("DELETE FROM road_defects")
    suspend fun deleteAllDefects()
}

@Dao
interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreadcrumb(breadcrumb: TelemetryBreadcrumbEntity)

    @Query("SELECT * FROM telemetry_breadcrumbs WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentBreadcrumbs(sessionId: String, limit: Int = 100): Flow<List<TelemetryBreadcrumbEntity>>

    @Query("SELECT * FROM telemetry_breadcrumbs ORDER BY timestamp DESC LIMIT 200")
    fun getAllBreadcrumbs(): Flow<List<TelemetryBreadcrumbEntity>>

    @Query("DELETE FROM telemetry_breadcrumbs")
    suspend fun clearBreadcrumbs()
}

@Dao
interface SurveySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: SurveySessionEntity)

    @Query("SELECT * FROM survey_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): SurveySessionEntity?

    @Query("SELECT * FROM survey_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SurveySessionEntity>>

    @Query("UPDATE survey_sessions SET isActive = 0, endTime = :endTime, distanceKm = :distanceKm, potholeCount = :potholeCount, crackCount = :crackCount, avgSpeedKmh = :avgSpeed WHERE isActive = 1")
    suspend fun stopActiveSession(
        endTime: Long,
        distanceKm: Float,
        potholeCount: Int,
        crackCount: Int,
        avgSpeed: Float
    )
}

@Dao
interface GpsLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: GpsLocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<GpsLocationEntity>)

    @Query("SELECT * FROM gps_locations ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<GpsLocationEntity>>

    @Query("SELECT * FROM gps_locations ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLocations(limit: Int = 100): Flow<List<GpsLocationEntity>>

    @Query("SELECT * FROM gps_locations ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLocation(): GpsLocationEntity?

    @Query("SELECT COUNT(*) FROM gps_locations")
    fun getLocationCount(): Flow<Int>

    @Query("DELETE FROM gps_locations")
    suspend fun clearLocations()
}
