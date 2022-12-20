package com.example.customscannerview.mlkit.interfaces

import com.example.customscannerview.mlkit.modelclasses.OCRResponseParent
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.Text


interface OnScanResult {

    fun onViewDetected(barCodeResult: MutableList<Barcode>)
    fun onMultiBarcodesDetected(barcodes: List<Barcode>)
    fun onOCRResponse(ocrResponse: OCRResponseParent?)
    fun onOCRResponseFailed(throwable: Throwable?)
    fun onSomeTextDetected(text: Text)
}