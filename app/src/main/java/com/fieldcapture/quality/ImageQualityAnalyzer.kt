package com.fieldcapture.quality

import android.graphics.Bitmap
import com.fieldcapture.domain.model.CaptureType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main quality analyzer — runs blur, brightness, and framing checks on-device (<50ms typical).
 */
@Singleton
class ImageQualityAnalyzer @Inject constructor(
    private val blurDetector: BlurDetector,
    private val brightnessAnalyzer: BrightnessAnalyzer,
    private val framingAnalyzer: FramingAnalyzer
) {
    fun analyze(bitmap: Bitmap, captureType: CaptureType = CaptureType.DAMAGE): QualityCheckResult {
        val blurThreshold = QualityThresholds.blurThresholdFor(captureType.name)
        val blurResult = blurDetector.analyze(bitmap, blurThreshold)
        val brightnessResult = brightnessAnalyzer.analyze(bitmap)
        val framingResult = framingAnalyzer.analyze(bitmap)

        val isAcceptable = !blurResult.isBlurry &&
            brightnessResult.isAcceptable &&
            !framingResult.isBadlyFramed

        val rejectionReasons = mutableListOf<String>()
        if (blurResult.isBlurry) rejectionReasons.add("too blurry")
        if (brightnessResult.isDark) rejectionReasons.add("too dark")
        if (brightnessResult.isOverexposed) rejectionReasons.add("overexposed")
        if (framingResult.isBadlyFramed) rejectionReasons.add("badly framed")

        return QualityCheckResult(
            isAcceptable = isAcceptable,
            blurScore = blurResult.laplacianVariance,
            brightnessScore = brightnessResult.meanBrightness,
            edgeDensity = framingResult.centerEdgeDensity,
            isBlurry = blurResult.isBlurry,
            isDark = brightnessResult.isDark,
            isOverexposed = brightnessResult.isOverexposed,
            isBadlyFramed = framingResult.isBadlyFramed,
            rejectionReason = if (rejectionReasons.isNotEmpty()) {
                rejectionReasons.joinToString(" and ")
            } else {
                null
            }
        )
    }
}

data class QualityCheckResult(
    val isAcceptable: Boolean,
    val blurScore: Double,
    val brightnessScore: Int,
    val edgeDensity: Double,
    val isBlurry: Boolean,
    val isDark: Boolean,
    val isOverexposed: Boolean,
    val isBadlyFramed: Boolean,
    val rejectionReason: String?
)
