package com.example.scannerview.modelclasses.ocr_response

data class ScanOutput(
    val address: Address,
    val courier_info: CourierInfo,
    val `data`: DataX,
    val item_info: ItemInfo,
    val package_id: String,
    val success: String
)