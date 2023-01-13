package vision_sdk_android

import com.example.customscannerview.mlkit.enums.ViewType
import com.example.customscannerview.mlkit.views.DetectionMode
import com.example.customscannerview.mlkit.views.ScanningMode


data class ScreenState(
    val scanningWindow: ViewType = ViewType.WINDOW,
    val detectionMode: DetectionMode = DetectionMode.Barcode,
    val scanningMode: ScanningMode = ScanningMode.Manual,
    val flashStatus: Boolean = false
)



