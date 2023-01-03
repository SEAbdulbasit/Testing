package com.example.customscannerview.mlkit.enums

sealed interface ViewType {
    object RECTANGLE : ViewType
    object SQUARE : ViewType
    object FULLSCRREN : ViewType
}

