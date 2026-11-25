package com.fieldcapture.domain.usecase

import android.graphics.Bitmap
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.QualityResult
import com.fieldcapture.quality.ImageQualityAnalyzer
import javax.inject.Inject

class CheckCaptureQualityUseCase @Inject constructor(
    private val imageQualityAnalyzer: ImageQualityAnalyzer
) {
    operator fun invoke(bitmap: Bitmap, captureType: CaptureType): QualityResult {
        // TODO: Pass captureType-specific framing requirements (e.g., text region check for NAMEPLATE)
        return imageQualityAnalyzer.analyze(bitmap, captureType)
    }
}
