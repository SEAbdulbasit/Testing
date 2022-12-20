package com.example.customscannerview.mlkit.modelclasses.ocr_response_demo


import com.google.gson.annotations.SerializedName

data class Dimensions(
    @SerializedName("height")
    val height: Any?,
    @SerializedName("length")
    val length: Any?,
    @SerializedName("width")
    val width: Any?
)