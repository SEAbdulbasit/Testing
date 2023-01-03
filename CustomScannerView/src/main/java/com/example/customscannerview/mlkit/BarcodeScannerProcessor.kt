package com.example.customscannerview.mlkit

import android.graphics.RectF
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
            callback.onTextDetected(it)
        }
        return barcodeScanner.process(image)
    }

    override fun onSuccess(results: List<Barcode>, graphicOverlay: GraphicOverlay) {
        getRectCallback.invoke()?.let { scanningRect ->
            results.map { barcode ->
                val barcodeOverlay = BarcodeGraphic(graphicOverlay, barcode, scanningRect)
                val barcodeDrawingReact = barcodeOverlay.getDrawingReact(barcode)
                if (scanningRect.contains(barcodeDrawingReact)) {
                    graphicOverlay.add(barcodeOverlay)
                    BarcodeWithAreaFlag(barcode, true)
                } else {
                    BarcodeWithAreaFlag(barcode, false)
                }
            }.filter { it.isWithInScanningArea }.apply {
                callback.onMultiBarcodeScanned(this.map { it.barcode }.toMutableList())
            }
                .onEach { callback.onNewBarcodeScanned(barcode = it.barcode) }.map { it.barcode }
        }
    }


    override fun onFailure(e: Exception) {
        e.printStackTrace()
    }

}

data class BarcodeWithAreaFlag(val barcode: Barcode, val isWithInScanningArea: Boolean)

interface CameraXBarcodeCallback {
    fun onNewBarcodeScanned(barcode: Barcode)
    fun onMultiBarcodeScanned(barcodes: MutableList<Barcode>)
    fun onTextDetected(text: Text)


}
