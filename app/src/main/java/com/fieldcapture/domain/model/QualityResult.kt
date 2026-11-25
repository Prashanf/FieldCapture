package com.fieldcapture.domain.model

data class QualityResult(
    val blurScore: Double,
    val brightness: Int,
    val textRegionPercent: Double = 0.0,
    val isBlurry: Boolean,
    val isDark: Boolean,
    val isOverexposed: Boolean = false,
    val isBadlyFramed: Boolean
) {
    val isAcceptable: Boolean
        get() = !isBlurry && !isDark && !isOverexposed && !isBadlyFramed
}
