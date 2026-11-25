package com.fieldcapture.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.repository.CaptureRepository
import com.fieldcapture.domain.usecase.ExtractCaptureDataUseCase
import com.fieldcapture.extraction.ExtractionMetadata
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * Background worker that processes queued extraction requests.
 */
@HiltWorker
class ExtractionQueueWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val captureRepository: CaptureRepository,
    private val extractCaptureDataUseCase: ExtractCaptureDataUseCase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "ExtractionQueueWorker"
        const val WORK_NAME = "extraction_queue_processing"
        const val PERIODIC_WORK_NAME = "extraction_queue_periodic"
        private const val DELAY_BETWEEN_EXTRACTIONS_MS = 2000L
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting queue processing...")

        val pendingCaptures = captureRepository.getCapturesByStatus(CaptureStatus.QUEUED) +
            captureRepository.getCapturesByStatus(CaptureStatus.EXTRACTION_FAILED)

        if (pendingCaptures.isEmpty()) {
            Log.i(TAG, "No pending captures to process")
            return Result.success()
        }

        Log.i(TAG, "Processing ${pendingCaptures.size} queued captures")

        var successCount = 0
        var failCount = 0

        for (capture in pendingCaptures) {
            captureRepository.updateCaptureStatus(capture.id, CaptureStatus.EXTRACTING)

            val metadata = ExtractionMetadata(
                room = capture.metadata.room,
                surface = capture.metadata.surface,
                spokenText = capture.metadata.noteText,
                captureTimestamp = capture.timestamp
            )

            val imageBytes = captureRepository.loadImageBytes(capture.id)
            if (imageBytes == null && capture.type != CaptureType.NOTE) {
                Log.e(TAG, "Could not load image for capture ${capture.id}")
                captureRepository.updateCaptureStatus(capture.id, CaptureStatus.EXTRACTION_FAILED)
                failCount++
                continue
            }

            val result = extractCaptureDataUseCase.execute(
                imageBytes = imageBytes,
                captureType = capture.type,
                metadata = metadata
            )

            result.fold(
                onSuccess = { extractionResult ->
                    captureRepository.updateExtraction(
                        capture.id,
                        extractionResult,
                        CaptureStatus.EXTRACTED
                    )
                    successCount++
                    Log.d(TAG, "Extracted ${capture.id}: confidence=${extractionResult.overallConfidence}")
                },
                onFailure = { error ->
                    captureRepository.updateCaptureStatus(capture.id, CaptureStatus.EXTRACTION_FAILED)
                    failCount++
                    Log.e(TAG, "Failed to extract ${capture.id}", error)
                }
            )

            delay(DELAY_BETWEEN_EXTRACTIONS_MS)
        }

        Log.i(TAG, "Queue processing complete: $successCount succeeded, $failCount failed")

        return if (failCount > 0 && successCount == 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
