package com.fieldcapture.domain.model

import com.fieldcapture.data.local.entity.CaptureStatus

data class Capture(
    val id: String,
    val jobId: String,
    val type: CaptureType,
    val timestampIso: String,
    val imagePath: String? = null,
    val noteText: String? = null,
    val status: CaptureStatus = CaptureStatus.PENDING,
    val quality: QualityResult? = null,
    val extraction: ExtractionResult? = null,
    val metadata: Map<String, String> = emptyMap()
)
