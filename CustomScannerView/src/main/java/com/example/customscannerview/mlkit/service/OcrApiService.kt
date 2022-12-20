package com.example.customscannerview.mlkit.service

import com.example.customscannerview.mlkit.modelclasses.OCRResponseDemo
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRQARequest
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OcrRequest
import com.example.customscannerview.mlkit.modelclasses.OcrResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OcrApiService {

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
        "x-api-key: key_stag_7da7b5e917tq2eCckhc5QnTr1SfpvFGjwbTfpu1SQYy242xPjBz2mk3hbtzN6eB85MftxVw1zj5K5XBF"
    )
    @POST("https://staging--api.packagex.io/v1/scans")
    suspend fun analyseOCRDemoAsync(
        @Body data: OcrRequest
    ): OCRResponseDemo

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
        "x-api-key: knF8L0CXTX4brmUCxvPzaauERYMshVg35yL6HIl6"
    )
    @POST("https://v1.packagex.io/iex/api/extract")
    suspend fun analyseOCRQAAsync(
        @Body data: OCRQARequest
    ): OcrResponse


}