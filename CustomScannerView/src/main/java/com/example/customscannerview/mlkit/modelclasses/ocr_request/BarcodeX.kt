package com.example.customscannerview.mlkit.modelclasses.ocr_request


import com.google.gson.annotations.SerializedName

data class BarcodeX(
    @SerializedName("frames")
    val frames: List<List<FrameX>>?
)