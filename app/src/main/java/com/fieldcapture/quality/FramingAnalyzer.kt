package com.fieldcapture.quality

import android.graphics.Bitmap
import android.graphics.Color
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Analyzes whether the frame contains meaningful content by checking Sobel edge density
 * in the center 60% region of the frame.
 */
@Singleton
class FramingAnalyzer @Inject constructor() {

    data class FramingResult(
        val centerEdgeDensity: Double,
        val isBadlyFramed: Boolean,
        val hasContent: Boolean
    )

    fun analyze(bitmap: Bitmap): FramingResult {
        val scaled = scaleBitmap(bitmap, maxDimension = 200)
        val width = scaled.width
        val height = scaled.height

        if (width < 5 || height < 5) {
            if (scaled != bitmap) scaled.recycle()
            return FramingResult(centerEdgeDensity = 0.0, isBadlyFramed = true, hasContent = false)
        }

        val gray = toGrayscale(scaled)

        // Focus on center 60% of the frame
        val marginX = (width * 0.2).toInt()
        val marginY = (height * 0.2).toInt()

        var edgePixels = 0
        var totalPixels = 0
        val sobelThreshold = 30.0

        for (y in (marginY + 1) until (height - marginY - 1)) {
            for (x in (marginX + 1) until (width - marginX - 1)) {
                val gx = -gray[(y - 1) * width + (x - 1)] +
                    gray[(y - 1) * width + (x + 1)] +
                    -2.0 * gray[y * width + (x - 1)] +
                    2.0 * gray[y * width + (x + 1)] +
                    -gray[(y + 1) * width + (x - 1)] +
                    gray[(y + 1) * width + (x + 1)]

                val gy = -gray[(y - 1) * width + (x - 1)] +
                    -2.0 * gray[(y - 1) * width + x] +
                    -gray[(y - 1) * width + (x + 1)] +
                    gray[(y + 1) * width + (x - 1)] +
                    2.0 * gray[(y + 1) * width + x] +
                    gray[(y + 1) * width + (x + 1)]

                val magnitude = sqrt(gx * gx + gy * gy)
                if (magnitude > sobelThreshold) edgePixels++
                totalPixels++
            }
        }

        val edgeDensity = if (totalPixels > 0) {
            (edgePixels.toDouble() / totalPixels) * 100.0
        } else {
            0.0
        }

        val isBadlyFramed = edgeDensity < QualityThresholds.FRAMING_EDGE_DENSITY_MIN

        if (scaled != bitmap) scaled.recycle()

        return FramingResult(
            centerEdgeDensity = edgeDensity,
            isBadlyFramed = isBadlyFramed,
            hasContent = !isBadlyFramed
        )
    }

    private fun toGrayscale(bitmap: Bitmap): DoubleArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return DoubleArray(pixels.size) { i ->
            val p = pixels[i]
            0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p)
        }
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
