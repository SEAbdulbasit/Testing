package com.example.scannerview.modelclasses.ocr_response

data class CourierInfo(
    val barcode_present: Boolean,
    val courier_from_barcode: Boolean,
    val courier_from_ocr: Boolean,
    val courier_name: String,
    val courier_name_detected: String,
    val dynamic_extracted_labels: List<Any>,
    val dynamic_labels: List<Any>,
    val location_based_labels: List<Any>,
    val miscellaneous: Miscellaneous,
    val preset_labels: List<Any>,
    val tracking_from_barcode: Boolean,
    val tracking_from_ocr: Boolean,
    val tracking_no: String,
    val weight_info: String
)