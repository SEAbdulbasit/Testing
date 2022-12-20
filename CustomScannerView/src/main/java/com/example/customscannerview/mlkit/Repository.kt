package com.example.customscannerview.mlkit

import com.example.customscannerview.mlkit.modelclasses.OCRResponseDemo
import com.example.customscannerview.mlkit.modelclasses.OCRResponseParent
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRQARequest
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRRequestParent
import com.example.customscannerview.mlkit.service.OcrApiService
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OcrRequest
import com.example.customscannerview.mlkit.modelclasses.OcrResponse

class Repository(private val apiService: OcrApiService) {

    suspend fun analyseOCRAsync(ocrRequestParent: OCRRequestParent): OCRResponseParent {
        return when (ocrRequestParent) {
            is OCRQARequest -> analyseOCRQAAsync(ocrRequestParent)
            is OcrRequest -> analyseOCRDemoAsync(ocrRequestParent)
        }
    }

    private suspend fun analyseOCRDemoAsync(ocrRequest: OcrRequest): OCRResponseDemo {
        return apiService.analyseOCRDemoAsync(ocrRequest)
    }

    private suspend fun analyseOCRQAAsync(ocrRequest: OCRQARequest): OcrResponse {
        return apiService.analyseOCRQAAsync(ocrRequest)
    }
}