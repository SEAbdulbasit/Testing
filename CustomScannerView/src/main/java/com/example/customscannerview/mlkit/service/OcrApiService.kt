package com.example.customscannerview.mlkit.service

import com.example.scannerview.modelclasses.ocr_request.OcrRequest
import com.example.scannerview.modelclasses.ocr_response.OcrResponse
import retrofit2.Call
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
    fun analyseOCRR(
        @Body data: OcrRequest
    ): Call<OcrResponse>


}