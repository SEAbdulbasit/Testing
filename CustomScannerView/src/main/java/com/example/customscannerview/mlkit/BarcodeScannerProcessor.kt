package com.example.customscannerview.mlkit

import com.example.customscannerview.mlkit.interfaces.OnScanResult
import com.example.customscannerview.mlkit.modelclasses.BoxSides
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
    private val boxSides: BoxSides,
    private val textCallback: CameraXTextCallback,
    private val multiBarcodes: CameraXMultiBarcodeCallback,
    private val somethingDetected: OnScanResult,
    ) : VisionProcessorBase<List<Barcode>>() {

    // Note that if you know which format of barcode your app is dealing with, detection will be
    // faster to specify the supported barcode formats one by one, e.g.
    // BarcodeScannerOptions.Builder()
    //     .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
    //     .build();
    private val textDetector by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()


    override fun stop() {
        super.stop()
        barcodeScanner.close()
    }

    override fun detectInImage(image: InputImage): Task<List<Barcode>> {
        textDetector.process(image)
            .addOnSuccessListener {
                textCallback.onTextDetected(it)
            }
            .addOnFailureListener {
//                    onScanResult?.onTextDetected()
            }
        return barcodeScanner.process(image)

    }

    override fun onSuccess(results: List<Barcode>, graphicOverlay: GraphicOverlay) {
        somethingDetected.onViewDetected(results as MutableList<Barcode>)
        for (barcode in results) {
            graphicOverlay.add(BarcodeGraphic(graphicOverlay, barcode, boxSides) { isInFocus ->
                val displayValue = barcode.displayValue
                val rawValue = barcode.rawValue
                if (isInFocus) {
                    callback.onNewBarcodeScanned(barcode)
                }
            })
        }
    }



    override fun onFailure(e: Exception) {
        // do nothing
    }

}

fun interface CameraXBarcodeCallback {
    fun onNewBarcodeScanned(barcode: Barcode)

}
fun interface CameraXTextCallback{
    fun onTextDetected(text:Text)
}
fun interface CameraXMultiBarcodeCallback{
    fun onMultiBarcodeScanned(barcodes:MutableList<Barcode>)
}


