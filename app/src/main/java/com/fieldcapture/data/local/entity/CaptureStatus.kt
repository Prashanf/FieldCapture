package com.fieldcapture.data.local.entity

enum class CaptureStatus {
    PENDING,      // Captured, waiting for quality check
    PROCESSING,   // Being sent to AI for extraction
    COMPLETED,    // Extraction done
    FAILED,       // Extraction failed
    QUEUED        // Offline — queued for later processing
}
