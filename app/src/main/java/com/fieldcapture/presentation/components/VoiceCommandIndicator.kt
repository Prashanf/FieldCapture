package com.fieldcapture.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fieldcapture.domain.model.VoiceCommand
import com.fieldcapture.voice.VoiceCommandService.ListeningState

@Composable
fun VoiceCommandIndicator(
    listeningState: ListeningState,
    lastRecognizedText: String?,
    lastCommand: VoiceCommand?,
    modifier: Modifier = Modifier
) {
    val parsedSummary = when (lastCommand) {
        is VoiceCommand.CaptureNameplate -> "CaptureNameplate"
        is VoiceCommand.CaptureDamage -> "CaptureDamage (${lastCommand.room}, ${lastCommand.surface})"
        is VoiceCommand.Note -> "Note"
        is VoiceCommand.Unknown -> "Unknown"
        null -> null
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Voice: ${listeningState.name}",
                    style = MaterialTheme.typography.labelLarge
                )
                if (parsedSummary != null) {
                    Text(
                        text = parsedSummary,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = lastRecognizedText?.let { "Last command: \"$it\"" }
                    ?: "Say: \"capture nameplate\", \"capture damage <room> <surface>\", or \"note ...\"",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
