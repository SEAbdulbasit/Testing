package com.example.customscannerview.mlkit.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class SenderAddress(
    @SerializedName("addressLine1")
    val addressLine1: String?,
    @SerializedName("city")
    val city: String?,
    @SerializedName("completeAddress")
    val completeAddress: String?,
    @SerializedName("country")
    val country: String?,
    @SerializedName("state")
    val state: String?,
    @SerializedName("zipCodeLine")
    val zipCodeLine: String?,
    @SerializedName("zipcode")
    val zipcode: String?
)