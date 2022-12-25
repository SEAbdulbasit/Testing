package com.example.customscannerview.mlkit

import com.example.customscannerview.mlkit.modelclasses.OCRResponseDemo
import com.example.customscannerview.mlkit.modelclasses.OCRResponseParent
import com.example.customscannerview.mlkit.modelclasses.OcrResponse
import com.example.customscannerview.mlkit.modelclasses.ocr_request.BarcodeX
import com.example.customscannerview.mlkit.modelclasses.ocr_request.FrameX
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRQARequest
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRRequestParent
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OcrRequest
import com.example.customscannerview.mlkit.service.OcrApiService
import com.google.mlkit.vision.barcode.common.Barcode

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

    fun getOCRRequest(
        barcodesList: List<Barcode>, baseImage: String, isQAVariant: Boolean
    ): OCRRequestParent {
        return if (isQAVariant) {
            OCRQARequest(
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

        } else {
            OcrRequest(image_url = "data:image/jpeg;base64,$baseImage",
                type = "shipping_label",
                barcode_values = barcodesList.map { it.displayValue!! })
        }
    }
}