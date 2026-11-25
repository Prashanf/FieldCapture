package com.fieldcapture.domain.model

data class QualityResult(
    val isAcceptable: Boolean,
    val blurScore: Double,
    val brightnessScore: Int,
    val edgeDensity: Double,
    val isBlurry: Boolean,
    val isDark: Boolean,
    val isOverexposed: Boolean,
    val isBadlyFramed: Boolean,
    val rejectionReason: String?
)
