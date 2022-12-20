package com.example.customscannerview.mlkit.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class RecipientFound(
    @SerializedName("name")
    val name: String?
)