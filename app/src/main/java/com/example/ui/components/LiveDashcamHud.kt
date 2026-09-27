package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.edgeai.DefectSeverity
import com.example.edgeai.DefectType
import com.example.edgeai.DetectedRoadDefect
import com.example.edgeai.DetectionMode
import com.example.ui.RoadVisionViewModel
import com.example.ui.theme.BoundingBoxCrack
import com.example.ui.theme.BoundingBoxManhole
import com.example.ui.theme.BoundingBoxPothole
import com.example.ui.theme.CyanEdge
import com.example.ui.theme.HazardRed
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.RoadBorder
import com.example.ui.theme.RoadDarkSurface
import com.example.ui.theme.RoadGreen
import java.util.concurrent.Executors

sealed class CameraUiState {
    object Initializing : CameraUiState()
    data class Active(val isFront: Boolean, val canFlip: Boolean) : CameraUiState()
    object PermissionRequired : CameraUiState()
    data class Unavailable(val reason: String) : CameraUiState()
    data class Error(val error: String) : CameraUiState()
}

@Composable
fun LiveDashcamHud(
    viewModel: RoadVisionViewModel,
    modifier: Modifier = Modifier
) {
    val detectionMode by viewModel.detectionMode.collectAsState()
    val inferenceMetrics by viewModel.inferenceMetrics.collectAsState()
    val detections by viewModel.currentDetections.collectAsState()
    val telemetry by viewModel.telemetryState.collectAsState()
    val isSurveying by viewModel.isSurveying.collectAsState()
    val sessionDuration by viewModel.sessionDurationSeconds.collectAsState()
    val sessionDistance by viewModel.sessionDistanceMeters.collectAsState()
    val simulatedSpeed by viewModel.simulatedSpeedKmh.collectAsState()
    val currentAlert by viewModel.currentAlert.collectAsState()

    var showSpeedSlider by remember { mutableStateOf(false) }
    var cameraUiState by remember { mutableStateOf<CameraUiState>(CameraUiState.Initializing) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("live_dashcam_hud")
    ) {
        // 1. Live Video Viewport (CameraX or Dashcam Drive Simulation)
        if (detectionMode == DetectionMode.LIVE_CAMERA) {
            CameraXLivePreview(
                viewModel = viewModel,
                lensFacing = lensFacing,
                cameraUiState = cameraUiState,
                onCameraStateChanged = { cameraUiState = it },
                onSwitchToSimulation = {
                    viewModel.setDetectionMode(DetectionMode.SIMULATED_DASHCAM_DRIVE)
                }
            )
        } else {
            SimulatedRoadDriveCanvas(
                speedKmh = simulatedSpeed,
                detections = detections
            )
        }

        // 2. HUD Bounding Boxes Overlay
        BoundingBoxOverlay(detections = detections)

        // 3. Top Edge AI Diagnostics Telemetry Bar
        TopDiagnosticsBar(
            fps = inferenceMetrics.fps,
            latencyMs = inferenceMetrics.inferenceTimeMs,
            accelerator = inferenceMetrics.accelerator,
            isTargetMet = inferenceMetrics.isRealTimeTargetMet,
            mode = detectionMode,
            cameraState = cameraUiState,
            onToggleMode = {
                viewModel.setDetectionMode(
                    if (detectionMode == DetectionMode.LIVE_CAMERA)
                        DetectionMode.SIMULATED_DASHCAM_DRIVE
                    else
                        DetectionMode.LIVE_CAMERA
                )
            },
            onFlipCamera = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            }
        )

        // 4. Hazard Warning Banner (when approaching critical road defect)
        AnimatedVisibility(
            visible = currentAlert != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            currentAlert?.let { alert ->
                HazardAlertBanner(defect = alert.defect)
            }
        }

        // 5. Bottom HUD Controls & Telemetry Cockpit
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            // Speed regulator slider popup when in simulation
            if (showSpeedSlider && detectionMode == DetectionMode.SIMULATED_DASHCAM_DRIVE) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoadDarkSurface.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Simulated Drive Speed", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${simulatedSpeed.toInt()} km/h", color = CyanEdge, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = simulatedSpeed,
                            onValueChange = { viewModel.setSimulatedSpeed(it) },
                            valueRange = 10f..110f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanEdge,
                                activeTrackColor = CyanEdge,
                                inactiveTrackColor = RoadBorder
                            )
                        )
                    }
                }
            }

            // Gauges Row: Speedometer + G-Force Seismograph + Survey Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Digital Speedometer Card
                SpeedometerCard(
                    speedKmh = telemetry.speedKmh,
                    roughnessIri = telemetry.roughnessIri,
                    roadQuality = telemetry.roadQualityScore,
                    onClick = {
                        if (detectionMode == DetectionMode.SIMULATED_DASHCAM_DRIVE) {
                            showSpeedSlider = !showSpeedSlider
                        }
                    },
                    modifier = Modifier.weight(1.0f)
                )

                // Accelerometer Seismograph Card
                GForceSeismographCard(
                    waveform = telemetry.vibrationHistory,
                    currentG = telemetry.verticalGForce,
                    peakG = telemetry.peakGForceWindow,
                    onBumpSimulate = {
                        viewModel.sensorEngine.injectBumpSpike(2.65f)
                    },
                    modifier = Modifier.weight(1.3f)
                )
            }

            // Bottom Survey Recording Action Bar
            SurveyActionBar(
                isSurveying = isSurveying,
                sessionDurationSec = sessionDuration,
                sessionDistanceM = sessionDistance,
                detectionCount = detections.size,
                latitude = telemetry.latitude,
                longitude = telemetry.longitude,
                headingDegrees = telemetry.bearingDegrees,
                onToggleSurvey = { viewModel.toggleSurveySession() }
            )
        }
    }
}

