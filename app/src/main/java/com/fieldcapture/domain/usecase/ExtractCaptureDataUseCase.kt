package com.fieldcapture.domain.usecase

import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.extraction.ExtractionMetadata
import com.fieldcapture.extraction.ExtractionResult
import com.fieldcapture.extraction.ExtractionService
import javax.inject.Inject

class ExtractCaptureDataUseCase @Inject constructor(
    private val extractionService: ExtractionService
) {
    /**
     * Extract structured data from a captured image (or spoken note) using Gemini AI.
     */
    suspend fun execute(
        imageBytes: ByteArray?,
        captureType: CaptureType,
        metadata: ExtractionMetadata
    ): Result<ExtractionResult> {
        return extractionService.extract(imageBytes, captureType, metadata)
    }
}
