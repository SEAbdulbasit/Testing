package com.example.customscannerview.mlkit

import android.graphics.RectF
import com.example.customscannerview.mlkit.interfaces.OnScanResult
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** Barcode Detector Demo.  */
class BarcodeScannerProcessor(
    private val callback: CameraXBarcodeCallback,
    private val textCallback: CameraXTextCallback,
    private val somethingDetected: OnScanResult,
    private val getRectCallback: () -> RectF?
) : VisionProcessorBase<List<Barcode>>() {

    private val textDetector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()

    override fun stop() {
        super.stop()
        barcodeScanner.close()
        textDetector.close()
    }

    override fun detectInImage(image: InputImage): Task<List<Barcode>> {
        textDetector.process(image).addOnSuccessListener {
            textCallback.onTextDetected(it)
        }
        return barcodeScanner.process(image)
    }

    override fun onSuccess(results: List<Barcode>, graphicOverlay: GraphicOverlay) {
        getRectCallback.invoke()?.let { scanningRect ->
            val filteredResults = results.map { barcode ->
                val barcodeOverlay = BarcodeGraphic(graphicOverlay, barcode, scanningRect)
                val barcodeDrawingReact = barcodeOverlay.getDrawingReact(barcode)
                if (scanningRect.contains(barcodeDrawingReact)) {
                    graphicOverlay.add(barcodeOverlay)
                    BarcodeWithAreaFlag(barcode, true)
                } else {
                    BarcodeWithAreaFlag(barcode, false)
                }
            }.filter { it.isWithInScanningArea }
                .onEach { callback.onNewBarcodeScanned(barcode = it.barcode) }.map { it.barcode }

            somethingDetected.onViewDetected(filteredResults.toMutableList())
        }
    }


    override fun onFailure(e: Exception) {
        e.printStackTrace()
    }

}

data class BarcodeWithAreaFlag(val barcode: Barcode, val isWithInScanningArea: Boolean)

fun interface CameraXBarcodeCallback {
    fun onNewBarcodeScanned(barcode: Barcode)

}

fun interface CameraXTextCallback {
    fun onTextDetected(text: Text)
}

fun interface CameraXMultiBarcodeCallback {
    fun onMultiBarcodeScanned(barcodes: MutableList<Barcode>)
}


