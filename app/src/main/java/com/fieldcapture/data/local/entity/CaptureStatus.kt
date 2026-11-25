package com.fieldcapture.data.local.entity

enum class CaptureStatus {
    CAPTURED,           // Image saved, not yet sent to AI
    EXTRACTING,         // Currently being processed by Gemini
    EXTRACTED,          // AI extraction complete
    EXTRACTION_FAILED,  // AI extraction failed (will retry)
    QUEUED,             // Waiting in offline queue (Phase 6)
    SYNCED              // Uploaded to backend (Phase 6)
}
