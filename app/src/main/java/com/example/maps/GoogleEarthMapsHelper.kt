package com.example.maps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale

object GoogleEarthMapsHelper {

    /**
     * Generates a web URL for Google Earth 3D camera view.
     */
    fun generateGoogleEarthWebUrl(
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double = 350.0,
        headingDegrees: Float = 0.0f,
        tiltDegrees: Float = 65.0f
    ): String {
        return String.format(
            Locale.US,
            "https://earth.google.com/web/@%.6f,%.6f,%.1fa,600d,35y,%.1fh,%.1ft,0r",
            latitude,
            longitude,
            altitudeMeters,
            headingDegrees,
            tiltDegrees
        )
    }

    /**
     * Generates a web URL for Google Maps 3D satellite view.
     */
    fun generateGoogleMapsWebUrl(
        latitude: Double,
        longitude: Double,
        zoom: Int = 18
    ): String {
        return String.format(
            Locale.US,
            "https://www.google.com/maps/@%.6f,%.6f,%dz/data=!3m1!1e3",
            latitude,
            longitude,
            zoom
        )
    }

    /**
     * Opens Google Earth 3D view at the specific latitude, longitude, altitude, heading, and tilt.
     * Tries the native Google Earth application first; falls back to Google Earth Web in browser.
     */
    fun openGoogleEarth3D(
        context: Context,
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double = 350.0,
        headingDegrees: Float = 0.0f,
        tiltDegrees: Float = 65.0f
    ) {
        val earthWebUrl = generateGoogleEarthWebUrl(
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = altitudeMeters,
            headingDegrees = headingDegrees,
            tiltDegrees = tiltDegrees
        )

        try {
            // Attempt opening via native Google Earth app
            val earthAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse(earthWebUrl)).apply {
                setPackage("com.google.earth")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(earthAppIntent)
        } catch (e: Exception) {
            // Fallback: Open in web browser / any handler
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(earthWebUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "Could not open Google Earth: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Opens Google Maps in 3D satellite imagery mode centered at the coordinate.
     */
    fun openGoogleMapsSatellite(
        context: Context,
        latitude: Double,
        longitude: Double,
        zoom: Int = 18
    ) {
        val mapsSatelliteUrl = generateGoogleMapsWebUrl(latitude, longitude, zoom)

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsSatelliteUrl)).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(mapsSatelliteUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (err: Exception) {
                Toast.makeText(context, "Could not open Google Maps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Opens Google Maps in 3D perspective mode with building photogrammetry.
     */
    fun openGoogleMaps3D(
        context: Context,
        latitude: Double,
        longitude: Double,
        headingDegrees: Float = 0.0f,
        tiltDegrees: Float = 60.0f
    ) {
        // Google Maps 3D mode with tilt and heading
        val url = String.format(
            Locale.US,
            "https://www.google.com/maps/@%.6f,%.6f,200m/data=!3m2!1e3!4b1!4m2!1m1!4e2?entry=ttu",
            latitude,
            longitude
        )

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openGoogleMapsSatellite(context, latitude, longitude, zoom = 19)
        }
    }

    /**
     * Launches Google Maps Navigation directly to the road hazard or waypoint.
     */
    fun openGoogleMapsNavigation(
        context: Context,
        latitude: Double,
        longitude: Double,
        label: String = "Road Hazard"
    ) {
        val navUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(navIntent)
        } catch (e: Exception) {
            // Fallback to geo: URI scheme
            val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
            val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(geoIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "No maps app installed to handle navigation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Opens Google Street View 360° visual inspection for the exact pothole coordinate.
     */
    fun openGoogleStreetView(
        context: Context,
        latitude: Double,
        longitude: Double,
        headingDegrees: Float = 0.0f
    ) {
        val streetViewUri = Uri.parse("google.streetview:cbll=$latitude,$longitude&cbp=1,$headingDegrees,,0,1")
        val streetViewIntent = Intent(Intent.ACTION_VIEW, streetViewUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(streetViewIntent)
        } catch (e: Exception) {
            // Web fallback
            val webUrl = String.format(
                Locale.US,
                "https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=%.6f,%.6f&heading=%.1f",
                latitude,
                longitude,
                headingDegrees
            )
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "Street View unavailable on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
