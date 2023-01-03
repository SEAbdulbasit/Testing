package com.example.customscannerview.mlkit.modelclasses.ocr_request


import com.google.gson.annotations.SerializedName

data class OCRQARequest(
    @SerializedName("barcode")
    val barcode: BarcodeX?,
    @SerializedName("callType")
    val callType: String?,
    @SerializedName("extractTime")
    val extractTime: String?,
    @SerializedName("image")
    val image: String?,
    @SerializedName("orgUuid")
    val orgUuid: String?,
    @SerializedName("platform")
    val platform: String?
)