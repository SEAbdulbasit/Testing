package com.example.customscannerview.mlkit

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class ImageAnalyzer(
    private val onScanResult: CameraXBarcodeCallback?
) : ImageAnalysis.Analyzer {

    private var isScanning: Boolean = false
    private val textDetector by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()
            //
            isScanning = true

            textDetector.process(image).addOnSuccessListener {
                onScanResult?.onTextDetected(it)
            }

            scanner.process(image).addOnSuccessListener { barcodes ->
                onScanResult?.onMultiBarcodeScanned(barcodes)
                isScanning = false
                imageProxy.close()
            }.addOnCompleteListener {
                onScanResult?.onMultiBarcodeScanned(it.result)
                it.result.forEach { onScanResult?.onNewBarcodeScanned(it) }
                imageProxy.close()
            }.addOnFailureListener {
                imageProxy.close()
            }

        }

    }

}

