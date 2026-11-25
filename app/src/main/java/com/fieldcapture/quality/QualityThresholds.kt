package com.fieldcapture.quality

object QualityThresholds {
    const val BLUR_LAPLACIAN_VARIANCE_MIN = 100.0  // Below = too blurry. Derived from testing 50+ frames.
    const val BRIGHTNESS_MIN = 40                   // Below = too dark (0-255 scale)
    const val BRIGHTNESS_MAX = 240                  // Above = overexposed
    const val MIN_TEXT_REGION_PERCENT = 5.0         // For nameplates: at least 5% of frame should contain text-like regions
}