@Composable
private fun TopDiagnosticsBar(
    fps: Float,
    latencyMs: Long,
    accelerator: String,
    isTargetMet: Boolean,
    mode: DetectionMode,
    cameraState: CameraUiState,
    onToggleMode: () -> Unit,
    onFlipCamera: () -> Unit
) {
    Surface(
        color = Color(0xCC090D16),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, RoadBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edge AI FPS Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isTargetMet) RoadGreen else NeonAmber)
                )
                Text(
                    text = String.format("%.1f FPS", fps),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "• ${latencyMs}ms",
                    color = CyanEdge,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Accelerator Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x3300E5FF))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ElectricBolt,
                    contentDescription = null,
                    tint = CyanEdge,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = accelerator.take(12),
                    color = CyanEdge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // If on Live Cam and multiple cameras are supported, show flip camera button
                if (mode == DetectionMode.LIVE_CAMERA && (cameraState is CameraUiState.Active && (cameraState as CameraUiState.Active).canFlip)) {
                    IconButton(
                        onClick = onFlipCamera,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(RoadDarkSurface)
                            .border(1.dp, RoadBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = CyanEdge,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Mode Selector Pill
                Surface(
                    color = if (mode == DetectionMode.LIVE_CAMERA) CyanEdge.copy(alpha = 0.2f) else NeonAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .clickable { onToggleMode() }
                        .border(
                            1.dp,
                            if (mode == DetectionMode.LIVE_CAMERA) CyanEdge else NeonAmber,
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (mode == DetectionMode.LIVE_CAMERA) Icons.Filled.CameraAlt else Icons.Outlined.DirectionsCar,
                            contentDescription = null,
                            tint = if (mode == DetectionMode.LIVE_CAMERA) CyanEdge else NeonAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (mode == DetectionMode.LIVE_CAMERA) "Live Cam" else "Sim Drive",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HazardAlertBanner(defect: DetectedRoadDefect) {
    Card(
        colors = CardDefaults.cardColors(containerColor = HazardRed.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ROAD HAZARD AHEAD: ${defect.type.label.uppercase()}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Text(
                    text = "Estimated ${String.format("%.1f", defect.estimatedDistanceMeters)}m ahead • Depth: ${String.format("%.1f", defect.estimatedDepthCm)}cm",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun BoundingBoxOverlay(detections: List<DetectedRoadDefect>) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw subtle vehicle horizon and lane guidance crosshairs
            val horizonY = heightPx * 0.52f
            drawLine(
                color = Color(0x3300E5FF),
                start = Offset(0f, horizonY),
                end = Offset(widthPx, horizonY),
                strokeWidth = 1.dp.toPx()
            )

            // Dynamic bounding boxes
            detections.forEach { defect ->
                val box = defect.box
                val left = box.left * widthPx
                val top = box.top * heightPx
                val right = box.right * widthPx
                val bottom = box.bottom * heightPx
                val bWidth = right - left
                val bHeight = bottom - top

                val boxColor = when (defect.type) {
                    DefectType.POTHOLE -> if (defect.severity == DefectSeverity.CRITICAL) HazardRed else NeonAmber
                    DefectType.ALLIGATOR_CRACK, DefectType.TRANSVERSE_CRACK -> BoundingBoxCrack
                    DefectType.MANHOLE_OFFSET -> BoundingBoxManhole
                    else -> CyanEdge
                }

                // Main bounding box rectangle
                drawRect(
                    color = boxColor.copy(alpha = 0.25f),
                    topLeft = Offset(left, top),
                    size = Size(bWidth, bHeight)
                )

                // High-tech corner bracket stroke
                val cornerLen = (bWidth * 0.25f).coerceIn(12f, 36f)
                val strokeW = 2.5.dp.toPx()

                // Top-Left
                drawLine(boxColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
                drawLine(boxColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)

                // Top-Right
                drawLine(boxColor, Offset(right, top), Offset(right - cornerLen, top), strokeW)
                drawLine(boxColor, Offset(right, top), Offset(right, top + cornerLen), strokeW)

                // Bottom-Left
                drawLine(boxColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
                drawLine(boxColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)

                // Bottom-Right
                drawLine(boxColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
                drawLine(boxColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)
            }
        }

        // Render defect floating pills
        detections.forEach { defect ->
            val box = defect.box
            val left = box.left * widthPx
            val top = box.top * heightPx

            Box(
                modifier = Modifier
                    .padding(
                        start = (left / LocalContext.current.resources.displayMetrics.density).dp,
                        top = ((top - 24f).coerceAtLeast(0f) / LocalContext.current.resources.displayMetrics.density).dp
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (defect.type) {
                            DefectType.POTHOLE -> HazardRed
                            else -> NeonAmber
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${defect.type.getIcon()} ${defect.type.label} ${(defect.confidence * 100).toInt()}% • ${String.format("%.1fm", defect.estimatedDistanceMeters)}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun SimulatedRoadDriveCanvas(
    speedKmh: Float,
    detections: List<DetectedRoadDefect>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Sky & horizon
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF070B14), Color(0xFF141F32)),
                startY = 0f,
                endY = height * 0.52f
            ),
            topLeft = Offset.Zero,
            size = Size(width, height * 0.52f)
        )

        // Asphalt road gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1E242B), Color(0xFF101317)),
                startY = height * 0.52f,
                endY = height
            ),
            topLeft = Offset(0f, height * 0.52f),
            size = Size(width, height * 0.48f)
        )

        // Perspective road borders
        val horizonY = height * 0.52f
        val leftBorderPath = Path().apply {
            moveTo(width * 0.35f, horizonY)
            lineTo(0f, height)
        }
        val rightBorderPath = Path().apply {
            moveTo(width * 0.65f, horizonY)
            lineTo(width, height)
        }
        drawPath(leftBorderPath, Color(0xFF475569), style = Stroke(width = 3.dp.toPx()))
        drawPath(rightBorderPath, Color(0xFF475569), style = Stroke(width = 3.dp.toPx()))

        // Animated center dashed lines moving toward camera
        val timePhase = (System.currentTimeMillis() % 1000) / 1000f
        val numDashes = 7
        for (i in 0 until numDashes) {
            val progress = ((i + timePhase) % numDashes) / numDashes.toFloat()
            val dashY = horizonY + progress * (height - horizonY)
            val dashLen = 10.dp.toPx() + progress * 40.dp.toPx()
            val dashWidth = 2.dp.toPx() + progress * 5.dp.toPx()

            drawLine(
                color = Color(0xDDFFD600),
                start = Offset(width * 0.5f, dashY),
                end = Offset(width * 0.5f, dashY + dashLen),
                strokeWidth = dashWidth,
                cap = StrokeCap.Round
            )
        }

        // Draw simulated defects onto asphalt
        detections.forEach { defect ->
            val box = defect.box
            val cx = box.centerX * width
            val cy = box.centerY * height
            val rX = box.width * width * 0.45f
            val rY = box.height * height * 0.45f

            when (defect.type) {
                DefectType.POTHOLE -> {
                    // Deep dark crater oval with cracked shadow edges
                    drawOval(
                        color = Color(0xFF050505),
                        topLeft = Offset(cx - rX, cy - rY),
                        size = Size(rX * 2, rY * 2)
                    )
                    drawOval(
                        color = HazardRed.copy(alpha = 0.7f),
                        topLeft = Offset(cx - rX, cy - rY),
                        size = Size(rX * 2, rY * 2),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                DefectType.ALLIGATOR_CRACK, DefectType.TRANSVERSE_CRACK -> {
                    // Cracking fissure pattern
                    drawLine(
                        color = NeonAmber,
                        start = Offset(cx - rX, cy),
                        end = Offset(cx + rX, cy + rY * 0.3f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = NeonAmber,
                        start = Offset(cx - rX * 0.4f, cy - rY * 0.4f),
                        end = Offset(cx + rX * 0.3f, cy + rY * 0.5f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
                DefectType.MANHOLE_OFFSET -> {
                    // Iron circular utility cover
                    drawCircle(
                        color = Color(0xFF2A2E33),
                        radius = (rX + rY) / 2f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = CyanEdge,
                        radius = (rX + rY) / 2f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun CameraXLivePreview(
    viewModel: RoadVisionViewModel,
    lensFacing: Int,
    cameraUiState: CameraUiState,
    onCameraStateChanged: (CameraUiState) -> Unit,
    onSwitchToSimulation: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    // Check camera permission
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            onCameraStateChanged(CameraUiState.Initializing)
            retryTrigger++
        } else {
            onCameraStateChanged(CameraUiState.PermissionRequired)
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            onCameraStateChanged(CameraUiState.PermissionRequired)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // Use COMPATIBLE (TextureView) to ensure reliable rendering on emulators and avoid black screens
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProviderRef = cameraProvider

                            val hasBack = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                            val hasFront = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

                            if (!hasBack && !hasFront) {
                                onCameraStateChanged(
                                    CameraUiState.Unavailable("No physical or emulated camera sensor detected on this device/emulator.")
                                )
                                return@addListener
                            }

                            val targetSelector = when {
                                lensFacing == CameraSelector.LENS_FACING_FRONT && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                                hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                                hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                                else -> CameraSelector.DEFAULT_BACK_CAMERA
                            }

                            val preview = Preview.Builder()
                                .setTargetResolution(android.util.Size(640, 480))
                                .build()
                                .also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(android.util.Size(640, 480))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, viewModel.edgeDetector)
                                }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                targetSelector,
                                preview,
                                imageAnalysis
                            )

                            onCameraStateChanged(
                                CameraUiState.Active(
                                    isFront = targetSelector == CameraSelector.DEFAULT_FRONT_CAMERA,
                                    canFlip = hasBack && hasFront
                                )
                            )
                        } catch (e: Exception) {
                            onCameraStateChanged(
                                CameraUiState.Error(e.localizedMessage ?: "Camera binding error")
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay: Permission Required Card
        if (!hasPermission || cameraUiState is CameraUiState.PermissionRequired) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6090D16))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, NeonAmber, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NeonAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Text(
                            text = "CAMERA ACCESS REQUIRED",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "RoadVision AI requires camera access to analyze live road distress, potholes, and surface conditions at 30+ FPS.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanEdge),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Camera Permission", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onSwitchToSimulation,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = CyanEdge, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Switch to Simulated Dashcam Drive", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Overlay: Hardware Unavailable in Emulator / Container
        if (cameraUiState is CameraUiState.Unavailable || cameraUiState is CameraUiState.Error) {
            val errorMsg = when (cameraUiState) {
                is CameraUiState.Unavailable -> cameraUiState.reason
                is CameraUiState.Error -> cameraUiState.error
                else -> "Camera initialization failed."
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6090D16))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CyanEdge, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CyanEdge.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VideocamOff,
                                contentDescription = null,
                                tint = CyanEdge,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Text(
                            text = "CAMERA HARDWARE NOTICE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "$errorMsg\n\nRoadVision AI has full virtual support with Simulated Dashcam Drive featuring 30+ FPS edge AI inference, real-time telemetry, and 3D hazards.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = onSwitchToSimulation,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanEdge),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Simulated Dashcam Drive", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onCameraStateChanged(CameraUiState.Initializing)
                                retryTrigger++
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = CyanEdge, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Camera Detection", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, lensFacing, retryTrigger) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
            } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }
}

@Composable
private fun SpeedometerCard(
    speedKmh: Float,
    roughnessIri: Float,
    roadQuality: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RoadDarkSurface.copy(alpha = 0.90f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(1.dp, RoadBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TELEMETRY",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Outlined.Speed,
                    contentDescription = null,
                    tint = CyanEdge,
                    modifier = Modifier.size(14.dp)
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${speedKmh.toInt()}",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = " km/h",
                    color = CyanEdge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "IRI: ${String.format("%.1f", roughnessIri)}",
                    color = if (roughnessIri < 2.5f) RoadGreen else NeonAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Q-Score: $roadQuality",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun GForceSeismographCard(
    waveform: List<Float>,
    currentG: Float,
    peakG: Float,
    onBumpSimulate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RoadDarkSurface.copy(alpha = 0.90f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(1.dp, RoadBorder, RoundedCornerShape(16.dp))
            .clickable { onBumpSimulate() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Z-ACCEL SEISMOGRAPH",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.2f", currentG)}G",
                    color = if (currentG > 1.8f) HazardRed else CyanEdge,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Real-time oscilloscope waveform Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF090D14))
            ) {
                val w = size.width
                val h = size.height

                // Baseline 1.0G line
                val baseY = h * 0.70f
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = Offset(0f, baseY),
                    end = Offset(w, baseY),
                    strokeWidth = 1.dp.toPx()
                )

                // 2.0G warning threshold line
                val threshY = h * 0.25f
                drawLine(
                    color = Color(0x44FF1744),
                    start = Offset(0f, threshY),
                    end = Offset(w, threshY),
                    strokeWidth = 1.dp.toPx()
                )

                if (waveform.size >= 2) {
                    val path = Path()
                    val dx = w / (waveform.size - 1).toFloat()

                    waveform.forEachIndexed { i, g ->
                        // Scale G from 0.5G to 3.0G into canvas height
                        val norm = ((g - 0.5f) / 2.5f).coerceIn(0f, 1f)
                        val y = h - (norm * h)
                        if (i == 0) {
                            path.moveTo(0f, y)
                        } else {
                            path.lineTo(i * dx, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = if (peakG > 1.8f) HazardRed else CyanEdge,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Peak: ${String.format("%.2f", peakG)}G",
                    color = if (peakG > 2.0f) HazardRed else Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tap to test impact",
                    color = CyanEdge.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun SurveyActionBar(
    isSurveying: Boolean,
    sessionDurationSec: Long,
    sessionDistanceM: Float,
    detectionCount: Int,
    latitude: Double,
    longitude: Double,
    headingDegrees: Float,
    onToggleSurvey: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RoadDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .border(1.dp, RoadBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSurveying) HazardRed else Color.Gray)
                    )
                    Text(
                        text = if (isSurveying) "RECORDING" else "STANDBY",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                val mins = sessionDurationSec / 60
                val secs = sessionDurationSec % 60
                Text(
                    text = "${String.format("%02d:%02d", mins, secs)} • ${String.format("%.1fkm", sessionDistanceM / 1000f)} • $detectionCount det",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val context = LocalContext.current

                // Launch Google Earth 3D
                IconButton(
                    onClick = {
                        com.example.maps.GoogleEarthMapsHelper.openGoogleEarth3D(
                            context = context,
                            latitude = latitude,
                            longitude = longitude,
                            altitudeMeters = 350.0,
                            headingDegrees = headingDegrees
                        )
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = "Google Earth 3D",
                        tint = CyanEdge,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Launch Google Maps
                IconButton(
                    onClick = {
                        com.example.maps.GoogleEarthMapsHelper.openGoogleMapsSatellite(
                            context = context,
                            latitude = latitude,
                            longitude = longitude
                        )
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Google Maps",
                        tint = RoadGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = onToggleSurvey,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSurveying) HazardRed else CyanEdge
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("toggle_survey_button")
                ) {
                    Icon(
                        imageVector = if (isSurveying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isSurveying) "STOP" else "SURVEY",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
