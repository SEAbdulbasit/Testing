package com.example.customscannerview.mlkit.interfaces

import com.example.scannerview.modelclasses.ocr_response.OcrResponse
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.Text


interface OnScanResult {

    fun onViewDetected(barCodeResult: MutableList<Barcode>)
    fun onMultiBarcodesDetected(barcodes:List<Barcode>)
    fun onOCRResponse(ocrResponse: OcrResponse?)
    fun onOCRResponseFailed(throwable: Throwable?)
    fun onSomeTextDetected(text: Text)
}