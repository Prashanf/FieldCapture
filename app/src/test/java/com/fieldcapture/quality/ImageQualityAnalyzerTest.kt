package com.fieldcapture.quality

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.fieldcapture.domain.model.CaptureType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ImageQualityAnalyzerTest {

    private val blurDetector = BlurDetector()
    private val brightnessAnalyzer = BrightnessAnalyzer()
    private val framingAnalyzer = FramingAnalyzer()
    private val imageQualityAnalyzer = ImageQualityAnalyzer(
        blurDetector = blurDetector,
        brightnessAnalyzer = brightnessAnalyzer,
        framingAnalyzer = framingAnalyzer
    )

    @Test
    fun `quality thresholds match documented values`() {
        assertEquals(80.0, QualityThresholds.BLUR_THRESHOLD_GENERAL, 0.001)
        assertEquals(100.0, QualityThresholds.BLUR_THRESHOLD_NAMEPLATE, 0.001)
        assertEquals(30, QualityThresholds.BRIGHTNESS_MIN)
        assertEquals(240, QualityThresholds.BRIGHTNESS_MAX)
        assertEquals(2.0, QualityThresholds.FRAMING_EDGE_DENSITY_MIN, 0.001)
        assertEquals(100.0, QualityThresholds.blurThresholdFor("NAMEPLATE"), 0.001)
        assertEquals(80.0, QualityThresholds.blurThresholdFor("DAMAGE"), 0.001)
    }

    @Test
    fun `sharp image has high laplacian variance`() {
        val bitmap = createCheckerboardBitmap(100, 100, blockSize = 5)
        val result = blurDetector.analyze(bitmap, threshold = QualityThresholds.BLUR_THRESHOLD_GENERAL)
        assertFalse(result.isBlurry)
        assertTrue(result.laplacianVariance > QualityThresholds.BLUR_THRESHOLD_GENERAL)
    }

    @Test
    fun `uniform image has low laplacian variance`() {
        val bitmap = createSolidBitmap(100, 100, Color.GRAY)
        val result = blurDetector.analyze(bitmap, threshold = QualityThresholds.BLUR_THRESHOLD_GENERAL)
        assertTrue(result.isBlurry)
        assertTrue(result.laplacianVariance < 10.0)
    }

    @Test
    fun `dark image is flagged`() {
        val bitmap = createSolidBitmap(100, 100, Color.rgb(20, 20, 20))
        val result = brightnessAnalyzer.analyze(bitmap)
        assertTrue(result.isDark)
        assertFalse(result.isOverexposed)
        assertFalse(result.isAcceptable)
    }

    @Test
    fun `bright image is flagged as overexposed`() {
        val bitmap = createSolidBitmap(100, 100, Color.rgb(250, 250, 250))
        val result = brightnessAnalyzer.analyze(bitmap)
        assertTrue(result.isOverexposed)
        assertFalse(result.isDark)
        assertFalse(result.isAcceptable)
    }

    @Test
    fun `normal brightness is acceptable`() {
        val bitmap = createSolidBitmap(100, 100, Color.rgb(128, 128, 128))
        val result = brightnessAnalyzer.analyze(bitmap)
        assertFalse(result.isDark)
        assertFalse(result.isOverexposed)
        assertTrue(result.isAcceptable)
    }

    @Test
    fun `blank image has low edge density`() {
        val bitmap = createSolidBitmap(100, 100, Color.WHITE)
        val result = framingAnalyzer.analyze(bitmap)
        assertTrue(result.isBadlyFramed)
        assertFalse(result.hasContent)
        assertTrue(result.centerEdgeDensity < QualityThresholds.FRAMING_EDGE_DENSITY_MIN)
    }

    @Test
    fun `patterned image has high edge density`() {
        val bitmap = createCheckerboardBitmap(100, 100, blockSize = 5)
        val result = framingAnalyzer.analyze(bitmap)
        assertFalse(result.isBadlyFramed)
        assertTrue(result.hasContent)
        assertTrue(result.centerEdgeDensity > QualityThresholds.FRAMING_EDGE_DENSITY_MIN)
    }

    @Test
    fun `combined analyzer accepts sharp well lit patterned image`() {
        val bitmap = createCheckerboardBitmap(100, 100, blockSize = 5)
        val result = imageQualityAnalyzer.analyze(bitmap, CaptureType.NAMEPLATE)
        assertTrue(result.isAcceptable)
        assertNull(result.rejectionReason)
    }

    @Test
    fun `combined analyzer reports rejection reason for dark uniform frame`() {
        val bitmap = createSolidBitmap(100, 100, Color.rgb(15, 15, 15))
        val result = imageQualityAnalyzer.analyze(bitmap, CaptureType.DAMAGE)
        assertFalse(result.isAcceptable)
        assertTrue(result.isBlurry)
        assertTrue(result.isDark)
        assertTrue(result.isBadlyFramed)
        assertEquals("too blurry and too dark and badly framed", result.rejectionReason)
    }
}

private fun createSolidBitmap(width: Int, height: Int, color: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height) { color }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

private fun createCheckerboardBitmap(width: Int, height: Int, blockSize: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val isBlack = ((x / blockSize) + (y / blockSize)) % 2 == 0
            bitmap.setPixel(x, y, if (isBlack) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}
