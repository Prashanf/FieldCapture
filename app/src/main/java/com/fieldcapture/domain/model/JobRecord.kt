package com.fieldcapture.domain.model

import android.os.Build
import com.fieldcapture.util.Constants
import com.google.gson.annotations.SerializedName

/**
 * A complete Job Record representing one field inspection visit.
 *
 * Schema versioned ("1.0.0") for forward/backward compatibility.
 */
data class JobRecord(
    @SerializedName("schema_version") val schemaVersion: String = "1.0.0",
    @SerializedName("job_id") val jobId: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("completed_at") val completedAt: String?,
    @SerializedName("technician_name") val technicianName: String,
    @SerializedName("job_address") val jobAddress: String?,
    @SerializedName("job_type") val jobType: String = "water_damage_assessment",
    val equipment: List<EquipmentEntry>,
    @SerializedName("damage_assessments") val damageAssessments: List<DamageAssessmentEntry>,
    val notes: List<NoteEntry>,
    @SerializedName("line_items") val lineItems: List<LineItemEntry>,
    val summary: JobSummary,
    val metadata: JobMetadata = JobMetadata()
)

data class EquipmentEntry(
    @SerializedName("capture_id") val captureId: String,
    @SerializedName("image_path") val imagePath: String,
    @SerializedName("captured_at") val capturedAt: String,
    @SerializedName("equipment_type") val equipmentType: String?,
    val manufacturer: String?,
    @SerializedName("model_number") val modelNumber: String?,
    @SerializedName("serial_number") val serialNumber: String?,
    @SerializedName("manufacture_date") val manufactureDate: String?,
    val capacity: String?,
    val confidence: Double,
    @SerializedName("quality_scores") val qualityScores: QualityScores
)

data class DamageAssessmentEntry(
    @SerializedName("capture_id") val captureId: String,
    @SerializedName("image_path") val imagePath: String,
    @SerializedName("captured_at") val capturedAt: String,
    @SerializedName("damage_type") val damageType: String,
    val severity: String,
    val description: String,
    @SerializedName("affected_materials") val affectedMaterials: List<String>,
    @SerializedName("affected_area") val affectedArea: String?,
    val room: String?,
    val surface: String?,
    @SerializedName("moisture_visible") val moistureVisible: Boolean,
    @SerializedName("mold_visible") val moldVisible: Boolean,
    @SerializedName("safety_concerns") val safetyConcerns: String?,
    val confidence: Double,
    @SerializedName("quality_scores") val qualityScores: QualityScores
)

data class NoteEntry(
    @SerializedName("capture_id") val captureId: String,
    @SerializedName("image_path") val imagePath: String?,
    @SerializedName("captured_at") val capturedAt: String,
    @SerializedName("transcribed_text") val transcribedText: String,
    val category: String,
    @SerializedName("image_context") val imageContext: String?,
    @SerializedName("key_facts") val keyFacts: List<String>,
    val confidence: Double
)

data class LineItemEntry(
    @SerializedName("item_number") val itemNumber: Int,
    val code: String,
    @SerializedName("display_name") val displayName: String,
    val description: String,
    val category: String,
    val unit: String,
    @SerializedName("unit_price") val unitPrice: Double,
    val quantity: Double,
    val total: Double,
    val reason: String,
    val priority: String,
    @SerializedName("source_capture_id") val sourceCaptureId: String,
    val confidence: Double,
    @SerializedName("is_valid_code") val isValidCode: Boolean
)

data class QualityScores(
    @SerializedName("blur_score") val blurScore: Double,
    @SerializedName("brightness_score") val brightnessScore: Int,
    @SerializedName("edge_density") val edgeDensity: Double
)

data class JobSummary(
    @SerializedName("total_captures") val totalCaptures: Int,
    @SerializedName("nameplate_captures") val nameplateCaptures: Int,
    @SerializedName("damage_captures") val damageCaptures: Int,
    @SerializedName("note_captures") val noteCaptures: Int,
    @SerializedName("rejected_captures") val rejectedCaptures: Int,
    @SerializedName("total_line_items") val totalLineItems: Int,
    @SerializedName("estimate_subtotal") val estimateSubtotal: Double,
    @SerializedName("average_extraction_confidence") val averageExtractionConfidence: Double,
    @SerializedName("unknown_line_item_codes") val unknownLineItemCodes: List<String>,
    @SerializedName("duration_seconds") val durationSeconds: Long?
)

data class JobMetadata(
    @SerializedName("app_version") val appVersion: String = "1.0.0",
    @SerializedName("sdk_version") val sdkVersion: String = "DAT 1.0",
    @SerializedName("device_model") val deviceModel: String = Build.MODEL ?: "unknown",
    @SerializedName("android_version") val androidVersion: String = Build.VERSION.RELEASE ?: "unknown",
    @SerializedName("gemini_model") val geminiModel: String = Constants.GEMINI_MODEL,
    @SerializedName("quality_thresholds") val qualityThresholds: Map<String, Double> = mapOf(
        "blur_general" to 80.0,
        "blur_nameplate" to 100.0,
        "brightness_min" to 30.0,
        "brightness_max" to 240.0,
        "framing_edge_density_min" to 2.0
    )
)
