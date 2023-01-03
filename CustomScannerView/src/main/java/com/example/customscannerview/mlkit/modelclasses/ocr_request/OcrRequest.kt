package com.example.customscannerview.mlkit.modelclasses.ocr_request

data class OcrRequest(
    val image_url: String,
    val type: String,
    val barcode_values: List<String>,
)