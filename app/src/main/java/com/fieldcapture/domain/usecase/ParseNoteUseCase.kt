package com.fieldcapture.domain.usecase

import com.fieldcapture.domain.model.ExtractionResult
import com.fieldcapture.domain.repository.ExtractionRepository
import javax.inject.Inject

class ParseNoteUseCase @Inject constructor(
    private val extractionRepository: ExtractionRepository
) {
    suspend operator fun invoke(rawText: String): Result<ExtractionResult.NoteData> {
        // TODO: Validate non-blank note and handle offline fallback structuring
        return extractionRepository.parseNote(rawText)
    }
}
