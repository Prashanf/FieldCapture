package com.fieldcapture.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fieldcapture.domain.model.QualityResult

@Composable
fun QualityIndicator(
    quality: QualityResult?,
    modifier: Modifier = Modifier
) {
    if (quality == null) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val statusText = if (quality.isAcceptable) "Quality OK" else "Retake Recommended"
        AssistChip(
            onClick = {},
            label = {
                Text(
                    text = "$statusText (blur=${quality.blurScore.toInt()}, lum=${quality.brightness})",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        )
    }
}
