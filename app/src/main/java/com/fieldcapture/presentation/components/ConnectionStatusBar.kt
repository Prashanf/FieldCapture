package com.fieldcapture.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fieldcapture.glasses.GlassesState

@Composable
fun ConnectionStatusBar(
    glassesState: GlassesState,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val glassesLabel = when (glassesState) {
        is GlassesState.Disconnected -> "Glasses: Disconnected"
        is GlassesState.Connecting -> "Glasses: Connecting..."
        is GlassesState.Connected -> "Glasses: ${glassesState.deviceName}"
        is GlassesState.Paused -> "Glasses: Paused"
        is GlassesState.Error -> "Glasses Error: ${glassesState.message}"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = glassesLabel, style = MaterialTheme.typography.labelMedium)
            Text(
                text = if (isOnline) "Network: Online" else "Network: Offline (Queued)",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
