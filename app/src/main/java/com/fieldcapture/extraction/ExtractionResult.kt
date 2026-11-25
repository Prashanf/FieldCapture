package com.fieldcapture.extraction

import com.google.gson.annotations.SerializedName

/** Sealed class for all Gemini AI extraction results. */
sealed class ExtractionResult {
    abstract val overallConfidence: Double
    abstract val rawJson: String
}

data class NameplateExtraction(
    @SerializedName("equipment_type") val equipmentType: String? = null,
    val manufacturer: String? = null,
    @SerializedName("model_number") val modelNumber: String? = null,
    @SerializedName("serial_number") val serialNumber: String? = null,
    @SerializedName("manufacture_date") val manufactureDate: String? = null,
    val capacity: String? = null,
    val voltage: String? = null,
    @SerializedName("additional_specs") val additionalSpecs: String? = null,
    val confidence: NameplateConfidence = NameplateConfidence(),
    @SerializedName("overall_confidence") override val overallConfidence: Double = 0.0,
    val notes: String? = null,
    val error: String? = null,
    val description: String? = null,
    override val rawJson: String = ""
) : ExtractionResult()

data class NameplateConfidence(
    @SerializedName("equipment_type") val equipmentType: Double = 0.0,
    val manufacturer: Double = 0.0,
    @SerializedName("model_number") val modelNumber: Double = 0.0,
    @SerializedName("serial_number") val serialNumber: Double = 0.0,
    @SerializedName("manufacture_date") val manufactureDate: Double = 0.0,
    val capacity: Double = 0.0
)

data class DamageExtraction(
    @SerializedName("damage_type") val damageType: String = "unknown",
    val severity: String = "unknown",
    val description: String = "",
    @SerializedName("affected_materials") val affectedMaterials: List<String> = emptyList(),
    @SerializedName("affected_area_estimate") val affectedAreaEstimate: String? = null,
    val room: String? = null,
    val surface: String? = null,
    @SerializedName("moisture_visible") val moistureVisible: Boolean = false,
    @SerializedName("mold_visible") val moldVisible: Boolean = false,
    @SerializedName("recommended_line_items") val recommendedLineItems: List<RecommendedLineItem> = emptyList(),
    val confidence: DamageConfidence = DamageConfidence(),
    @SerializedName("overall_confidence") override val overallConfidence: Double = 0.0,
    @SerializedName("safety_concerns") val safetyConcerns: String? = null,
    val notes: String? = null,
    val error: String? = null,
    override val rawJson: String = ""
) : ExtractionResult()

data class RecommendedLineItem(
    val code: String = "",
    val reason: String = "",
    val priority: String = "medium",
    @SerializedName("estimated_quantity") val estimatedQuantity: String? = null
)

data class DamageConfidence(
    @SerializedName("damage_type") val damageType: Double = 0.0,
    val severity: Double = 0.0,
    @SerializedName("affected_materials") val affectedMaterials: Double = 0.0,
    @SerializedName("line_items") val lineItems: Double = 0.0
)

data class NoteExtraction(
    @SerializedName("transcribed_note") val transcribedNote: String = "",
    val category: String = "general",
    @SerializedName("image_context") val imageContext: String? = null,
    @SerializedName("key_facts") val keyFacts: List<String> = emptyList(),
    val confidence: NoteConfidence = NoteConfidence(),
    @SerializedName("overall_confidence") override val overallConfidence: Double = 0.0,
    @SerializedName("follow_up_needed") val followUpNeeded: Boolean = false,
    @SerializedName("follow_up_reason") val followUpReason: String? = null,
    val error: String? = null,
    override val rawJson: String = ""
) : ExtractionResult()

data class NoteConfidence(
    val transcription: Double = 0.0,
    val category: Double = 0.0,
    @SerializedName("image_context") val imageContext: Double = 0.0
)
