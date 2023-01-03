package com.example.customscannerview.mlkit.interfaces

import com.example.customscannerview.mlkit.modelclasses.OCRResponse
import com.example.customscannerview.mlkit.modelclasses.OcrResponseQA
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.Text


interface OnScanResult {

    fun onViewDetected(barCodeResult: MutableList<Barcode>)
    fun onMultiBarcodesDetected(barcodes: List<Barcode>)
    fun onSomeTextDetected(text: Text)
}

interface OCRResult {
    fun onOCRResponse(ocrResponse: OCRResponse?)
    fun onOCRResponseFailed(throwable: Throwable?)
}

interface OCRResultQA {
    fun onOCRResponse(ocrResponse: OcrResponseQA?)
    fun onOCRResponseFailed(throwable: Throwable?)
}