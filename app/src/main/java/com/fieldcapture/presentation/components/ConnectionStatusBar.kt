package com.fieldcapture.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fieldcapture.glasses.GlassesState

@Composable
fun ConnectionStatusBar(
    glassesState: GlassesState,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val (statusColor, glassesLabel) = when (glassesState) {
        is GlassesState.Disconnected -> Color(0xFF9E9E9E) to "Glasses: Disconnected"
        is GlassesState.Connecting -> Color(0xFFFFA000) to "Glasses: Connecting..."
        is GlassesState.Connected -> Color(0xFF2E7D32) to "Glasses: Connected"
        is GlassesState.Paused -> Color(0xFF1976D2) to "Glasses: Paused"
        is GlassesState.Error -> Color(0xFFC62828) to "Error: ${glassesState.message}"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = glassesLabel, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                text = if (isOnline) "Online" else "Offline",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
