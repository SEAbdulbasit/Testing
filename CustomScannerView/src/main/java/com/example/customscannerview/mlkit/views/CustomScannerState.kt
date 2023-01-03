package com.example.customscannerview.mlkit.views

import com.example.customscannerview.mlkit.enums.ViewType

data class CustomScannerState(
    val scanningWindow: ViewType = ViewType.RECTANGLE,
    val scanningMode: ScanningMode = ScanningMode.Manual,
    val detectionMode: DetectionMode = DetectionMode.Barcode,
    val cameraTriggerForDetected: Boolean = false
)


sealed interface ScanningMode {
    object Manual : ScanningMode
    object Auto : ScanningMode
}


sealed interface DetectionMode {
    object QR : DetectionMode
    object Barcode : DetectionMode
    object Auto : DetectionMode
    object OCR : DetectionMode
}
