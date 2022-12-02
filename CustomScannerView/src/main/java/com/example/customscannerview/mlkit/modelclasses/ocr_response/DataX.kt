package com.example.scannerview.modelclasses.ocr_response

data class DataX(
    val businesses_found: List<Any>,
    val members_found: List<Any>,
    val non_members_found: List<NonMembersFound>,
    val sender_found: List<SenderFound>,
    val suggestions: List<Any>
)