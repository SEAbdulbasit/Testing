package com.example.customscannerview.mlkit.modelclasses

import com.example.customscannerview.mlkit.modelclasses.ocr_response.Data
import com.google.gson.annotations.SerializedName

data class OcrResponseQA(
    @SerializedName("data") val data: Data?
)