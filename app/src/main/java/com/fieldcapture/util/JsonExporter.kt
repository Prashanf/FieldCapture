package com.fieldcapture.util

import android.content.Context
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.extraction.DamageExtraction
import com.fieldcapture.extraction.NameplateExtraction
import com.fieldcapture.extraction.NoteExtraction
import com.google.gson.GsonBuilder
import java.io.File
import javax.inject.Inject

class JsonExporter @Inject constructor() {

    private val prettyGson = GsonBuilder().setPrettyPrinting().create()

    fun toSchemaJson(jobRecord: JobRecord): String {
        val payload = mapOf(
            "job_id" to jobRecord.jobId,
            "created_at" to jobRecord.createdAt,
            "technician_id" to jobRecord.technicianId,
            "location" to mapOf(
                "address" to jobRecord.location.address,
                "lat" to jobRecord.location.lat,
                "lng" to jobRecord.location.lng
            ),
            "session" to mapOf(
                "started_at" to jobRecord.session.startedAt,
                "ended_at" to jobRecord.session.endedAt,
                "device" to jobRecord.session.device,
                "total_captures" to jobRecord.session.totalCaptures,
                "retakes" to jobRecord.session.retakes
            ),
            "captures" to jobRecord.captures.map { it.toSchemaMap() },
            "line_items" to jobRecord.lineItems.map { item ->
                mapOf(
                    "code" to item.code.name,
                    "description" to item.description,
                    "unit" to item.unit,
                    "estimated_quantity" to item.estimatedQuantity,
                    "source_capture_ids" to item.sourceCaptureIds,
                    "confidence" to item.confidence
                )
            },
            "notes" to jobRecord.notes.map { note ->
                mapOf(
                    "text" to note.text,
                    "structured" to note.structured,
                    "timestamp" to note.timestamp
                )
            }
        )
        return prettyGson.toJson(payload)
    }

    fun exportToFile(context: Context, jobRecord: JobRecord): Result<File> = runCatching {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "job_record_${jobRecord.jobId}.json")
        file.writeText(toSchemaJson(jobRecord))
        file
    }

    private fun Capture.toSchemaMap(): Map<String, Any?> {
        val qualityMap = qualityResult?.let {
            mapOf(
                "blur_score" to it.blurScore,
                "brightness" to it.brightnessScore,
                "is_acceptable" to it.isAcceptable
            )
        }
        val extractionMap = extractionResult?.let { ext ->
            val fields: Map<String, Any?> = when (ext) {
                is NameplateExtraction -> mapOf(
                    "equipment_type" to mapOf("value" to ext.equipmentType, "confidence" to ext.confidence.equipmentType),
                    "manufacturer" to mapOf("value" to ext.manufacturer, "confidence" to ext.confidence.manufacturer),
                    "model_number" to mapOf("value" to ext.modelNumber, "confidence" to ext.confidence.modelNumber),
                    "serial_number" to mapOf("value" to ext.serialNumber, "confidence" to ext.confidence.serialNumber),
                    "manufacture_date" to mapOf("value" to ext.manufactureDate, "confidence" to ext.confidence.manufactureDate),
                    "capacity" to mapOf("value" to ext.capacity, "confidence" to ext.confidence.capacity)
                )
                is DamageExtraction -> mapOf(
                    "damage_type" to mapOf("value" to ext.damageType, "confidence" to ext.confidence.damageType),
                    "severity" to mapOf("value" to ext.severity, "confidence" to ext.confidence.severity),
                    "room" to mapOf("value" to ext.room, "confidence" to ext.overallConfidence),
                    "surface" to mapOf("value" to ext.surface, "confidence" to ext.overallConfidence),
                    "affected_materials" to mapOf("value" to ext.affectedMaterials, "confidence" to ext.confidence.affectedMaterials)
                )
                is NoteExtraction -> mapOf(
                    "transcribed_note" to ext.transcribedNote,
                    "category" to ext.category,
                    "key_facts" to ext.keyFacts
                )
            }
            mapOf(
                "fields" to fields,
                "raw_response" to ext.rawJson,
                "model_used" to Constants.GEMINI_MODEL,
                "overall_confidence" to ext.overallConfidence
            )
        }

        return mapOf(
            "id" to id,
            "type" to type.jsonValue,
            "timestamp" to timestampIso,
            "image_path" to imagePath.orEmpty(),
            "quality" to qualityMap,
            "extraction" to extractionMap
        )
    }
}
