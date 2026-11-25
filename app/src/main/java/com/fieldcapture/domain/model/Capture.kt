package com.fieldcapture.domain.model

import com.fieldcapture.data.local.entity.CaptureStatus
import java.time.Instant
import java.util.UUID

data class Capture(
    val id: String = UUID.randomUUID().toString(),
    val jobId: String = "",
    val type: CaptureType,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val imageBytes: ByteArray? = null,
    val metadata: CaptureMetadata = CaptureMetadata(),
    val status: CaptureStatus = CaptureStatus.PENDING,
    val qualityResult: QualityResult? = null,
    val extractionResult: ExtractionResult? = null
) {
    val timestampIso: String
        get() = Instant.ofEpochMilli(timestamp).toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Capture) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class CaptureMetadata(
    val room: String? = null,
    val surface: String? = null,
    val noteText: String? = null,
    val voiceCommandRaw: String? = null
)
