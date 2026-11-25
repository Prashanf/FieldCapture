package com.fieldcapture.domain.usecase

import android.graphics.Bitmap
import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureMetadata
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.repository.CaptureRepository
import com.fieldcapture.extraction.ExtractionMetadata
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

class ProcessCaptureUseCase @Inject constructor(
    private val checkCaptureQualityUseCase: CheckCaptureQualityUseCase,
    private val extractCaptureDataUseCase: ExtractCaptureDataUseCase,
    private val captureRepository: CaptureRepository
) {
    suspend operator fun invoke(
        jobId: String,
        type: CaptureType,
        bitmap: Bitmap? = null,
        imagePath: String? = null,
        metadata: CaptureMetadata = CaptureMetadata()
    ): Result<Capture> {
        val quality = bitmap?.let { checkCaptureQualityUseCase(it, type) }
        val captureId = UUID.randomUUID().toString()
        val initialCapture = Capture(
            id = captureId,
            jobId = jobId,
            type = type,
            timestamp = System.currentTimeMillis(),
            imagePath = imagePath,
            metadata = metadata,
            status = CaptureStatus.CAPTURED,
            qualityResult = quality
        )
        captureRepository.saveCapture(initialCapture)

        val imageBytes = bitmap?.let { bmp ->
            ByteArrayOutputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.toByteArray()
            }
        }

        val extractionResult = extractCaptureDataUseCase.execute(
            imageBytes = imageBytes,
            captureType = type,
            metadata = ExtractionMetadata(
                room = metadata.room,
                surface = metadata.surface,
                spokenText = metadata.noteText
            )
        ).getOrNull()

        val finalStatus = if (extractionResult != null) CaptureStatus.EXTRACTED else CaptureStatus.CAPTURED
        val updatedCapture = initialCapture.copy(
            status = finalStatus,
            extractionResult = extractionResult
        )
        return captureRepository.saveCapture(updatedCapture).map { updatedCapture }
    }
}
