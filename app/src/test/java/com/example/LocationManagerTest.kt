package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.RoadVisionDatabase
import com.example.location.LocationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationManagerTest {

    private lateinit var database: RoadVisionDatabase
    private lateinit var locationManager: LocationManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RoadVisionDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        locationManager = LocationManager(
            context = context,
            gpsLocationDao = database.gpsLocationDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPersistGpsCoordinatesWithTimestamp() = runBlocking {
        val timestamp = System.currentTimeMillis()
        val id1 = locationManager.persistLocation(
            latitude = 37.7749,
            longitude = -122.4194,
            altitudeMeters = 15.2,
            accuracyMeters = 3.5f,
            speedKmh = 45.0f,
            bearingDegrees = 180.0f,
            timestamp = timestamp
        )
        assertTrue(id1 > 0)

        val id2 = locationManager.persistLocation(
            latitude = 37.7750,
            longitude = -122.4195,
            altitudeMeters = 15.4,
            accuracyMeters = 3.2f,
            speedKmh = 48.0f,
            bearingDegrees = 182.0f,
            timestamp = timestamp + 1000L
        )
        assertTrue(id2 > 0)

        val count = locationManager.getPersistedLocationCount().first()
        assertEquals(2, count)

        val locations = locationManager.getAllPersistedLocations().first()
        assertEquals(2, locations.size)
        assertEquals(timestamp + 1000L, locations[0].timestamp)
        assertEquals(37.7750, locations[0].latitude, 0.0001)
        assertEquals(-122.4195, locations[0].longitude, 0.0001)

        val latest = database.gpsLocationDao().getLatestLocation()
        assertNotNull(latest)
        assertEquals(37.7750, latest!!.latitude, 0.0001)
    }

    @Test
    fun testClearPersistedLocations() = runBlocking {
        locationManager.persistLocation(
            latitude = 40.7128,
            longitude = -74.0060,
            timestamp = System.currentTimeMillis()
        )
        assertEquals(1, locationManager.getPersistedLocationCount().first())

        locationManager.clearPersistedLocations()
        assertEquals(0, locationManager.getPersistedLocationCount().first())
    }
}
