package com.fieldcapture.quality

/**
 * Image quality thresholds for field capture scenarios.
 *
 * RATIONALE FOR EACH THRESHOLD:
 *
 * These thresholds were initially derived from published computer vision literature
 * on image quality assessment, then tuned on a test set of 40+ real captures
 * (see eval/eval_report.md for the tuning results).
 *
 * The environment is challenging: techs move while capturing, work in dark spaces
 * (under sinks, in attics, crawl spaces), and use glasses with a fixed wide-angle lens.
 * Thresholds are intentionally lenient — we'd rather send a marginal image to
 * the AI (which can report low confidence) than annoy a tech with constant retake
 * requests. A tech who switches off the quality check is worse than a slightly
 * noisy extraction.
 */
object QualityThresholds {

    /**
     * BLUR — Laplacian Variance
     *
     * The Laplacian operator highlights edges. Sharp images have high edge variance,
     * blurry images have low variance.
     *
     * - Very sharp photo: variance ~500-1000+
     * - Acceptable handheld: variance ~100-300
     * - Motion blur / out of focus: variance ~20-80
     * - Severe blur: variance <20
     *
     * Threshold set at 80.0 — this catches significant motion blur and out-of-focus
     * shots while allowing the slight softness typical of a head-mounted camera on
     * a moving person. Lower than typical phone camera thresholds (~100) because
     * glasses cameras have lower resolution and more inherent softness.
     *
     * Nameplate captures use a stricter threshold (100.0) because OCR accuracy
     * drops sharply with blur — a slightly blurry damage photo is still usable,
     * but a blurry serial number is not.
     */
    const val BLUR_THRESHOLD_GENERAL = 80.0
    const val BLUR_THRESHOLD_NAMEPLATE = 100.0

    /**
     * BRIGHTNESS — Mean Luminance (0-255 scale)
     *
     * Average pixel intensity of the grayscale image.
     *
     * - Bright daylight: ~160-200
     * - Well-lit indoor: ~100-160
     * - Dim room: ~50-100
     * - Dark cupboard/crawl space: ~15-50
     * - Near black: <15
     *
     * MIN set at 30 — allows dim rooms but rejects near-dark captures where
     * nothing is distinguishable. Techs often work under sinks, in attics,
     * and in closets. Setting this too high (e.g., 60) would trigger constant
     * retakes in legitimate work environments.
     *
     * MAX set at 240 — rejects overexposed/washed out images where text
     * and details are lost to glare. Common when a work light reflects off
     * a glossy nameplate.
     */
    const val BRIGHTNESS_MIN = 30
    const val BRIGHTNESS_MAX = 240

    /**
     * FRAMING — Edge Density in Center Region
     *
     * Percentage of pixels in the center 60% of the frame that are classified
     * as edges (using Sobel operator with threshold). An image with meaningful
     * content (text, damage patterns, objects) has higher edge density than
     * a blank wall, floor, or sky.
     *
     * - Nameplate with text: edge density ~8-25%
     * - Damage (cracks, stains): edge density ~5-15%
     * - Textured surface: edge density ~3-8%
     * - Blank wall / sky: edge density ~0.5-2%
     *
     * Threshold set at 2.0% — catches clearly empty frames (pointing at nothing)
     * while allowing low-contrast damage that may have subtle edges.
     * This is the least reliable check, so it's lenient.
     */
    const val FRAMING_EDGE_DENSITY_MIN = 2.0

    /**
     * Returns the blur threshold for a specific capture type.
     * Nameplates need sharper images for OCR.
     */
    fun blurThresholdFor(captureType: String): Double {
        return when (captureType.uppercase()) {
            "NAMEPLATE" -> BLUR_THRESHOLD_NAMEPLATE
            else -> BLUR_THRESHOLD_GENERAL
        }
    }
}
