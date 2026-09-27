package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveRoadMap(
    viewModel: RoadVisionViewModel,
    modifier: Modifier = Modifier
) {
    val defects by viewModel.allDefects.collectAsState()
    val breadcrumbs by viewModel.allBreadcrumbs.collectAsState()
    val telemetry by viewModel.telemetryState.collectAsState()
    val selectedDefect by viewModel.selectedDefect.collectAsState()

    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var showOnlySevere by remember { mutableStateOf(false) }
    var showHeatmapTrails by remember { mutableStateOf(true) }
    var activeMapMode by remember { mutableIntStateOf(1) } // 0 = 2D GIS Radar, 1 = Google Earth 3D, 2 = Google Maps 3D

    val filteredDefects = remember(defects, showOnlySevere) {
        if (showOnlySevere) {
            defects.filter { it.severity == "SEVERE" || it.severity == "CRITICAL" }
        } else {
            defects
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoadDeepNavy)
            .testTag("interactive_road_map")
    ) {
        when (activeMapMode) {
            1 -> {
                // Google Earth 3D Photogrammetric Satellite Visualizer
                GoogleEarth3DView(
                    viewModel = viewModel,
                    initialBasemap = MapBasemapType.GOOGLE_EARTH,
                    modifier = Modifier.fillMaxSize()
                )
            }
            2 -> {
                // Google Maps 3D Hybrid Satellite & Road Network
                GoogleEarth3DView(
                    viewModel = viewModel,
                    initialBasemap = MapBasemapType.GOOGLE_HYBRID,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // 1. Custom GIS Canvas Map
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                    .pointerInput(filteredDefects, zoomLevel, telemetry) {
                        detectTapGestures { tapOffset ->
                            val w = size.width
                            val h = size.height
                            val cx = w / 2f
                            val cy = h / 2f
                            val scale = 36000f * zoomLevel

                            // Find closest defect within 32dp tap radius
                            var clicked: RoadDefectEntity? = null
                            var minDistanceSq = 36f * 36f

                            filteredDefects.forEach { defect ->
                                val dx = (defect.longitude - telemetry.longitude).toFloat() * scale
                                val dy = -(defect.latitude - telemetry.latitude).toFloat() * scale
                                val px = cx + dx
                                val py = cy + dy
                                val distSq = (tapOffset.x - px) * (tapOffset.x - px) + (tapOffset.y - py) * (tapOffset.y - py)
                                if (distSq < minDistanceSq) {
                                    minDistanceSq = distSq
                                    clicked = defect
                                }
                            }
                            viewModel.selectDefect(clicked)
                        }
                    }
            ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val scale = 36000f * zoomLevel

            // Draw GIS coordinate grid lines
            val gridSize = 60.dp.toPx()
            for (x in 0..(w / gridSize).toInt() + 1) {
                drawLine(
                    color = Color(0x1500E5FF),
                    start = Offset(x * gridSize, 0f),
                    end = Offset(x * gridSize, h),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(h / gridSize).toInt() + 1) {
                drawLine(
                    color = Color(0x1500E5FF),
                    start = Offset(0f, y * gridSize),
                    end = Offset(w, y * gridSize),
                    strokeWidth = 1f
                )
            }

            // Draw Survey Route Trajectory (Breadcrumbs)
            if (showHeatmapTrails && breadcrumbs.size >= 2) {
                val sorted = breadcrumbs.sortedBy { it.timestamp }
                for (i in 0 until sorted.size - 1) {
                    val b1 = sorted[i]
                    val b2 = sorted[i + 1]

                    val p1x = cx + (b1.longitude - telemetry.longitude).toFloat() * scale
                    val p1y = cy - (b1.latitude - telemetry.latitude).toFloat() * scale
                    val p2x = cx + (b2.longitude - telemetry.longitude).toFloat() * scale
                    val p2y = cy - (b2.latitude - telemetry.latitude).toFloat() * scale

                    val segmentColor = when {
                        b1.roughnessIri > 3.8f -> HazardRed
                        b1.roughnessIri > 2.4f -> NeonAmber
                        else -> RoadGreen
                    }

                    drawLine(
                        color = segmentColor.copy(alpha = 0.85f),
                        start = Offset(p1x, p1y),
                        end = Offset(p2x, p2y),
                        strokeWidth = 4.dp.toPx()
                    )
                }
            }

            // Draw Pothole & Road Hazard Pins
            filteredDefects.forEach { defect ->
                val px = cx + (defect.longitude - telemetry.longitude).toFloat() * scale
                val py = cy - (defect.latitude - telemetry.latitude).toFloat() * scale

                val markerColor = when (defect.severity) {
                    "CRITICAL" -> HazardRed
                    "SEVERE" -> HazardRed
                    "MEDIUM" -> NeonAmber
                    else -> CyanEdge
                }

                // Outer pulsating halo
                drawCircle(
                    color = markerColor.copy(alpha = 0.25f),
                    radius = 16.dp.toPx(),
                    center = Offset(px, py)
                )
                // Inner solid pin
                drawCircle(
                    color = markerColor,
                    radius = 8.dp.toPx(),
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // Center Vehicle Radar & Indicator
            // Radar pulse ring
            val pulseR = 38.dp.toPx()
            drawCircle(
                color = CyanEdge.copy(alpha = 0.20f),
                radius = pulseR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = CyanEdge,
                radius = pulseR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Vehicle heading arrow
            val rad = Math.toRadians((telemetry.bearingDegrees - 90.0))
            val arrowLength = 22.dp.toPx()
            val tipX = cx + (arrowLength * cos(rad)).toFloat()
            val tipY = cy + (arrowLength * sin(rad)).toFloat()

            drawLine(
                color = CyanEdge,
                start = Offset(cx, cy),
                end = Offset(tipX, tipY),
                strokeWidth = 3.dp.toPx()
            )
            drawCircle(
                color = CyanEdge,
                radius = 7.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
        }
        }

        // 2. Top GIS Layer Controls Bar
        Surface(
            color = RoadDarkSurface.copy(alpha = 0.92f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, RoadBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Map Mode Switcher Tabs
                TabRow(
                    selectedTabIndex = activeMapMode,
                    containerColor = Color.Transparent,
                    contentColor = CyanEdge,
                    divider = {},
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Tab(
                        selected = activeMapMode == 0,
                        onClick = { activeMapMode = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(13.dp))
                                Text("2D Radar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = activeMapMode == 1,
                        onClick = { activeMapMode = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(13.dp))
                                Text("Earth 3D", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = activeMapMode == 2,
                        onClick = { activeMapMode = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(13.dp))
                                Text("Maps 3D", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (activeMapMode) {
                                1 -> "GOOGLE EARTH 3D • SATELLITE"
                                2 -> "GOOGLE MAPS 3D • HYBRID"
                                else -> "CENTRAL MAPPING BACKEND"
                            },
                            color = CyanEdge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "GPS: ${String.format(Locale.US, "%.5f", telemetry.latitude)}, ${String.format(Locale.US, "%.5f", telemetry.longitude)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Hazard count badge
                    Surface(
                        color = HazardRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, HazardRed, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "${filteredDefects.size} Hazards Logged",
                            color = HazardRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (activeMapMode == 0) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = showOnlySevere,
                            onClick = { showOnlySevere = !showOnlySevere },
                            label = { Text("Severe Only", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HazardRed.copy(alpha = 0.3f),
                                selectedLabelColor = Color.White
                            )
                        )

                        FilterChip(
                            selected = showHeatmapTrails,
                            onClick = { showHeatmapTrails = !showHeatmapTrails },
                            label = { Text("IRI Heatmap Route", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanEdge.copy(alpha = 0.3f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // 3. Map Zoom & Re-Center Floating Controls (2D radar mode)
        if (activeMapMode == 0) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = RoadDarkSurface.copy(alpha = 0.9f),
                    shape = CircleShape,
                    modifier = Modifier
                        .border(1.dp, RoadBorder, CircleShape)
                        .size(44.dp)
                        .clickable { zoomLevel = (zoomLevel * 1.3f).coerceAtMost(3.0f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Zoom In", tint = Color.White)
                    }
                }

                Surface(
                    color = RoadDarkSurface.copy(alpha = 0.9f),
                    shape = CircleShape,
                    modifier = Modifier
                        .border(1.dp, RoadBorder, CircleShape)
                        .size(44.dp)
                        .clickable { zoomLevel = (zoomLevel / 1.3f).coerceAtLeast(0.4f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", tint = Color.White)
                    }
                }

                Surface(
                    color = RoadDarkSurface.copy(alpha = 0.9f),
                    shape = CircleShape,
                    modifier = Modifier
                        .border(1.dp, RoadBorder, CircleShape)
                        .size(44.dp)
                        .clickable { zoomLevel = 1.0f }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Re-Center", tint = CyanEdge)
                    }
                }
            }
        }

        // 4. Selected Defect Inspector Card Popup
        selectedDefect?.let { defect ->
            DefectInspectorCard(
                defect = defect,
                onDismiss = { viewModel.selectDefect(null) },
                onDelete = { viewModel.deleteDefect(defect.id) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun DefectInspectorCard(
    defect: RoadDefectEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, if (defect.severity == "CRITICAL") HazardRed else NeonAmber, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (defect.defectType == "POTHOLE") "⚠️" else "⚡",
                        fontSize = 20.sp
                    )
                    Column {
                        Text(
                            text = "${defect.defectType.replace("_", " ")} [${defect.severity}]",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = defect.streetName,
                            color = CyanEdge,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(title = "DEPTH", value = "${String.format("%.1f", defect.depthCm)} cm")
                MetricColumn(title = "G-FORCE IMPACT", value = "${String.format("%.2f", defect.peakGForce)} G")
                MetricColumn(title = "SPEED", value = "${defect.vehicleSpeedKmh.toInt()} km/h")
                MetricColumn(title = "CONFIDENCE", value = "${(defect.confidence * 100).toInt()}%")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Google Earth / Maps Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        GoogleEarthMapsHelper.openGoogleEarth3D(
                            context = context,
                            latitude = defect.latitude,
                            longitude = defect.longitude,
                            altitudeMeters = 250.0
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = CyanEdge, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Earth 3D", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        GoogleEarthMapsHelper.openGoogleMapsNavigation(
                            context = context,
                            latitude = defect.latitude,
                            longitude = defect.longitude,
                            label = defect.defectType
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, tint = RoadGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Maps Nav", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        GoogleEarthMapsHelper.openGoogleStreetView(
                            context = context,
                            latitude = defect.latitude,
                            longitude = defect.longitude
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Streetview, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Street View", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GPS: ${String.format(Locale.US, "%.5f", defect.latitude)}, ${String.format(Locale.US, "%.5f", defect.longitude)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Surface(
                    color = if (defect.syncStatus == "SYNCED") RoadGreen.copy(alpha = 0.2f) else NeonAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = defect.syncStatus,
                        color = if (defect.syncStatus == "SYNCED") RoadGreen else NeonAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(title: String, value: String) {
    Column {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
