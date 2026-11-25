package com.fieldcapture.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CaptureCard(
    capture: Capture,
    modifier: Modifier = Modifier
) {
    val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(capture.timestamp))
    val title = when (capture.type) {
        CaptureType.NAMEPLATE -> "Nameplate - $timeString"
        CaptureType.DAMAGE -> {
            val room = capture.metadata.room ?: "unknown"
            val surface = capture.metadata.surface ?: "unknown"
            "Damage ($room • $surface) - $timeString"
        }
        CaptureType.NOTE -> "Note - $timeString"
    }
    val subtitle = when (capture.type) {
        CaptureType.NOTE -> capture.metadata.noteText ?: ""
        else -> capture.metadata.voiceCommandRaw
            ?: capture.imagePath?.substringAfterLast("/")
            ?: "Captured frame"
    }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (capture.type == CaptureType.NOTE) {
                            Icons.AutoMirrored.Filled.NoteAdd
                        } else {
                            Icons.Default.PhotoCamera
                        },
                        contentDescription = capture.type.name,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = capture.status.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (capture.qualityResult != null) {
                Spacer(modifier = Modifier.height(6.dp))
                QualityIndicator(quality = capture.qualityResult)
            }
        }
    }
}
