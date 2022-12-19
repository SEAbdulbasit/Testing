package com.example.customscannerview.mlkit.modelclasses.ocr_request


import com.google.gson.annotations.SerializedName

data class FrameX(
    @SerializedName("payload")
    val payload: String?,
    @SerializedName("type")
    val type: String?
)