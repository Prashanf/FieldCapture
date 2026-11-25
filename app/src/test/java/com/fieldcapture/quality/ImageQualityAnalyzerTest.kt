package com.fieldcapture.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageQualityAnalyzerTest {

    @Test
    fun `quality thresholds match documented defaults`() {
        assertEquals(100.0, QualityThresholds.BLUR_LAPLACIAN_VARIANCE_MIN, 0.001)
        assertEquals(40, QualityThresholds.BRIGHTNESS_MIN)
        assertEquals(240, QualityThresholds.BRIGHTNESS_MAX)
        assertEquals(5.0, QualityThresholds.MIN_TEXT_REGION_PERCENT, 0.001)
    }

    @Test
    fun `brightness analyzer flags dark and overexposed thresholds`() {
        val analyzer = BrightnessAnalyzer()
        assertTrue(analyzer.isTooDark(20))
        assertTrue(analyzer.isOverexposed(250))
    }

    // TODO: Add Robolectric or Android Bitmap unit tests for BlurDetector and FramingAnalyzer
}
