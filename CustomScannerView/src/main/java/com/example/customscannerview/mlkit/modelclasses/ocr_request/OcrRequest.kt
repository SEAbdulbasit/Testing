package com.example.scannerview.modelclasses.ocr_request

data class OcrRequest(
    val barcode: Barcode,
    val callType: String,
    val device: String,
    val image: String,
    val imageUpload: Boolean,
    val locationId: Int,
    val mailroomId: Int,
    val memberId: Int,
    val platform: String,
    val scanOut: Boolean,
    val scanTime: String,
    val userEmail: String
)