package com.fieldcapture.domain.usecase

import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.DamageAssessmentEntry
import com.fieldcapture.domain.model.EquipmentEntry
import com.fieldcapture.domain.model.JobMetadata
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.model.JobSummary
import com.fieldcapture.domain.model.LineItemCode
import com.fieldcapture.domain.model.LineItemEntry
import com.fieldcapture.domain.model.NoteEntry
import com.fieldcapture.domain.model.QualityScores
import com.fieldcapture.extraction.DamageExtraction
import com.fieldcapture.extraction.NameplateExtraction
import com.fieldcapture.extraction.NoteExtraction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Builds a complete JobRecord from all captures and their extraction results.
 */
class BuildJobRecordUseCase @Inject constructor(
    private val mapToLineItemsUseCase: MapToLineItemsUseCase
) {
    private fun newIsoFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    fun execute(
        jobId: String,
        technicianName: String,
        jobAddress: String?,
        captures: List<Capture>,
        jobStartTime: Long,
        jobEndTime: Long? = null,
        rejectedCaptures: Int = 0
    ): JobRecord {
        val isoFormat = newIsoFormat()

        val nameplateCaps = captures.filter { it.type == CaptureType.NAMEPLATE && it.extractionResult != null }
        val damageCaps = captures.filter { it.type == CaptureType.DAMAGE && it.extractionResult != null }
        val noteCaps = captures.filter { it.type == CaptureType.NOTE && it.extractionResult != null }

        val equipment = nameplateCaps.mapNotNull { cap ->
            val ext = cap.extractionResult as? NameplateExtraction ?: return@mapNotNull null
            if (ext.error != null) return@mapNotNull null
            EquipmentEntry(
                captureId = cap.id,
                imagePath = cap.imagePath.orEmpty(),
                capturedAt = isoFormat.format(Date(cap.timestamp)),
                equipmentType = ext.equipmentType,
                manufacturer = ext.manufacturer,
                modelNumber = ext.modelNumber,
                serialNumber = ext.serialNumber,
                manufactureDate = ext.manufactureDate,
                capacity = ext.capacity,
                confidence = ext.overallConfidence,
                qualityScores = buildQualityScores(cap)
            )
        }

        val damageAssessments = damageCaps.mapNotNull { cap ->
            val ext = cap.extractionResult as? DamageExtraction ?: return@mapNotNull null
            if (ext.error != null) return@mapNotNull null
            DamageAssessmentEntry(
                captureId = cap.id,
                imagePath = cap.imagePath.orEmpty(),
                capturedAt = isoFormat.format(Date(cap.timestamp)),
                damageType = ext.damageType,
                severity = ext.severity,
                description = ext.description,
                affectedMaterials = ext.affectedMaterials,
                affectedArea = ext.affectedAreaEstimate,
                room = ext.room ?: cap.metadata.room,
                surface = ext.surface ?: cap.metadata.surface,
                moistureVisible = ext.moistureVisible,
                moldVisible = ext.moldVisible,
                safetyConcerns = ext.safetyConcerns,
                confidence = ext.overallConfidence,
                qualityScores = buildQualityScores(cap)
            )
        }

        val notes = noteCaps.mapNotNull { cap ->
            val ext = cap.extractionResult as? NoteExtraction ?: return@mapNotNull null
            NoteEntry(
                captureId = cap.id,
                imagePath = cap.imagePath,
                capturedAt = isoFormat.format(Date(cap.timestamp)),
                transcribedText = ext.transcribedNote,
                category = ext.category,
                imageContext = ext.imageContext,
                keyFacts = ext.keyFacts,
                confidence = ext.overallConfidence
            )
        }

        val damageExtractionsWithIds = damageCaps.mapNotNull { cap ->
            val ext = cap.extractionResult as? DamageExtraction ?: return@mapNotNull null
            if (ext.error != null) return@mapNotNull null
            Pair(cap.id, ext)
        }
        val lineItemSummary = mapToLineItemsUseCase.execute(damageExtractionsWithIds)

        val lineItems = lineItemSummary.lineItems.mapIndexed { index, item ->
            LineItemEntry(
                itemNumber = index + 1,
                code = item.code,
                displayName = item.displayName,
                description = item.description,
                category = LineItemCode.fromString(item.code)?.category?.displayName ?: "Other",
                unit = item.unit,
                unitPrice = item.unitPrice,
                quantity = item.quantity,
                total = item.total,
                reason = item.reason,
                priority = item.priority,
                sourceCaptureId = item.sourceCapture,
                confidence = item.confidence,
                isValidCode = item.isValidCode
            )
        }

        val allExtractionConfidences = captures
            .mapNotNull { it.extractionResult?.overallConfidence }
        val avgConfidence = if (allExtractionConfidences.isNotEmpty()) {
            allExtractionConfidences.average()
        } else {
            0.0
        }

        val endTime = jobEndTime ?: System.currentTimeMillis()

        val summary = JobSummary(
            totalCaptures = captures.size,
            nameplateCaptures = nameplateCaps.size,
            damageCaptures = damageCaps.size,
            noteCaptures = noteCaps.size,
            rejectedCaptures = rejectedCaptures,
            totalLineItems = lineItems.size,
            estimateSubtotal = lineItemSummary.subtotal,
            averageExtractionConfidence = (avgConfidence * 100.0).roundToLong() / 100.0,
            unknownLineItemCodes = lineItemSummary.unknownCodes,
            durationSeconds = ((endTime - jobStartTime) / 1000L).coerceAtLeast(0L)
        )

        return JobRecord(
            jobId = jobId,
            createdAt = isoFormat.format(Date(jobStartTime)),
            completedAt = isoFormat.format(Date(endTime)),
            technicianName = technicianName,
            jobAddress = jobAddress,
            equipment = equipment,
            damageAssessments = damageAssessments,
            notes = notes,
            lineItems = lineItems,
            summary = summary,
            metadata = JobMetadata()
        )
    }

    private fun buildQualityScores(capture: Capture): QualityScores {
        val qr = capture.qualityResult
        return QualityScores(
            blurScore = qr?.blurScore ?: 0.0,
            brightnessScore = qr?.brightnessScore ?: 0,
            edgeDensity = qr?.edgeDensity ?: 0.0
        )
    }
}
