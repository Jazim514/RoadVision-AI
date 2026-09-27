package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.RoadDefectEntity
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

@Composable
fun IncidentLogView(
    viewModel: RoadVisionViewModel,
    modifier: Modifier = Modifier
) {
    val defects by viewModel.allDefects.collectAsState()
    val potholeCount by viewModel.potholeCount.collectAsState()
    val totalCount by viewModel.totalDefectCount.collectAsState()
    val pendingSync by viewModel.pendingSyncCount.collectAsState()

    var filterType by remember { mutableStateOf("ALL") }

    val filteredList = remember(defects, filterType) {
        when (filterType) {
            "POTHOLES" -> defects.filter { it.defectType == "POTHOLE" }
            "CRACKS" -> defects.filter { it.defectType.contains("CRACK") }
            "SEVERE" -> defects.filter { it.severity == "SEVERE" || it.severity == "CRITICAL" }
            else -> defects
        }
    }

    val maxGForce = remember(defects) {
        defects.maxOfOrNull { it.peakGForce } ?: 1.0f
    }
    val avgDepth = remember(defects) {
        if (defects.isNotEmpty()) defects.map { it.depthCm }.average().toFloat() else 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoadDeepNavy)
            .testTag("incident_log_view")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "INCIDENT AUDIT LOG",
                            color = CyanEdge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Synchronized Road Defect Records",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { viewModel.exportCsv() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RoadDarkSurface)
                                .border(1.dp, RoadBorder, CircleShape)
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = "Export CSV", tint = CyanEdge, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Dashboard Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatsCard(
                        title = "POTHOLES",
                        value = "$potholeCount",
                        subValue = "of $totalCount total",
                        color = HazardRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = "PEAK G-FORCE",
                        value = "${String.format("%.2f", maxGForce)}G",
                        subValue = "Max shock",
                        color = NeonAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = "AVG DEPTH",
                        value = "${String.format("%.1f", avgDepth)}cm",
                        subValue = "Crater depth",
                        color = CyanEdge,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All ($totalCount)", "POTHOLES" to "Potholes ($potholeCount)", "CRACKS" to "Cracks", "SEVERE" to "Severe Only").forEach { (type, label) ->
                        FilterChip(
                            selected = filterType == type,
                            onClick = { filterType = type },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanEdge.copy(alpha = 0.25f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    EmptyLogCard()
                }
            } else {
                items(filteredList, key = { it.id }) { defect ->
                    DefectItemCard(
                        defect = defect,
                        onSelect = { viewModel.selectDefect(defect) },
                        onDelete = { viewModel.deleteDefect(defect.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    value: String,
    subValue: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, RoadBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text(subValue, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun DefectItemCard(
    defect: RoadDefectEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RoadBorder, RoundedCornerShape(14.dp))
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Hazard badge circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when (defect.severity) {
                                "CRITICAL" -> HazardRed.copy(alpha = 0.2f)
                                "SEVERE" -> HazardRed.copy(alpha = 0.2f)
                                else -> NeonAmber.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (defect.defectType == "POTHOLE") "⚠️" else "⚡",
                        fontSize = 18.sp
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = defect.defectType.replace("_", " "),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Surface(
                            color = when (defect.severity) {
                                "CRITICAL", "SEVERE" -> HazardRed.copy(alpha = 0.25f)
                                else -> NeonAmber.copy(alpha = 0.25f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = defect.severity,
                                color = when (defect.severity) {
                                    "CRITICAL", "SEVERE" -> HazardRed
                                    else -> NeonAmber
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "${defect.streetName} • ${dateFormat.format(Date(defect.timestamp))}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Depth: ${String.format("%.1f", defect.depthCm)}cm",
                            color = CyanEdge,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Shock: ${String.format("%.2f", defect.peakGForce)}G",
                            color = if (defect.peakGForce > 2.0f) HazardRed else Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Conf: ${(defect.confidence * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Sync Status & Delete
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (defect.syncStatus == "SYNCED") Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
                        contentDescription = defect.syncStatus,
                        tint = if (defect.syncStatus == "SYNCED") RoadGreen else NeonAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = defect.syncStatus,
                        color = if (defect.syncStatus == "SYNCED") RoadGreen else NeonAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val ctx = LocalContext.current
                    // Launch Google Earth 3D
                    IconButton(
                        onClick = {
                            com.example.maps.GoogleEarthMapsHelper.openGoogleEarth3D(
                                context = ctx,
                                latitude = defect.latitude,
                                longitude = defect.longitude,
                                altitudeMeters = 200.0
                            )
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Public, contentDescription = "Earth 3D", tint = CyanEdge, modifier = Modifier.size(15.dp))
                    }

                    // Launch Google Maps Nav
                    IconButton(
                        onClick = {
                            com.example.maps.GoogleEarthMapsHelper.openGoogleMapsNavigation(
                                context = ctx,
                                latitude = defect.latitude,
                                longitude = defect.longitude,
                                label = defect.defectType
                            )
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Maps Nav", tint = RoadGreen, modifier = Modifier.size(15.dp))
                    }

                    // Delete
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLogCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp)
            .border(1.dp, RoadBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🛣️", fontSize = 42.sp)
            Text(
                text = "No Road Defects Logged",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Start a Survey Run or drive in simulated mode to detect potholes and sync road telematics in real-time.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
