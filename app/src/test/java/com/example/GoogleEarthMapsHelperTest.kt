package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.maps.GoogleEarthMapsHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleEarthMapsHelperTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testGenerateGoogleEarthWebUrl() {
        val lat = 37.7749
        val lng = -122.4194
        val altitude = 350.0
        val heading = 45.0f
        val tilt = 65.0f

        val url = GoogleEarthMapsHelper.generateGoogleEarthWebUrl(lat, lng, altitude, heading, tilt)
        assertNotNull(url)
        assertTrue(url.startsWith("https://earth.google.com/web/@37.774900,-122.419400"))
        assertTrue(url.contains("350.0a"))
        assertTrue(url.contains("45.0h"))
        assertTrue(url.contains("65.0t"))
    }

    @Test
    fun testGenerateGoogleMapsWebUrl() {
        val lat = 40.7128
        val lng = -74.0060
        val zoom = 18

        val url = GoogleEarthMapsHelper.generateGoogleMapsWebUrl(lat, lng, zoom)
        assertNotNull(url)
        assertTrue(url.startsWith("https://www.google.com/maps/@40.712800,-74.006000,18z"))
    }

    @Test
    fun testOpenGoogleEarth3DSafeExecution() {
        // Must execute cleanly without unhandled exceptions
        GoogleEarthMapsHelper.openGoogleEarth3D(
            context = context,
            latitude = 34.0522,
            longitude = -118.2437,
            altitudeMeters = 400.0,
            headingDegrees = 90.0f,
            tiltDegrees = 60.0f
        )
    }

    @Test
    fun testOpenGoogleMapsSatelliteSafeExecution() {
        GoogleEarthMapsHelper.openGoogleMapsSatellite(
            context = context,
            latitude = 51.5074,
            longitude = -0.1278
        )
    }

    @Test
    fun testOpenGoogleMaps3DSafeExecution() {
        GoogleEarthMapsHelper.openGoogleMaps3D(
            context = context,
            latitude = 48.8566,
            longitude = 2.3522
        )
    }

    @Test
    fun testOpenGoogleMapsNavigationSafeExecution() {
        GoogleEarthMapsHelper.openGoogleMapsNavigation(
            context = context,
            latitude = 35.6762,
            longitude = 139.6503,
            label = "Pothole Hazard"
        )
    }

    @Test
    fun testOpenGoogleStreetViewSafeExecution() {
        GoogleEarthMapsHelper.openGoogleStreetView(
            context = context,
            latitude = 37.7749,
            longitude = -122.4194,
            headingDegrees = 180.0f
        )
    }
}
