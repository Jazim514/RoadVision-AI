package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.IncidentLogView
import com.example.ui.components.InteractiveRoadMap
import com.example.ui.components.LiveDashcamHud
import com.example.ui.components.SyncSettingsView
import com.example.ui.theme.CyanEdge
import com.example.ui.theme.HazardRed
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.RoadBorder
import com.example.ui.theme.RoadDarkSurface
import com.example.ui.theme.RoadDeepNavy

enum class AppTab(val title: String) {
    LIVE_HUD("Live HUD"),
    ROAD_MAP("GIS Map"),
    INCIDENTS("Incidents"),
    EDGE_SYNC("Edge Sync")
}

@Composable
fun MainScreen(viewModel: RoadVisionViewModel) {
    var currentTabIndex by remember { mutableIntStateOf(0) }
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val totalDefects by viewModel.totalDefectCount.collectAsState()
    val context = LocalContext.current

    // Request Camera and Location permissions for Live Camera & GPS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (locationGranted) {
            viewModel.sensorEngine.startListening()
        }
    }

    LaunchedEffect(Unit) {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasCamera || !hasLocation) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Handle back button when not on primary home screen
    if (currentTabIndex != 0) {
        BackHandler {
            currentTabIndex = 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = RoadDeepNavy,
        bottomBar = {
            NavigationBar(
                containerColor = RoadDarkSurface,
                modifier = Modifier
                    .border(width = 0.8.dp, color = RoadBorder)
                    .testTag("main_bottom_nav")
            ) {
                // Tab 0: Live HUD
                NavigationBarItem(
                    selected = currentTabIndex == 0,
                    onClick = { currentTabIndex = 0 },
                    icon = {
                        Icon(
                            imageVector = if (currentTabIndex == 0) Icons.Filled.Speed else Icons.Outlined.Speed,
                            contentDescription = "Live Dashcam HUD",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Live HUD", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyanEdge,
                        indicatorColor = CyanEdge,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_item_live_hud")
                )

                // Tab 1: Road Map
                NavigationBarItem(
                    selected = currentTabIndex == 1,
                    onClick = { currentTabIndex = 1 },
                    icon = {
                        Icon(
                            imageVector = if (currentTabIndex == 1) Icons.Filled.Map else Icons.Outlined.Map,
                            contentDescription = "Road Heatmap",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("GIS Map", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyanEdge,
                        indicatorColor = CyanEdge,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_item_map")
                )

                // Tab 2: Incidents
                NavigationBarItem(
                    selected = currentTabIndex == 2,
                    onClick = { currentTabIndex = 2 },
                    icon = {
                        BadgedBox(badge = {
                            if (totalDefects > 0) {
                                Badge(containerColor = HazardRed, contentColor = Color.White) {
                                    Text("$totalDefects", fontSize = 9.sp)
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (currentTabIndex == 2) Icons.Filled.ListAlt else Icons.Outlined.ListAlt,
                                contentDescription = "Incidents",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = { Text("Incidents", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyanEdge,
                        indicatorColor = CyanEdge,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_item_incidents")
                )

                // Tab 3: Edge Sync
                NavigationBarItem(
                    selected = currentTabIndex == 3,
                    onClick = { currentTabIndex = 3 },
                    icon = {
                        BadgedBox(badge = {
                            if (pendingSyncCount > 0) {
                                Badge(containerColor = NeonAmber, contentColor = Color.Black) {
                                    Text("$pendingSyncCount", fontSize = 9.sp)
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (currentTabIndex == 3) Icons.Filled.CloudSync else Icons.Outlined.CloudSync,
                                contentDescription = "Edge Sync",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = { Text("Edge Sync", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyanEdge,
                        indicatorColor = CyanEdge,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_item_sync")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTabIndex) {
                0 -> LiveDashcamHud(viewModel = viewModel)
                1 -> InteractiveRoadMap(viewModel = viewModel)
                2 -> IncidentLogView(viewModel = viewModel)
                3 -> SyncSettingsView(viewModel = viewModel)
            }
        }
    }
}
