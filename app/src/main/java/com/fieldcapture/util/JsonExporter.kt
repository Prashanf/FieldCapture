package com.fieldcapture.util

import android.content.Context
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.ExtractionResult
import com.fieldcapture.domain.model.JobRecord
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
                is ExtractionResult.NameplateData -> mapOf(
                    "make" to ext.make,
                    "model" to ext.model,
                    "serial_number" to ext.serialNumber,
                    "manufacture_date" to ext.manufactureDate
                )
                is ExtractionResult.DamageData -> mapOf(
                    "room" to ext.room,
                    "surface" to ext.surface,
                    "damage_type" to ext.damageType,
                    "estimated_size_sqft" to ext.estimatedSizeSqft,
                    "severity" to ext.severity
                )
                is ExtractionResult.NoteData -> ext.structuredFields
            }
            mapOf(
                "fields" to fields,
                "raw_response" to ext.rawResponse,
                "model_used" to ext.modelUsed,
                "processing_time_ms" to ext.processingTimeMs
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
