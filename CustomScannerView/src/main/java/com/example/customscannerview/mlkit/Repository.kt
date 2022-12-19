package com.example.customscannerview.mlkit

import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRQARequest
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRRequestParent
import com.example.customscannerview.mlkit.service.OcrApiService
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OcrRequest
import com.example.customscannerview.mlkit.modelclasses.ocr_response.OcrResponse
import kotlinx.coroutines.Deferred

class Repository(private val apiService: OcrApiService) {

    suspend fun analyseOCRAsync(ocrRequestParent: OCRRequestParent): Deferred<OcrResponse> {
        return when (ocrRequestParent) {
            is OCRQARequest -> analyseOCRQAAsync(ocrRequestParent)
            is OcrRequest -> analyseOCRDemoAsync(ocrRequestParent)
        }
    }

    private suspend fun analyseOCRDemoAsync(ocrRequest: OcrRequest): Deferred<OcrResponse> {
        return apiService.analyseOCRDemoAsync(ocrRequest)
    }

    private suspend fun analyseOCRQAAsync(ocrRequest: OCRQARequest): Deferred<OcrResponse> {
        return apiService.analyseOCRQAAsync(ocrRequest)
    }
}