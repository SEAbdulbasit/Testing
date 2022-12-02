package com.example.scannerview.modelclasses.ocr_response

data class SenderAddress(
    val address_line_1: String,
    val city: String,
    val complete_address: String,
    val state: String,
    val zip_code_line: String,
    val zipcode: String
)