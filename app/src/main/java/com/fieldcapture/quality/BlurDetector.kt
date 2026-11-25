package com.fieldcapture.quality

import android.graphics.Bitmap
import javax.inject.Inject

class BlurDetector @Inject constructor() {

    fun computeLaplacianVariance(bitmap: Bitmap): Double {
        // TODO: Convert bitmap to grayscale, apply 3x3 Laplacian kernel, and compute pixel variance
        return QualityThresholds.BLUR_LAPLACIAN_VARIANCE_MIN
    }

    fun isBlurry(bitmap: Bitmap): Boolean {
        return computeLaplacianVariance(bitmap) < QualityThresholds.BLUR_LAPLACIAN_VARIANCE_MIN
    }
}
