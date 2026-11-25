package com.fieldcapture.quality

import android.graphics.Bitmap
import javax.inject.Inject

class BrightnessAnalyzer @Inject constructor() {

    fun computeMeanLuminance(bitmap: Bitmap): Int {
        // TODO: Sample pixels and compute average perceived luminance (0-255 scale)
        return 128
    }

    fun isTooDark(luminance: Int): Boolean = luminance < QualityThresholds.BRIGHTNESS_MIN

    fun isOverexposed(luminance: Int): Boolean = luminance > QualityThresholds.BRIGHTNESS_MAX
}
