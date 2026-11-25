package com.fieldcapture.domain.usecase

import android.graphics.Bitmap
import com.fieldcapture.domain.model.ExtractionResult
import com.fieldcapture.domain.repository.ExtractionRepository
import javax.inject.Inject

class ExtractDamageUseCase @Inject constructor(
    private val extractionRepository: ExtractionRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<ExtractionResult.DamageData> {
        // TODO: Add retry/timeout handling and merge voice command room/surface hints
        return extractionRepository.extractDamage(bitmap)
    }
}
