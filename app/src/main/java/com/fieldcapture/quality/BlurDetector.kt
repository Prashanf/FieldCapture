package com.fieldcapture.quality

import android.graphics.Bitmap
import android.graphics.Color
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Detects image blur using the variance of the Laplacian operator.
 *
 * Algorithm:
 * 1. Convert image to grayscale
 * 2. Apply 3x3 Laplacian kernel to detect edges
 * 3. Compute variance of the Laplacian response
 * 4. Low variance = blurry (edges are smeared), high variance = sharp
 *
 * Pure Android implementation — no OpenCV dependency needed.
 */
@Singleton
class BlurDetector @Inject constructor() {

    private val laplacianKernel = arrayOf(
        intArrayOf(0, 1, 0),
        intArrayOf(1, -4, 1),
        intArrayOf(0, 1, 0)
    )

    data class BlurResult(
        val laplacianVariance: Double,
        val isBlurry: Boolean,
        val threshold: Double
    )

    fun analyze(
        bitmap: Bitmap,
        threshold: Double = QualityThresholds.BLUR_THRESHOLD_GENERAL
    ): BlurResult {
        val scaled = scaleBitmap(bitmap, maxDimension = 300)
        val width = scaled.width
        val height = scaled.height

        if (width < 3 || height < 3) {
            if (scaled != bitmap) scaled.recycle()
            return BlurResult(laplacianVariance = 0.0, isBlurry = true, threshold = threshold)
        }

        val gray = toGrayscale(scaled)
        val laplacian = applyLaplacian(gray, width, height)

        val mean = laplacian.average()
        val variance = laplacian.map { (it - mean).pow(2) }.average()

        if (scaled != bitmap) scaled.recycle()

        return BlurResult(
            laplacianVariance = variance,
            isBlurry = variance < threshold,
            threshold = threshold
        )
    }

    private fun toGrayscale(bitmap: Bitmap): DoubleArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        return DoubleArray(pixels.size) { i ->
            val pixel = pixels[i]
            0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
        }
    }

    private fun applyLaplacian(gray: DoubleArray, width: Int, height: Int): DoubleArray {
        val result = DoubleArray((width - 2) * (height - 2))
        var index = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sum = 0.0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        sum += gray[(y + ky) * width + (x + kx)] * laplacianKernel[ky + 1][kx + 1]
                    }
                }
                result[index++] = sum
            }
        }

        return result
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
