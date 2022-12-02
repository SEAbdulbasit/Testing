package com.example.scannerview.modelclasses.ocr_response

data class Output(
    val blurr_value: Any,
    val duplicate_package_flag: Boolean,
    val duplicate_packages: List<Any>,
    val platform: String,
    val scan_output: ScanOutput,
    val time_logs: TimeLogs
)