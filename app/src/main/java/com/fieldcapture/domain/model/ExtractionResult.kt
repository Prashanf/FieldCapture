package com.fieldcapture.domain.model

sealed class ExtractionResult {
    abstract val rawResponse: String
    abstract val modelUsed: String
    abstract val processingTimeMs: Long

    data class NameplateData(
        val make: FieldWithConfidence<String>,
        val model: FieldWithConfidence<String>,
        val serialNumber: FieldWithConfidence<String>,
        val manufactureDate: FieldWithConfidence<String>,
        override val rawResponse: String = "",
        override val modelUsed: String = "gemini-1.5-flash",
        override val processingTimeMs: Long = 0L
    ) : ExtractionResult()

    data class DamageData(
        val room: FieldWithConfidence<String>,
        val surface: FieldWithConfidence<String>,
        val damageType: FieldWithConfidence<String>,
        val estimatedSizeSqft: FieldWithConfidence<Double>,
        val severity: FieldWithConfidence<String>,
        override val rawResponse: String = "",
        override val modelUsed: String = "gemini-1.5-flash",
        override val processingTimeMs: Long = 0L
    ) : ExtractionResult()

    data class NoteData(
        val text: String,
        val structuredFields: Map<String, String> = emptyMap(),
        override val rawResponse: String = "",
        override val modelUsed: String = "gemini-1.5-flash",
        override val processingTimeMs: Long = 0L
    ) : ExtractionResult()
}
