package com.example.customscannerview.mlkit.modelclasses.ocr_request

import com.example.scannerview.modelclasses.ocr_request.Barcode

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
) : OCRRequestParent