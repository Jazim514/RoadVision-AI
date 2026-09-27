package com.example.edgeai

enum class DefectType(val label: String, val code: String) {
    POTHOLE("Pothole", "POTH_01"),
    ALLIGATOR_CRACK("Alligator Fatigue Crack", "CRK_ALLIGATOR"),
    TRANSVERSE_CRACK("Transverse Road Crack", "CRK_TRANSVERSE"),
    RUTTING("Surface Rutting", "RUT_01"),
    MANHOLE_OFFSET("Uneven Manhole", "MANHOLE_OFF"),
    SPEED_BUMP("Speed Bump / Ridge", "BUMP_01");

    fun getIcon(): String {
        return when (this) {
            POTHOLE -> "⚠️"
            ALLIGATOR_CRACK -> "⚡"
            TRANSVERSE_CRACK -> "〰️"
            RUTTING -> "⫽"
            MANHOLE_OFFSET -> "⭕"
            SPEED_BUMP -> "⛰️"
        }
    }
}

enum class DefectSeverity(val label: String, val level: Int) {
    LOW("Minor", 1),
    MEDIUM("Moderate", 2),
    SEVERE("Severe", 3),
    CRITICAL("Hazardous", 4)
}

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
}

data class DetectedRoadDefect(
    val id: String,
    val type: DefectType,
    val severity: DefectSeverity,
    val confidence: Float,
    val box: NormalizedRect,
    val estimatedDistanceMeters: Float,
    val estimatedDepthCm: Float,
    val estimatedSurfaceAreaCm2: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isConfirmedByGForce: Boolean = false,
    val gForceTrigger: Float = 0f
)

data class InferenceMetrics(
    val fps: Float = 32.4f,
    val inferenceTimeMs: Long = 21,
    val frameCount: Long = 0,
    val accelerator: String = "NPU / NNAPI",
    val thermalState: String = "Nominal (34°C)",
    val modelResolution: String = "640x640",
    val isRealTimeTargetMet: Boolean = true
)

enum class DetectionMode {
    LIVE_CAMERA,
    SIMULATED_DASHCAM_DRIVE
}
