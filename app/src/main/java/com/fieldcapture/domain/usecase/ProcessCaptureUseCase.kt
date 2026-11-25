package com.fieldcapture.domain.usecase

import android.graphics.Bitmap
import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.repository.CaptureRepository
import java.time.Instant
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
        noteText: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Result<Capture> {
        // TODO: Run quality check on bitmap -> persist PENDING capture -> run extraction -> update status & store result
        val quality = bitmap?.let { checkCaptureQualityUseCase(it, type) }
        val captureId = UUID.randomUUID().toString()
        val initialCapture = Capture(
            id = captureId,
            jobId = jobId,
            type = type,
            timestampIso = Instant.now().toString(),
            imagePath = imagePath,
            noteText = noteText,
            status = CaptureStatus.PENDING,
            quality = quality,
            metadata = metadata
        )
        captureRepository.saveCapture(initialCapture)

        val extractionResult = when (type) {
            CaptureType.NAMEPLATE -> bitmap?.let { extractNameplateUseCase(it).getOrNull() }
            CaptureType.DAMAGE -> bitmap?.let { extractDamageUseCase(it).getOrNull() }
            CaptureType.NOTE -> noteText?.let { parseNoteUseCase(it).getOrNull() }
        }

        val finalStatus = if (extractionResult != null) CaptureStatus.UPLOADED else CaptureStatus.PENDING
        val updatedCapture = initialCapture.copy(
            status = finalStatus,
            extraction = extractionResult
        )
        return captureRepository.saveCapture(updatedCapture).map { updatedCapture }
    }
}
