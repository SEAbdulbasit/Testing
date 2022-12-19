package com.example.customscannerview.mlkit.modelclasses.ocr_response

import com.example.scannerview.modelclasses.ocr_response.ScanOutput
import com.example.scannerview.modelclasses.ocr_response.TimeLogs

data class Output(
    val blurr_value: Any,
    val duplicate_package_flag: Boolean,
    val duplicate_packages: List<Any>,
    val platform: String,
    val scan_output: ScanOutput,
    val time_logs: TimeLogs
)