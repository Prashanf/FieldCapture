package com.fieldcapture.domain.usecase

import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.domain.repository.CaptureRepository
import com.fieldcapture.extraction.ExtractionMetadata
import java.io.File
import javax.inject.Inject

class SyncCapturesUseCase @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val extractCaptureDataUseCase: ExtractCaptureDataUseCase
) {
    suspend operator fun invoke(): Result<Int> = runCatching {
        val pending = captureRepository.getPendingCaptures()
        var syncedCount = 0
        for (capture in pending) {
            captureRepository.updateCaptureStatus(capture.id, CaptureStatus.EXTRACTING)
            val imageBytes = capture.imagePath?.let { path ->
                File(path).takeIf { it.exists() }?.readBytes()
            }
            val result = extractCaptureDataUseCase.execute(
                imageBytes = imageBytes,
                captureType = capture.type,
                metadata = ExtractionMetadata(
                    room = capture.metadata.room,
                    surface = capture.metadata.surface,
                    spokenText = capture.metadata.noteText,
                    captureTimestamp = capture.timestamp
                )
            )
            result.fold(
                onSuccess = { extraction ->
                    captureRepository.updateExtraction(capture.id, extraction, CaptureStatus.EXTRACTED)
                    syncedCount++
                },
                onFailure = {
                    captureRepository.updateCaptureStatus(capture.id, CaptureStatus.EXTRACTION_FAILED)
                }
            )
        }
        syncedCount
    }
}
