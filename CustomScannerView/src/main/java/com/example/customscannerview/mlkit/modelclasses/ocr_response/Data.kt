package com.example.scannerview.modelclasses.ocr_response

import com.example.customscannerview.mlkit.modelclasses.ocr_response.Output

data class Data(
    val call_type: String,
    val confVersion: String,
    val duplicate_uuid_validation: DuplicateUuidValidation,
    val is_cold_start: Boolean,
    val locationId: Int,
    val mailroom_id: Int,
    val message: String,
    val multiHops: Boolean,
    val output: Output,
    val status: Int,
    val uuid: String,
    val workflowId: Int
)