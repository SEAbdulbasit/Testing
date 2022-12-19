package com.example.customscannerview.mlkit.service

import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRQARequest
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OcrRequest
import com.example.customscannerview.mlkit.modelclasses.ocr_response.OcrResponse
import kotlinx.coroutines.Deferred
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OcrApiService {

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
        "x-api-key: XRI8HaRVOM5P9OS2Mhakq3voRSOs66hK6j7CSWUG"
    )
    @POST("https://v1.qa.packagex.xyz/iex/api/extract/match")
    suspend fun analyseOCRDemoAsync(
        @Body data: OcrRequest
    ): Deferred<OcrResponse>

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
        "x-api-key: key_stag_7da7b5e917tq2eCckhc5QnTr1SfpvFGjwbTfpu1SQYy242xPjBz2mk3hbtzN6eB85MftxVw1zj5K5XBF"
    )
    @POST("https://staging--api.packagex.io/v1/scans")
    suspend fun analyseOCRQAAsync(
        @Body data: OCRQARequest
    ): Deferred<OcrResponse>


}