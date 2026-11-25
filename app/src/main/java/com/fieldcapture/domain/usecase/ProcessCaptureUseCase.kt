package com.fieldcapture.domain.usecase

import android.graphics.Bitmap
import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureMetadata
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.repository.CaptureRepository
import java.util.UUID
import javax.inject.Inject

class ProcessCaptureUseCase @Inject constructor(
    private val checkCaptureQualityUseCase: CheckCaptureQualityUseCase,
    private val extractNameplateUseCase: ExtractNameplateUseCase,
    private val extractDamageUseCase: ExtractDamageUseCase,
    private val parseNoteUseCase: ParseNoteUseCase,
    private val captureRepository: CaptureRepository
) {
    suspend operator fun invoke(
        jobId: String,
        type: CaptureType,
        bitmap: Bitmap? = null,
        imagePath: String? = null,
        metadata: CaptureMetadata = CaptureMetadata()
    ): Result<Capture> {
        // TODO: Phase 3/4 — run quality check on bitmap -> persist PENDING capture -> run extraction -> update status & store result
        val quality = bitmap?.let { checkCaptureQualityUseCase(it, type) }
        val captureId = UUID.randomUUID().toString()
        val initialCapture = Capture(
            id = captureId,
            jobId = jobId,
            type = type,
            timestamp = System.currentTimeMillis(),
            imagePath = imagePath,
            metadata = metadata,
            status = CaptureStatus.PENDING,
            qualityResult = quality
        )
        captureRepository.saveCapture(initialCapture)

        val extractionResult = when (type) {
            CaptureType.NAMEPLATE -> bitmap?.let { extractNameplateUseCase(it).getOrNull() }
            CaptureType.DAMAGE -> bitmap?.let { extractDamageUseCase(it).getOrNull() }
            CaptureType.NOTE -> metadata.noteText?.let { parseNoteUseCase(it).getOrNull() }
        }

        val finalStatus = if (extractionResult != null) CaptureStatus.COMPLETED else CaptureStatus.PENDING
        val updatedCapture = initialCapture.copy(
            status = finalStatus,
            extractionResult = extractionResult
        )
        return captureRepository.saveCapture(updatedCapture).map { updatedCapture }
    }
}
