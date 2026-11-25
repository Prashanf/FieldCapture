package com.fieldcapture.quality

import android.graphics.Bitmap
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.QualityResult
import javax.inject.Inject

class ImageQualityAnalyzer @Inject constructor(
    private val blurDetector: BlurDetector,
    private val brightnessAnalyzer: BrightnessAnalyzer,
    private val framingAnalyzer: FramingAnalyzer
) {
    fun analyze(bitmap: Bitmap, captureType: CaptureType = CaptureType.DAMAGE): QualityResult {
        val blurScore = blurDetector.computeLaplacianVariance(bitmap)
        val brightness = brightnessAnalyzer.computeMeanLuminance(bitmap)
        val textPercent = if (captureType == CaptureType.NAMEPLATE) {
            framingAnalyzer.estimateTextRegionPercent(bitmap)
        } else {
            0.0
        }

        return QualityResult(
            blurScore = blurScore,
            brightness = brightness,
            textRegionPercent = textPercent,
            isBlurry = blurScore < QualityThresholds.BLUR_LAPLACIAN_VARIANCE_MIN,
            isDark = brightnessAnalyzer.isTooDark(brightness),
            isOverexposed = brightnessAnalyzer.isOverexposed(brightness),
            isBadlyFramed = framingAnalyzer.isBadlyFramed(bitmap, captureType)
        )
    }
}
