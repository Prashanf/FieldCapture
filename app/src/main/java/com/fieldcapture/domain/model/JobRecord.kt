package com.fieldcapture.domain.model

data class JobLocation(
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class SessionMetadata(
    val startedAt: String,
    val endedAt: String? = null,
    val device: String = "mock_rayban_meta_gen2",
    val totalCaptures: Int = 0,
    val retakes: Int = 0
)

data class JobNote(
    val text: String,
    val structured: Map<String, String> = emptyMap(),
    val timestamp: String
)

data class JobRecord(
    val jobId: String,
    val createdAt: String,
    val technicianId: String,
    val location: JobLocation = JobLocation(),
    val session: SessionMetadata,
    val captures: List<Capture> = emptyList(),
    val lineItems: List<LineItem> = emptyList(),
    val notes: List<JobNote> = emptyList()
)
