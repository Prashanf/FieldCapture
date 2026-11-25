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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
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
import com.fieldcapture.voice.VoiceCommandService.ListeningState

@Composable
fun ConnectionStatusBar(
    glassesState: GlassesState,
    listeningState: ListeningState,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val (statusColor, glassesLabel) = when (glassesState) {
        is GlassesState.Disconnected -> Color(0xFF9E9E9E) to "Disconnected"
        is GlassesState.Connecting -> Color(0xFFFFC107) to "Connecting..."
        is GlassesState.Connected -> Color(0xFF2E7D32) to "Connected"
        is GlassesState.Paused -> Color(0xFFFFC107) to "Paused"
        is GlassesState.Error -> Color(0xFFC62828) to "Error: ${glassesState.message}"
    }

    val isMicActive = listeningState == ListeningState.LISTENING || listeningState == ListeningState.PROCESSING
    val micLabel = when (listeningState) {
        ListeningState.IDLE -> "Idle"
        ListeningState.LISTENING -> "Listening"
        ListeningState.PROCESSING -> "Processing"
        ListeningState.ERROR -> "Mic Error"
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = glassesLabel,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isMicActive) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = micLabel,
                    tint = if (isMicActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = micLabel, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
