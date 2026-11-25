package com.fieldcapture.quality

import android.graphics.Bitmap
import com.fieldcapture.domain.model.CaptureType
import javax.inject.Inject

class FramingAnalyzer @Inject constructor() {

    fun estimateTextRegionPercent(bitmap: Bitmap): Double {
        // TODO: Estimate high-contrast edge/text region density for nameplate framing validation
        return QualityThresholds.MIN_TEXT_REGION_PERCENT
    }

    fun isBadlyFramed(bitmap: Bitmap, captureType: CaptureType): Boolean {
        // TODO: Apply subject/edge framing heuristics (and MIN_TEXT_REGION_PERCENT check for NAMEPLATE)
        return if (captureType == CaptureType.NAMEPLATE) {
            estimateTextRegionPercent(bitmap) < QualityThresholds.MIN_TEXT_REGION_PERCENT
        } else {
            false
        }
    }
}
