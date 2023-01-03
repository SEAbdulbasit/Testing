package com.example.customscannerview.mlkit.views

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.common.Barcode

interface ScannerCallbacks {
    fun onBarcodeDetected(barcode: Barcode)
    fun onMultipleBarcodesDetected(barcodeList: List<Barcode>)
    fun onFailure(exception: ScannerException)
    fun onImageCaptured(bitmap: Bitmap, value: MutableList<Barcode>?)
}


sealed class ScannerException : Exception() {
    class QRCodeNotDetected(string: String = "No qrcode detected") : ScannerException()
    class BarCodeNotDetected(string: String = "No barcode detected") : ScannerException()
}