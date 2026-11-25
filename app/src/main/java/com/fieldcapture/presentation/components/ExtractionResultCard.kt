package com.fieldcapture.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fieldcapture.extraction.DamageExtraction
import com.fieldcapture.extraction.ExtractionResult
import com.fieldcapture.extraction.NameplateExtraction
import com.fieldcapture.extraction.NoteExtraction
import java.util.Locale

@Composable
fun ExtractionResultCard(
    result: ExtractionResult,
    modifier: Modifier = Modifier
) {
    when (result) {
        is NameplateExtraction -> NameplateResultCard(result = result, modifier = modifier)
        is DamageExtraction -> DamageResultCard(result = result, modifier = modifier)
        is NoteExtraction -> NoteResultCard(result = result, modifier = modifier)
    }
}

@Composable
fun NameplateResultCard(
    result: NameplateExtraction,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Nameplate Extraction",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (!result.error.isNullOrBlank()) {
                Text(
                    text = "Notice: ${result.error} — ${result.description ?: result.notes.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828)
                )
            } else {
                ConfidenceFieldRow("Equipment", result.equipmentType, result.confidence.equipmentType)
                ConfidenceFieldRow("Manufacturer", result.manufacturer, result.confidence.manufacturer)
                ConfidenceFieldRow("Model", result.modelNumber, result.confidence.modelNumber)
                ConfidenceFieldRow("Serial", result.serialNumber, result.confidence.serialNumber)
                ConfidenceFieldRow("Year / Date", result.manufactureDate, result.confidence.manufactureDate)
                ConfidenceFieldRow("Capacity", result.capacity, result.confidence.capacity)
                result.voltage?.let {
                    Text(text = "Voltage: $it", style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            ConfidenceSummaryRow("Overall Confidence", result.overallConfidence)
        }
    }
}

@Composable
fun DamageResultCard(
    result: DamageExtraction,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Damage Assessment",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Severity: ${result.severity.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = severityColor(result.severity)
                )
            }

            if (!result.error.isNullOrBlank()) {
                Text(
                    text = "Notice: ${result.error} — ${result.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828)
                )
            } else {
                ConfidenceFieldRow("Type", result.damageType, result.confidence.damageType)
                if (result.description.isNotBlank()) {
                    Text(text = result.description, style = MaterialTheme.typography.bodySmall)
                }
                if (result.affectedMaterials.isNotEmpty()) {
                    Text(
                        text = "Materials: ${result.affectedMaterials.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                result.affectedAreaEstimate?.let {
                    Text(text = "Area: $it", style = MaterialTheme.typography.bodySmall)
                }
                if (result.recommendedLineItems.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Text(
                        text = "Recommended Line Items:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    result.recommendedLineItems.forEach { item ->
                        Text(
                            text = "• ${item.code} (${item.priority})${item.estimatedQuantity?.let { " [$it]" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (item.reason.isNotBlank()) {
                            Text(
                                text = "  ${item.reason}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                result.safetyConcerns?.takeIf { it.isNotBlank() }?.let { concern ->
                    Text(
                        text = "Safety: $concern",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            ConfidenceSummaryRow("Overall Confidence", result.overallConfidence)
        }
    }
}

@Composable
fun NoteResultCard(
    result: NoteExtraction,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Structured Note",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = result.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (result.transcribedNote.isNotBlank()) {
                Text(text = result.transcribedNote, style = MaterialTheme.typography.bodySmall)
            }
            result.imageContext?.takeIf { it.isNotBlank() }?.let { ctx ->
                Text(text = "Context: $ctx", style = MaterialTheme.typography.bodySmall)
            }
            if (result.keyFacts.isNotEmpty()) {
                Text(
                    text = "Facts: ${result.keyFacts.joinToString("; ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (result.followUpNeeded) {
                Text(
                    text = "Follow-up: ${result.followUpReason ?: "Recommended"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF57C00)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            ConfidenceSummaryRow("Overall Confidence", result.overallConfidence)
        }
    }
}

@Composable
private fun ConfidenceFieldRow(
    label: String,
    value: String?,
    confidence: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label: ${value ?: "—"}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "conf: ${formatConfidence(confidence)}",
            style = MaterialTheme.typography.labelSmall,
            color = confidenceColor(confidence)
        )
    }
}

@Composable
private fun ConfidenceSummaryRow(
    label: String,
    confidence: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(
            text = formatConfidence(confidence),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = confidenceColor(confidence)
        )
    }
}

private fun confidenceColor(confidence: Double): Color {
    return when {
        confidence >= 0.7 -> Color(0xFF2E7D32)
        confidence >= 0.4 -> Color(0xFFF57C00)
        else -> Color(0xFFC62828)
    }
}

private fun severityColor(severity: String): Color {
    return when (severity.lowercase()) {
        "critical", "high" -> Color(0xFFC62828)
        "medium" -> Color(0xFFF57C00)
        else -> Color(0xFF2E7D32)
    }
}

private fun formatConfidence(confidence: Double): String =
    String.format(Locale.US, "%.2f", confidence)
