package com.fieldcapture.quality

import android.graphics.Bitmap
import android.graphics.Color
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyzes image brightness using mean pixel luminance and dark/bright pixel ratios.
 */
@Singleton
class BrightnessAnalyzer @Inject constructor() {

    data class BrightnessResult(
        val meanBrightness: Int,
        val isDark: Boolean,
        val isOverexposed: Boolean,
        val isAcceptable: Boolean,
        val darkPixelPercentage: Double,
        val brightPixelPercentage: Double
    )

    fun analyze(bitmap: Bitmap): BrightnessResult {
        val scaled = scaleBitmap(bitmap, maxDimension = 200)
        val width = scaled.width
        val height = scaled.height
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        if (pixels.isEmpty()) {
            if (scaled != bitmap) scaled.recycle()
            return BrightnessResult(
                meanBrightness = 0,
                isDark = true,
                isOverexposed = false,
                isAcceptable = false,
                darkPixelPercentage = 100.0,
                brightPixelPercentage = 0.0
            )
        }

        var totalLuminance = 0L
        var darkPixels = 0
        var brightPixels = 0

        for (pixel in pixels) {
            val luminance = (0.299 * Color.red(pixel) +
                0.587 * Color.green(pixel) +
                0.114 * Color.blue(pixel)).toInt()
            totalLuminance += luminance
            if (luminance < QualityThresholds.BRIGHTNESS_MIN) darkPixels++
            if (luminance > QualityThresholds.BRIGHTNESS_MAX) brightPixels++
        }

        val meanBrightness = (totalLuminance / pixels.size).toInt()
        val darkPercentage = (darkPixels.toDouble() / pixels.size) * 100.0
        val brightPercentage = (brightPixels.toDouble() / pixels.size) * 100.0

        val isDark = meanBrightness < QualityThresholds.BRIGHTNESS_MIN
        val isOverexposed = meanBrightness > QualityThresholds.BRIGHTNESS_MAX

        if (scaled != bitmap) scaled.recycle()

        return BrightnessResult(
            meanBrightness = meanBrightness,
            isDark = isDark,
            isOverexposed = isOverexposed,
            isAcceptable = !isDark && !isOverexposed,
            darkPixelPercentage = darkPercentage,
            brightPixelPercentage = brightPercentage
        )
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxSide
        val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
