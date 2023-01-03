package com.example.customscannerview.mlkit.service

import com.example.customscannerview.mlkit.modelclasses.OCRResponse
import com.example.customscannerview.mlkit.modelclasses.OcrResponseQA
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OCRQARequest
import com.example.customscannerview.mlkit.modelclasses.ocr_request.OcrRequest
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface OcrApiService {

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @POST
    suspend fun analyseOCRDemoAsync(
        @Body data: OcrRequest,
        @Header("x-api-key") apiKey: String,
        @Url url: String
    ): OCRResponse

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @POST
    suspend fun analyseOCRQAAsync(
        @Body data: OCRQARequest,
        @Header("x-api-key") apiKey: String,
        @Url url: String,
    ): OcrResponseQA


}