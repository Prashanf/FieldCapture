package com.fieldcapture.domain.usecase

import android.graphics.Bitmap
import com.fieldcapture.domain.model.ExtractionResult
import com.fieldcapture.domain.repository.ExtractionRepository
import javax.inject.Inject

class ExtractNameplateUseCase @Inject constructor(
    private val extractionRepository: ExtractionRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<ExtractionResult.NameplateData> {
        // TODO: Add retry/timeout handling and post-extraction validation
        return extractionRepository.extractNameplate(bitmap)
    }
}
