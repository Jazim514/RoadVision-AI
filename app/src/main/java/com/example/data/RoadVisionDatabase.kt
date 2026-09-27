package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RoadDefectEntity::class,
        TelemetryBreadcrumbEntity::class,
        SurveySessionEntity::class,
        GpsLocationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RoadVisionDatabase : RoomDatabase() {
    abstract fun roadDefectDao(): RoadDefectDao
    abstract fun telemetryDao(): TelemetryDao
    abstract fun surveySessionDao(): SurveySessionDao
    abstract fun gpsLocationDao(): GpsLocationDao

    companion object {
        @Volatile
        private var INSTANCE: RoadVisionDatabase? = null

        fun getInstance(context: Context): RoadVisionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoadVisionDatabase::class.java,
                    "road_vision_telemetry.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
