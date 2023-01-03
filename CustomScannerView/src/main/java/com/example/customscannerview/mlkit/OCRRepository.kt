package com.example.customscannerview.mlkit

import com.example.customscannerview.mlkit.modelclasses.OCRResponse
import com.example.customscannerview.mlkit.modelclasses.OcrResponseQA
import com.example.customscannerview.mlkit.modelclasses.ocr_request.BarcodeX
import com.example.customscannerview.mlkit.modelclasses.ocr_request.FrameX
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRQARequest
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OcrRequest
import com.example.customscannerview.mlkit.service.OcrApiService
import com.example.customscannerview.mlkit.service.ServiceBuilder
import com.google.mlkit.vision.barcode.common.Barcode

class OCRRepository {
    private val apiService = ServiceBuilder.buildService(OcrApiService::class.java)

    suspend fun analyseOCRAsync(ocrRequest: OcrRequest): OCRResponse {
        return apiService.analyseOCRDemoAsync(
            data = ocrRequest, apiKey = getApiKey(), url = ServiceBuilder.getUrl(getEnvironment())
        )
    }

    suspend fun analyseOCRAsyncQA(ocrRequestParent: OCRQARequest): OcrResponseQA {
        return apiService.analyseOCRQAAsync(
            data = ocrRequestParent,
            apiKey = getApiKey(),
            url = ServiceBuilder.getQAUrl(getEnvironment())
        )
    }

    fun getQARequest(barcodesList: List<Barcode>, baseImage: String): OCRQARequest {
        return OCRQARequest(
            barcode = BarcodeX(listOf(barcodesList.map { barcode ->
                FrameX(
                    barcode.displayValue, barcode.format.toString()
                )
            }.toList())),
            callType = "extract",
            extractTime = "2022-08-29T05:58:28.902Z",
            image = baseImage,
            orgUuid = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            platform = "API"
        )
    }

    fun getDemoRequest(barcodesList: List<Barcode>, baseImage: String): OcrRequest {
        return OcrRequest(image_url = "data:image/jpeg;base64,$baseImage",
            type = "shipping_label",
            barcode_values = barcodesList.map { it.displayValue!! })
    }

    private fun getApiKey(): String {
        val visionSDK = VisionSDK.getInstance()
        if (visionSDK.apiKey == null) {
            throw Exception("Api key not set")
        }
        return visionSDK.apiKey!!
    }

    private fun getEnvironment(): Environment {
        val visionSDK = VisionSDK.getInstance()
        if (visionSDK.environment == null) {
            throw Exception("Environment not set")
        }
        return visionSDK.environment!!
    }
}