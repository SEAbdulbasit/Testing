package com.example.customscannerview.mlkit.modelclasses.ocr_response_demo


import com.google.gson.annotations.SerializedName

data class OcrResponse(
    @SerializedName("data")
    val `data`: DataX?
)