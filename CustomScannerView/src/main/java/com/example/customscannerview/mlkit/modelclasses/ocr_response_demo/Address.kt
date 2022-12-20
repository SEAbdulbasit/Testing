package com.example.customscannerview.mlkit.modelclasses.ocr_response_demo


import com.google.gson.annotations.SerializedName

data class Address(
    @SerializedName("city")
    val city: Any?,
    @SerializedName("coordinates")
    val coordinates: Any?,
    @SerializedName("country")
    val country: Any?,
    @SerializedName("country_code")
    val countryCode: Any?,
    @SerializedName("formatted_address")
    val formattedAddress: String?,
    @SerializedName("hash")
    val hash: Any?,
    @SerializedName("id")
    val id: Any?,
    @SerializedName("line1")
    val line1: Any?,
    @SerializedName("line2")
    val line2: Any?,
    @SerializedName("object")
    val objectX: String?,
    @SerializedName("postal_code")
    val postalCode: Any?,
    @SerializedName("state")
    val state: Any?,
    @SerializedName("state_code")
    val stateCode: Any?,
    @SerializedName("textarea")
    val textarea: String?,
    @SerializedName("timezone")
    val timezone: Any?,
    @SerializedName("verified")
    val verified: Boolean?
)