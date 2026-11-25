package com.fieldcapture.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fieldcapture.domain.model.QualityResult
import kotlin.math.roundToInt

@Composable
fun QualityIndicator(
    quality: QualityResult?,
    modifier: Modifier = Modifier
) {
    if (quality == null) return

    val okColor = Color(0xFF2E7D32)
    val failColor = Color(0xFFC62828)

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (quality.isAcceptable) {
                    "Quality: Acceptable"
                } else {
                    "Quality: ${quality.rejectionReason ?: "Rejected"}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (quality.isAcceptable) okColor else failColor
            )

            QualityMetricRow(
                label = "Sharpness",
                progress = (quality.blurScore / 500.0).toFloat().coerceIn(0f, 1f),
                scoreText = quality.blurScore.roundToInt().toString(),
                passed = !quality.isBlurry,
                okColor = okColor,
                failColor = failColor
            )

            QualityMetricRow(
                label = "Brightness",
                progress = (quality.brightnessScore / 255f).coerceIn(0f, 1f),
                scoreText = quality.brightnessScore.toString(),
                passed = !quality.isDark && !quality.isOverexposed,
                okColor = okColor,
                failColor = failColor
            )

            QualityMetricRow(
                label = "Content",
                progress = (quality.edgeDensity / 25.0).toFloat().coerceIn(0f, 1f),
                scoreText = "${quality.edgeDensity.roundToInt()}%",
                passed = !quality.isBadlyFramed,
                okColor = okColor,
                failColor = failColor
            )
        }
    }
}

@Composable
private fun QualityMetricRow(
    label: String,
    progress: Float,
    scoreText: String,
    passed: Boolean,
    okColor: Color,
    failColor: Color
) {
    val barColor = if (passed) okColor else failColor
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(68.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            color = barColor,
            trackColor = barColor.copy(alpha = 0.2f),
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
        )
        Text(
            text = scoreText,
            style = MaterialTheme.typography.labelSmall,
            color = barColor,
            modifier = Modifier.width(36.dp)
        )
    }
}
