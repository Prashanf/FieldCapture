package com.fieldcapture.domain.usecase

import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.domain.repository.CaptureRepository
import javax.inject.Inject

class SyncCapturesUseCase @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val extractNameplateUseCase: ExtractNameplateUseCase,
    private val extractDamageUseCase: ExtractDamageUseCase,
    private val parseNoteUseCase: ParseNoteUseCase
) {
    suspend operator fun invoke(): Result<Int> = runCatching {
        // TODO: Load pending/failed captures, reload saved bitmaps from imagePath, invoke extraction, update statuses
        val pending = captureRepository.getPendingCaptures()
        var syncedCount = 0
        for (capture in pending) {
            captureRepository.updateCaptureStatus(capture.id, CaptureStatus.PROCESSING)
            // TODO: Execute extraction based on capture.type and mark COMPLETED or FAILED
            syncedCount++
        }
        syncedCount
    }
}
