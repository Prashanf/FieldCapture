package com.fieldcapture.domain.model

data class FieldWithConfidence<T>(
    val value: T?,
    val confidence: Double
)
