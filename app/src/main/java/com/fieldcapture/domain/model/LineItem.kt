package com.fieldcapture.domain.model

data class LineItem(
    val code: LineItemCode,
    val description: String = code.description,
    val unit: String = code.unit,
    val estimatedQuantity: Double,
    val sourceCaptureIds: List<String>,
    val confidence: Double
)
