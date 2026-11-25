package com.fieldcapture.domain.usecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.QualityResult
import com.fieldcapture.quality.ImageQualityAnalyzer
import com.fieldcapture.quality.QualityCheckResult
import javax.inject.Inject

class CheckCaptureQualityUseCase @Inject constructor(
    private val imageQualityAnalyzer: ImageQualityAnalyzer
) {
    /**
     * Check quality of captured image bytes.
     * Returns QualityResult with pass/fail and detailed scores.
     */
    fun execute(imageBytes: ByteArray, captureType: CaptureType): QualityResult {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return QualityResult(
                isAcceptable = false,
                blurScore = 0.0,
                brightnessScore = 0,
                edgeDensity = 0.0,
                isBlurry = false,
                isDark = false,
                isOverexposed = false,
                isBadlyFramed = false,
                rejectionReason = "failed to decode image"
            )

        val result = imageQualityAnalyzer.analyze(bitmap, captureType)
        bitmap.recycle()
        return result.toDomain()
    }

    operator fun invoke(bitmap: Bitmap, captureType: CaptureType): QualityResult {
        return imageQualityAnalyzer.analyze(bitmap, captureType).toDomain()
    }

    private fun QualityCheckResult.toDomain(): QualityResult {
        return QualityResult(
            isAcceptable = isAcceptable,
            blurScore = blurScore,
            brightnessScore = brightnessScore,
            edgeDensity = edgeDensity,
            isBlurry = isBlurry,
            isDark = isDark,
            isOverexposed = isOverexposed,
            isBadlyFramed = isBadlyFramed,
            rejectionReason = rejectionReason
        )
    }
}
