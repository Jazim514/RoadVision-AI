package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sync.SyncState
import com.example.ui.RoadVisionViewModel
import com.example.ui.theme.CyanEdge
import com.example.ui.theme.HazardRed
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.RoadBorder
import com.example.ui.theme.RoadDarkSurface
import com.example.ui.theme.RoadDeepNavy
import com.example.ui.theme.RoadGreen

@Composable
fun SyncSettingsView(
    viewModel: RoadVisionViewModel,
    modifier: Modifier = Modifier
) {
    val pendingCount by viewModel.pendingSyncCount.collectAsState()
    val totalCount by viewModel.totalDefectCount.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val exportData by viewModel.exportContent.collectAsState()
    val persistedGpsCount by viewModel.persistedGpsCount.collectAsState()
    val latestGpsLocation by viewModel.latestGpsLocation.collectAsState()
    val isGpsStreaming by viewModel.isGpsStreaming.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var endpointUrl by remember { mutableStateOf(viewModel.backendClient.backendEndpointUrl) }
    var confidenceSlider by remember { mutableFloatStateOf(viewModel.edgeDetector.confidenceThreshold) }
    var bumpGSlider by remember { mutableFloatStateOf(viewModel.sensorEngine.bumpThresholdG) }
    var nnapiEnabled by remember { mutableStateOf(viewModel.edgeDetector.isNNAPIEnabled) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoadDeepNavy)
            .testTag("sync_settings_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header
            Text(
                text = "EDGE ARCHITECTURE & SYNC",
                color = CyanEdge,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = "Telemetry Backend & Edge AI Settings",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )

            // Section 1: Central Mapping Backend Synchronization
            Card(
                colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RoadBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudSync, contentDescription = null, tint = CyanEdge, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Central Mapping Sync Engine",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Endpoint config field
                    OutlinedTextField(
                        value = endpointUrl,
                        onValueChange = {
                            endpointUrl = it
                            viewModel.backendClient.backendEndpointUrl = it
                        },
                        label = { Text("GIS Telemetry Ingestion API", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanEdge,
                            unfocusedBorderColor = RoadBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = CyanEdge
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Queue status bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Local Sync Buffer Queue",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$pendingCount pending of $totalCount logged",
                                color = if (pendingCount > 0) NeonAmber else RoadGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.triggerBackendSync() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanEdge),
                            shape = RoundedCornerShape(10.dp),
                            enabled = syncState !is SyncState.Syncing,
                            modifier = Modifier.testTag("sync_now_button")
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SYNC NOW", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }

                    // Sync state indicators
                    when (val state = syncState) {
                        is SyncState.Syncing -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Uploading batch to GIS mapping server (${state.itemsUploaded}/${state.totalItems})...",
                                    color = CyanEdge,
                                    fontSize = 11.sp
                                )
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CyanEdge,
                                    trackColor = RoadBorder
                                )
                            }
                        }
                        is SyncState.Success -> {
                            Text(
                                text = "✓ Sync successful: ${state.itemsSynced} records transmitted to central map",
                                color = RoadGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is SyncState.Error -> {
                            Text(
                                text = "✕ ${state.message}",
                                color = HazardRed,
                                fontSize = 12.sp
                            )
                        }
                        else -> {}
                    }
                }
            }

            // Section 2: Municipal GIS Data Exports
            Card(
                colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RoadBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Data Export for Urban Maintenance & GIS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Export road condition telematics to RFC 7946 GeoJSON for QGIS/ArcGIS or engineering CSV spreadsheets.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.exportGeoJson() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export GeoJSON", color = CyanEdge, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.exportCsv() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export CSV", color = CyanEdge, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section 2.5: Continuous GPS Telemetry Provider (FusedLocationProviderClient + Room)
            Card(
                colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RoadBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Continuous GPS Stream & Room Persistence",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "FusedLocationProviderClient continuous coordinate stream",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            color = if (isGpsStreaming) RoadGreen.copy(alpha = 0.2f) else NeonAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(1.dp, if (isGpsStreaming) RoadGreen else NeonAmber, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = if (isGpsStreaming) "STREAMING" else "IDLE",
                                color = if (isGpsStreaming) RoadGreen else NeonAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Persisted GPS Records: $persistedGpsCount",
                                color = CyanEdge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (latestGpsLocation != null) {
                                val loc = latestGpsLocation!!
                                Text(
                                    text = "Lat: ${String.format("%.5f", loc.latitude)}, Lng: ${String.format("%.5f", loc.longitude)}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Speed: ${String.format("%.1f", loc.speedKmh)} km/h • ±${String.format("%.1f", loc.accuracyMeters)}m",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            } else {
                                Text(
                                    text = "No real GPS fix yet (simulated navigation active)",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isGpsStreaming) {
                                    viewModel.locationManager.stopContinuousTracking()
                                } else {
                                    if (viewModel.locationManager.hasLocationPermission()) {
                                        viewModel.locationManager.startContinuousTracking(1000L)
                                        Toast.makeText(context, "Started GPS stream & Room persistence", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Location permission required", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isGpsStreaming) HazardRed else CyanEdge
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isGpsStreaming) "STOP GPS" else "START GPS",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Section 2.5: Google Earth & Google Maps 3D Integration
            val telemetryState by viewModel.telemetryState.collectAsState()
            Card(
                colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanEdge.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = null,
                                tint = CyanEdge,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Google Earth & Maps 3D Engine",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Real-time 3D photogrammetry & satellite sync",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            color = RoadGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "CONNECTED",
                                color = RoadGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Telemetry Sync coordinates
                    Surface(
                        color = Color(0xFF090D14),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("STREAMING COORDINATE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                Text("ALTITUDE / SPEED", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.5f", telemetryState.latitude)}, ${String.format(java.util.Locale.US, "%.5f", telemetryState.longitude)}",
                                    color = CyanEdge,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${telemetryState.altitudeMeters.toInt()}m • ${telemetryState.speedKmh.toInt()} km/h",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Test launch action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                com.example.maps.GoogleEarthMapsHelper.openGoogleEarth3D(
                                    context = context,
                                    latitude = telemetryState.latitude,
                                    longitude = telemetryState.longitude,
                                    altitudeMeters = telemetryState.altitudeMeters + 350.0,
                                    headingDegrees = telemetryState.bearingDegrees,
                                    tiltDegrees = 65f
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = null,
                                tint = CyanEdge,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Earth 3D", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                com.example.maps.GoogleEarthMapsHelper.openGoogleMapsSatellite(
                                    context = context,
                                    latitude = telemetryState.latitude,
                                    longitude = telemetryState.longitude
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Map,
                                contentDescription = null,
                                tint = RoadGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Maps 3D", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section 3: Edge AI Vision & Sensor Calibration
            Card(
                colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RoadBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Memory, contentDescription = null, tint = CyanEdge, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Edge AI & Sensor Thresholds",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // NNAPI toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NNAPI Hardware Acceleration", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Target Edge NPU / TPU silicon for 30+ FPS", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = nnapiEnabled,
                            onCheckedChange = {
                                nnapiEnabled = it
                                viewModel.edgeDetector.isNNAPIEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = CyanEdge
                            )
                        )
                    }

                    // Vision confidence slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Vision Confidence Threshold", color = Color.White, fontSize = 12.sp)
                            Text("${(confidenceSlider * 100).toInt()}%", color = CyanEdge, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = confidenceSlider,
                            onValueChange = {
                                confidenceSlider = it
                                viewModel.edgeDetector.confidenceThreshold = it
                            },
                            valueRange = 0.50f..0.95f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanEdge,
                                activeTrackColor = CyanEdge,
                                inactiveTrackColor = RoadBorder
                            )
                        )
                    }

                    // Bump shock threshold slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Suspension Bump Spike Trigger", color = Color.White, fontSize = 12.sp)
                            Text("${String.format("%.2f", bumpGSlider)}G", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = bumpGSlider,
                            onValueChange = {
                                bumpGSlider = it
                                viewModel.sensorEngine.bumpThresholdG = it
                            },
                            valueRange = 1.20f..3.00f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonAmber,
                                activeTrackColor = NeonAmber,
                                inactiveTrackColor = RoadBorder
                            )
                        )
                    }
                }
            }

            // Section 4: Maintenance & Data Controls
            Card(
                colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, RoadBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Data Management",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearAllData()
                                Toast.makeText(context, "Local database purged", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = HazardRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Purge DB", color = HazardRed, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Export Dialog
        exportData?.let { dataString ->
            AlertDialog(
                onDismissRequest = { viewModel.clearExport() },
                title = {
                    Text("Exported Telemetry Data", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Column(
                        modifier = Modifier
                            .height(280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = dataString,
                            color = CyanEdge,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(dataString))
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            viewModel.clearExport()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanEdge)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearExport() }) {
                        Text("Close", color = Color.Gray)
                    }
                },
                containerColor = RoadDarkSurface
            )
        }
    }
}
