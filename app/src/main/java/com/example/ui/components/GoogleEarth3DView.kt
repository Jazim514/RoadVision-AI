package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssistantDirection
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.RoadDefectEntity
import com.example.maps.GoogleEarthMapsHelper
import com.example.ui.RoadVisionViewModel
import com.example.ui.theme.CyanEdge
import com.example.ui.theme.HazardRed
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.RoadBorder
import com.example.ui.theme.RoadDarkSurface
import com.example.ui.theme.RoadDeepNavy
import com.example.ui.theme.RoadGreen
import java.util.Locale

enum class MapBasemapType(val id: String, val label: String) {
    GOOGLE_EARTH("google_earth", "Google Earth 3D"),
    GOOGLE_HYBRID("google_hybrid", "Maps Hybrid 3D"),
    GOOGLE_TERRAIN("google_terrain", "3D Terrain"),
    ESRI_SATELLITE("esri_satellite", "ESRI Photogrammetry")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleEarth3DView(
    viewModel: RoadVisionViewModel,
    modifier: Modifier = Modifier,
    initialBasemap: MapBasemapType = MapBasemapType.GOOGLE_EARTH
) {
    val telemetry by viewModel.telemetryState.collectAsState()
    val defects by viewModel.allDefects.collectAsState()
    val context = LocalContext.current

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var selectedBasemap by remember { mutableStateOf(initialBasemap) }
    var isFollowVehicle by remember { mutableStateOf(true) }
    var isDriverViewEnabled by remember { mutableStateOf(false) } // Rotates 3D map with car heading
    var is3DOrbitEnabled by remember { mutableStateOf(false) } // 360° drone orbit
    var cameraTiltAngle by remember { mutableFloatStateOf(62f) }
    var isPageLoading by remember { mutableStateOf(true) }
    var showQuickActionSheet by remember { mutableStateOf(false) }
    var showCameraControlsSheet by remember { mutableStateOf(false) }
    var clickedDefectId by remember { mutableStateOf<Long?>(null) }

    // Stream real-time GPS coordinates directly into 3D WebGL engine
    LaunchedEffect(telemetry.latitude, telemetry.longitude, telemetry.bearingDegrees, telemetry.speedKmh, telemetry.altitudeMeters, isFollowVehicle, isDriverViewEnabled) {
        webViewInstance?.let { webView ->
            val script = String.format(
                Locale.US,
                "if (window.updateVehicleLocation) { window.updateVehicleLocation(%.6f, %.6f, %.1f, %.1f, %.1f, %b, %b); }",
                telemetry.latitude,
                telemetry.longitude,
                telemetry.bearingDegrees,
                telemetry.speedKmh,
                telemetry.altitudeMeters,
                isFollowVehicle,
                isDriverViewEnabled
            )
            webView.evaluateJavascript(script, null)
        }
    }

    // Switch Basemap Tile Layer
    LaunchedEffect(selectedBasemap) {
        webViewInstance?.let { webView ->
            webView.evaluateJavascript("if (window.setBasemapLayer) { window.setBasemapLayer('${selectedBasemap.id}'); }", null)
        }
    }

    // Update 3D Camera tilt angle
    LaunchedEffect(cameraTiltAngle) {
        webViewInstance?.let { webView ->
            webView.evaluateJavascript("if (window.setCameraTilt) { window.setCameraTilt($cameraTiltAngle); }", null)
        }
    }

    // Toggle 360° Drone Orbit mode
    LaunchedEffect(is3DOrbitEnabled) {
        webViewInstance?.let { webView ->
            webView.evaluateJavascript("if (window.setOrbitMode) { window.setOrbitMode($is3DOrbitEnabled); }", null)
        }
    }

    // Sync defects into 3D satellite space
    LaunchedEffect(defects.size) {
        webViewInstance?.let { webView ->
            val defectsJsonArray = buildDefectsJson(defects)
            webView.evaluateJavascript("if (window.setRoadDefects) { window.setRoadDefects($defectsJsonArray); }", null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoadDeepNavy)
            .testTag("google_earth_3d_view")
    ) {
        // 1. Hardware Accelerated 3D Satellite WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowContentAccess = true
                        allowFileAccess = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isPageLoading = true
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isPageLoading = false
                            val initScript = String.format(
                                Locale.US,
                                "if (window.initEarthView) { window.initEarthView(%.6f, %.6f, %.1f, %.1f, '%s', %s); }",
                                telemetry.latitude,
                                telemetry.longitude,
                                cameraTiltAngle,
                                telemetry.bearingDegrees,
                                selectedBasemap.id,
                                buildDefectsJson(defects)
                            )
                            view?.evaluateJavascript(initScript, null)
                        }
                    }

                    // Bridge to handle hazard click events inside 3D space
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onHazardClicked(id: Long) {
                            clickedDefectId = id
                            val match = defects.find { it.id == id }
                            viewModel.selectDefect(match)
                        }
                    }, "AndroidBridge")

                    val htmlContent = generate3DSatelliteHtml(
                        initialLat = telemetry.latitude,
                        initialLng = telemetry.longitude,
                        initialBearing = telemetry.bearingDegrees,
                        initialTilt = cameraTiltAngle,
                        initialBasemap = selectedBasemap.id
                    )
                    loadDataWithBaseURL("https://earth.google.com", htmlContent, "text/html", "UTF-8", null)
                    webViewInstance = this
                }
            },
            update = {},
            modifier = Modifier.fillMaxSize()
        )

        // Loading bar
        if (isPageLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = CyanEdge,
                trackColor = Color.Transparent
            )
        }

        // 2. Top HUD: Real-time Telemetry, Basemap Switcher & Quick Launch
        Surface(
            color = RoadDarkSurface.copy(alpha = 0.94f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, RoadBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedBasemap == MapBasemapType.GOOGLE_TERRAIN) Icons.Filled.Terrain else Icons.Filled.Public,
                                contentDescription = null,
                                tint = CyanEdge,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = selectedBasemap.label.uppercase(Locale.US),
                                color = CyanEdge,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                            if (is3DOrbitEnabled) {
                                Surface(
                                    color = NeonAmber.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "360° ORBIT",
                                        color = NeonAmber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${String.format(Locale.US, "%.5f", telemetry.latitude)}, ${String.format(Locale.US, "%.5f", telemetry.longitude)} • Alt: ${telemetry.altitudeMeters.toInt()}m • ${telemetry.speedKmh.toInt()} km/h",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Launch native Google Earth or Maps Button
                    Button(
                        onClick = {
                            GoogleEarthMapsHelper.openGoogleEarth3D(
                                context = context,
                                latitude = telemetry.latitude,
                                longitude = telemetry.longitude,
                                altitudeMeters = telemetry.altitudeMeters + 350.0,
                                headingDegrees = telemetry.bearingDegrees,
                                tiltDegrees = cameraTiltAngle
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanEdge),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("launch_google_earth_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInBrowser,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "EARTH 3D",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Basemap Selector Chips (Scrollable row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MapBasemapType.values().forEach { basemap ->
                        FilterChip(
                            selected = selectedBasemap == basemap,
                            onClick = { selectedBasemap = basemap },
                            label = { Text(basemap.label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanEdge.copy(alpha = 0.3f),
                                selectedLabelColor = Color.White,
                                containerColor = RoadDarkSurface
                            )
                        )
                    }
                }
            }
        }

        // 3. Floating 3D Navigation & Camera Mode Controls (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Recenter & Follow Vehicle
            Surface(
                color = RoadDarkSurface.copy(alpha = 0.94f),
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, if (isFollowVehicle) CyanEdge else RoadBorder, CircleShape)
                    .size(44.dp)
                    .clickable {
                        isFollowVehicle = true
                        webViewInstance?.evaluateJavascript(
                            String.format(
                                Locale.US,
                                "if (window.panToVehicle) { window.panToVehicle(%.6f, %.6f); }",
                                telemetry.latitude,
                                telemetry.longitude
                            ),
                            null
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Center Vehicle",
                        tint = if (isFollowVehicle) CyanEdge else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Driver 3D View (Heading Lock)
            Surface(
                color = RoadDarkSurface.copy(alpha = 0.94f),
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, if (isDriverViewEnabled) RoadGreen else RoadBorder, CircleShape)
                    .size(44.dp)
                    .clickable {
                        isDriverViewEnabled = !isDriverViewEnabled
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = "Driver 3D View",
                        tint = if (isDriverViewEnabled) RoadGreen else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 360° Drone Orbit Mode
            Surface(
                color = RoadDarkSurface.copy(alpha = 0.94f),
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, if (is3DOrbitEnabled) NeonAmber else RoadBorder, CircleShape)
                    .size(44.dp)
                    .clickable {
                        is3DOrbitEnabled = !is3DOrbitEnabled
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.RotateRight,
                        contentDescription = "360 Drone Orbit",
                        tint = if (is3DOrbitEnabled) NeonAmber else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3D Perspective Tilt Angle Toggle / Slider Opener
            Surface(
                color = RoadDarkSurface.copy(alpha = 0.94f),
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, RoadBorder, CircleShape)
                    .size(44.dp)
                    .clickable {
                        showCameraControlsSheet = !showCameraControlsSheet
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.ViewInAr,
                        contentDescription = "3D Camera Perspective",
                        tint = CyanEdge,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Zoom In
            Surface(
                color = RoadDarkSurface.copy(alpha = 0.94f),
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, RoadBorder, CircleShape)
                    .size(44.dp)
                    .clickable {
                        webViewInstance?.evaluateJavascript("if (window.zoomIn) { window.zoomIn(); }", null)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Zoom In",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Zoom Out
            Surface(
                color = RoadDarkSurface.copy(alpha = 0.94f),
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, RoadBorder, CircleShape)
                    .size(44.dp)
                    .clickable {
                        webViewInstance?.evaluateJavascript("if (window.zoomOut) { window.zoomOut(); }", null)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Google Maps & Earth Quick Menu
            Surface(
                color = RoadDarkSurface.copy(alpha = 0.94f),
                shape = CircleShape,
                modifier = Modifier
                    .border(1.dp, RoadBorder, CircleShape)
                    .size(44.dp)
                    .clickable { showQuickActionSheet = !showQuickActionSheet }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = "Google Maps Menu",
                        tint = RoadGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 4. Camera 3D Perspective Adjuster Dialog/Card
        AnimatedVisibility(
            visible = showCameraControlsSheet,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RoadDarkSurface.copy(alpha = 0.98f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanEdge, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3D CAMERA PERSPECTIVE",
                            color = CyanEdge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        IconButton(onClick = { showCameraControlsSheet = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Text(
                        text = "Pitch Tilt Angle: ${cameraTiltAngle.toInt()}°",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Slider(
                        value = cameraTiltAngle,
                        onValueChange = { cameraTiltAngle = it },
                        valueRange = 0f..75f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanEdge,
                            activeTrackColor = CyanEdge,
                            inactiveTrackColor = RoadBorder
                        )
                    )

                    // Quick Pitch presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { cameraTiltAngle = 0f },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("0° 2D", fontSize = 10.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { cameraTiltAngle = 45f },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("45° Drone", fontSize = 10.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { cameraTiltAngle = 65f },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("65° Horizon", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    // Altitude / Zoom Presets
                    Text(
                        text = "Camera Altitude Presets",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                webViewInstance?.evaluateJavascript("if (window.setZoomLevel) { window.setZoomLevel(19); }", null)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("50m Road", fontSize = 10.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                webViewInstance?.evaluateJavascript("if (window.setZoomLevel) { window.setZoomLevel(17); }", null)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("250m Aerial", fontSize = 10.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                webViewInstance?.evaluateJavascript("if (window.setZoomLevel) { window.setZoomLevel(14); }", null)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("1000m City", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // 5. Google Maps & Earth Quick Actions Bottom Floating Sheet
        AnimatedVisibility(
            visible = showQuickActionSheet,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RoadDarkSurface.copy(alpha = 0.96f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanEdge, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GOOGLE MAPS & EARTH TELEMATICS",
                            color = CyanEdge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        IconButton(onClick = { showQuickActionSheet = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Google Maps Satellite 3D
                        OutlinedButton(
                            onClick = {
                                GoogleEarthMapsHelper.openGoogleMapsSatellite(
                                    context = context,
                                    latitude = telemetry.latitude,
                                    longitude = telemetry.longitude
                                )
                                showQuickActionSheet = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Layers, contentDescription = null, tint = CyanEdge, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Maps 3D", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Turn-by-Turn Navigation
                        OutlinedButton(
                            onClick = {
                                GoogleEarthMapsHelper.openGoogleMapsNavigation(
                                    context = context,
                                    latitude = telemetry.latitude,
                                    longitude = telemetry.longitude,
                                    label = "Vehicle Location"
                                )
                                showQuickActionSheet = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.AssistantDirection, contentDescription = null, tint = RoadGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Navigate", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Google Street View 360°
                        OutlinedButton(
                            onClick = {
                                GoogleEarthMapsHelper.openGoogleStreetView(
                                    context = context,
                                    latitude = telemetry.latitude,
                                    longitude = telemetry.longitude,
                                    headingDegrees = telemetry.bearingDegrees
                                )
                                showQuickActionSheet = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Streetview, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Street View", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}

/**
 * Builds JSON string representation of defects for 3D marker injection
 */
private fun buildDefectsJson(defects: List<RoadDefectEntity>): String {
    val sb = StringBuilder("[")
    defects.take(200).forEachIndexed { i, d ->
        if (i > 0) sb.append(",")
        sb.append("{")
        sb.append("\"id\":").append(d.id).append(",")
        sb.append("\"lat\":").append(d.latitude).append(",")
        sb.append("\"lng\":").append(d.longitude).append(",")
        sb.append("\"type\":\"").append(d.defectType.replace("\"", "")).append("\",")
        sb.append("\"severity\":\"").append(d.severity).append("\",")
        sb.append("\"depthCm\":").append(d.depthCm).append(",")
        sb.append("\"gForce\":").append(d.peakGForce)
        sb.append("}")
    }
    sb.append("]")
    return sb.toString()
}

/**
 * Generates an embedded, hardware-accelerated 3D Satellite & Aerial Perspective WebGL visualization
 * featuring Google Maps / Google Earth tile layers, dynamic 3D perspective tilt,
 * heading-aligned vehicle follow mode, 360° drone orbit, holographic hazard markers,
 * and real-time telemetry polyline path.
 */
private fun generate3DSatelliteHtml(
    initialLat: Double,
    initialLng: Double,
    initialBearing: Float,
    initialTilt: Float,
    initialBasemap: String
): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Google Earth 3D Visualizer</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { width: 100%; height: 100%; overflow: hidden; background: #090D16; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        
        #map-container {
            width: 100%;
            height: 100%;
            position: absolute;
            perspective: 900px;
            transform-style: preserve-3d;
            background: #090D16;
            overflow: hidden;
        }

        #map {
            width: 100%;
            height: 120%;
            position: absolute;
            top: -10%;
            left: 0;
            transform-origin: center center;
            transition: transform 0.3s ease-out;
            background: #090D16;
        }

        /* 3D Vehicle Marker with Heading Beam */
        .vehicle-marker {
            width: 44px;
            height: 44px;
            position: relative;
            transform-origin: center center;
            transition: transform 0.2s linear;
        }
        .vehicle-beam {
            width: 0;
            height: 0;
            border-left: 18px solid transparent;
            border-right: 18px solid transparent;
            border-bottom: 55px solid rgba(0, 229, 255, 0.45);
            position: absolute;
            top: -30px;
            left: 4px;
            filter: drop-shadow(0 0 12px #00E5FF);
            pointer-events: none;
        }
        .vehicle-cone {
            width: 0;
            height: 0;
            border-left: 10px solid transparent;
            border-right: 10px solid transparent;
            border-bottom: 24px solid #00E5FF;
            position: absolute;
            top: 10px;
            left: 12px;
            filter: drop-shadow(0 0 8px #00E5FF);
        }
        .vehicle-pulse {
            width: 44px;
            height: 44px;
            border-radius: 50%;
            background: rgba(0, 229, 255, 0.25);
            border: 2px solid #00E5FF;
            position: absolute;
            top: 0;
            left: 0;
            animation: pulse 1.8s infinite;
        }
        .vehicle-badge {
            position: absolute;
            bottom: -18px;
            left: 50%;
            transform: translateX(-50%);
            background: rgba(15, 23, 42, 0.9);
            color: #00E5FF;
            font-size: 9px;
            font-weight: 800;
            padding: 1px 4px;
            border-radius: 4px;
            border: 1px solid #00E5FF;
            white-space: nowrap;
        }

        /* 3D Holographic Hazard Markers */
        .hazard-pin {
            width: 26px;
            height: 26px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 13px;
            font-weight: bold;
            color: #fff;
            box-shadow: 0 0 14px rgba(255, 23, 68, 0.95);
            border: 2px solid #fff;
            transform: translateZ(20px);
            cursor: pointer;
        }
        .hazard-pothole { background: #FF1744; }
        .hazard-crack { background: #FF9100; }
        .hazard-shadow {
            width: 26px;
            height: 10px;
            border-radius: 50%;
            background: rgba(0,0,0,0.6);
            position: absolute;
            bottom: -6px;
            left: 0;
            filter: blur(2px);
        }

        @keyframes pulse {
            0% { transform: scale(0.6); opacity: 1; }
            100% { transform: scale(1.6); opacity: 0; }
        }

        /* Compass Rose HUD */
        .compass-hud {
            position: absolute;
            bottom: 24px;
            left: 20px;
            width: 46px;
            height: 46px;
            border-radius: 50%;
            background: rgba(15, 23, 42, 0.88);
            border: 1.5px solid #00E5FF;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            color: #00E5FF;
            font-weight: 900;
            font-size: 11px;
            box-shadow: 0 4px 14px rgba(0,0,0,0.6);
            z-index: 1000;
            pointer-events: none;
        }
        .compass-arrow {
            width: 0;
            height: 0;
            border-left: 4px solid transparent;
            border-right: 4px solid transparent;
            border-bottom: 8px solid #FF1744;
            margin-bottom: 2px;
        }

        .leaflet-control-attribution { display: none !important; }
    </style>
</head>
<body>
    <div id="map-container">
        <div id="map"></div>
    </div>
    <div class="compass-hud" id="compass-container">
        <div class="compass-arrow"></div>
        <div id="compass-label">0°</div>
    </div>

    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        var map, vehicleMarker, breadcrumbPolyline;
        var breadcrumbPoints = [];
        var currentTilt = ${initialTilt};
        var currentRotation = 0;
        var hazardLayerGroup;
        var activeTileLayer = null;
        var isOrbiting = false;
        var orbitInterval = null;
        var orbitAngle = 0;
        var currentLat = ${initialLat};
        var currentLng = ${initialLng};
        var currentBearing = ${initialBearing};

        // Available Google Earth & Google Maps tile servers
        var tileConfigs = {
            google_earth: {
                url: 'https://mt{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}',
                subdomains: ['0', '1', '2', '3'],
                maxZoom: 20
            },
            google_hybrid: {
                url: 'https://mt{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}',
                subdomains: ['0', '1', '2', '3'],
                maxZoom: 20
            },
            google_terrain: {
                url: 'https://mt{s}.google.com/vt/lyrs=p&x={x}&y={y}&z={z}',
                subdomains: ['0', '1', '2', '3'],
                maxZoom: 20
            },
            esri_satellite: {
                url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
                subdomains: [''],
                maxZoom: 20
            }
        };

        function setBasemapLayer(layerId) {
            var config = tileConfigs[layerId] || tileConfigs['google_earth'];
            if (activeTileLayer) {
                map.removeLayer(activeTileLayer);
            }
            activeTileLayer = L.tileLayer(config.url, {
                subdomains: config.subdomains,
                maxZoom: config.maxZoom,
                maxNativeZoom: 19
            }).addTo(map);
        }

        function initEarthView(lat, lng, tilt, bearing, basemapId, defects) {
            if (typeof L === 'undefined') {
                setTimeout(function() {
                    initEarthView(lat, lng, tilt, bearing, basemapId, defects);
                }, 150);
                return;
            }
            if (map) {
                currentLat = lat;
                currentLng = lng;
                currentTilt = tilt;
                currentBearing = parseFloat(bearing) || 0;
                apply3DTransform();
                if (defects && defects.length > 0) setRoadDefects(defects);
                return;
            }

            currentLat = lat;
            currentLng = lng;
            currentTilt = tilt;
            currentBearing = parseFloat(bearing) || 0;
            
            map = L.map('map', {
                center: [lat, lng],
                zoom: 18,
                zoomControl: false,
                attributionControl: false
            });

            // Set chosen basemap
            setBasemapLayer(basemapId);

            // Layer for road hazard pins
            hazardLayerGroup = L.layerGroup().addTo(map);

            // Real-time Trajectory polyline
            breadcrumbPolyline = L.polyline([], {
                color: '#00E5FF',
                weight: 5,
                opacity: 0.95,
                dashArray: '6, 8'
            }).addTo(map);

            // Create 3D Vehicle Marker with forward beam & speedometer badge
            var vehicleHtml = '<div class="vehicle-marker" id="v-marker">' +
                              '<div class="vehicle-beam"></div>' +
                              '<div class="vehicle-pulse"></div>' +
                              '<div class="vehicle-cone"></div>' +
                              '<div class="vehicle-badge" id="v-speed">0 km/h</div>' +
                              '</div>';
            var vehicleIcon = L.divIcon({
                html: vehicleHtml,
                className: '',
                iconSize: [44, 44],
                iconAnchor: [22, 22]
            });
            vehicleMarker = L.marker([lat, lng], { icon: vehicleIcon }).addTo(map);

            // Apply 3D perspective tilt
            apply3DTransform();

            // Render defects
            if (defects && defects.length > 0) {
                setRoadDefects(defects);
            }
        }

        function apply3DTransform() {
            var mapEl = document.getElementById('map');
            if (mapEl) {
                mapEl.style.transform = 'rotateX(' + currentTilt + 'deg) rotateZ(' + currentRotation + 'deg)';
            }
        }

        // Live location update from Kotlin
        function updateVehicleLocation(lat, lng, bearing, speedKmh, altMeters, follow, driverHeading) {
            if (!map || !vehicleMarker) return;

            currentLat = lat;
            currentLng = lng;
            currentBearing = bearing;
            var newLatLng = L.latLng(lat, lng);
            vehicleMarker.setLatLng(newLatLng);

            // Rotate vehicle marker to match bearing
            var markerEl = document.getElementById('v-marker');
            if (markerEl) {
                markerEl.style.transform = 'rotate(' + bearing + 'deg)';
            }

            // Update vehicle speed badge
            var speedEl = document.getElementById('v-speed');
            if (speedEl) {
                speedEl.innerText = Math.round(speedKmh) + ' km/h';
            }

            // Update compass
            var compass = document.getElementById('compass-label');
            if (compass) {
                compass.innerText = Math.round(bearing) + '°';
            }

            // Record trajectory breadcrumb
            breadcrumbPoints.push(newLatLng);
            if (breadcrumbPoints.length > 250) breadcrumbPoints.shift();
            breadcrumbPolyline.setLatLngs(breadcrumbPoints);

            // Driver 3D View: Rotate camera plane with heading
            if (driverHeading && !isOrbiting) {
                currentRotation = -bearing;
                apply3DTransform();
            } else if (!isOrbiting && currentRotation !== 0) {
                currentRotation = 0;
                apply3DTransform();
            }

            if (follow && !isOrbiting) {
                map.panTo(newLatLng, { animate: true, duration: 0.4 });
            }
        }

        // Set camera tilt angle (0 to 75 deg)
        function setCameraTilt(angle) {
            currentTilt = angle;
            apply3DTransform();
        }

        // Toggle 360° Drone Orbit mode
        function setOrbitMode(enabled) {
            isOrbiting = enabled;
            if (isOrbiting) {
                if (orbitInterval) clearInterval(orbitInterval);
                orbitInterval = setInterval(function() {
                    orbitAngle = (orbitAngle + 1.5) % 360;
                    currentRotation = orbitAngle;
                    apply3DTransform();
                }, 50);
            } else {
                if (orbitInterval) clearInterval(orbitInterval);
                orbitInterval = null;
                currentRotation = 0;
                apply3DTransform();
            }
        }

        // Render Road Defects in 3D Satellite Space
        function setRoadDefects(defects) {
            if (!hazardLayerGroup) return;
            hazardLayerGroup.clearLayers();

            defects.forEach(function(d) {
                var isPothole = d.type.indexOf('POTHOLE') !== -1;
                var className = isPothole ? 'hazard-pothole' : 'hazard-crack';
                var iconSymbol = isPothole ? '⚠️' : '⚡';

                var html = '<div class="hazard-shadow"></div>' +
                           '<div class="hazard-pin ' + className + '" onclick="window.notifyHazardClick(' + d.id + ')">' + 
                           iconSymbol + 
                           '</div>';
                var icon = L.divIcon({
                    html: html,
                    className: '',
                    iconSize: [26, 26],
                    iconAnchor: [13, 13]
                });

                var marker = L.marker([d.lat, d.lng], { icon: icon });
                marker.bindPopup(
                    '<div style="color:#090D16; font-size:12px; font-weight:bold; font-family:sans-serif;">' +
                    d.type + ' [' + d.severity + ']<br>' +
                    'Depth: ' + d.depthCm.toFixed(1) + 'cm • Impact: ' + d.gForce.toFixed(2) + 'G' +
                    '</div>'
                );
                marker.on('click', function() {
                    window.notifyHazardClick(d.id);
                });
                hazardLayerGroup.addLayer(marker);
            });
        }

        function notifyHazardClick(id) {
            if (window.AndroidBridge && window.AndroidBridge.onHazardClicked) {
                window.AndroidBridge.onHazardClicked(id);
            }
        }

        function zoomIn() { if (map) map.zoomIn(); }
        function zoomOut() { if (map) map.zoomOut(); }
        function setZoomLevel(z) { if (map) map.setZoom(z); }
        function panToVehicle(lat, lng) { if (map) map.setView([lat, lng], 18, { animate: true }); }
    </script>
</body>
</html>
    """.trimIndent()
}
