package com.fieldcapture.presentation.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fieldcapture.domain.model.EquipmentEntry
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.model.LineItemEntry
import java.util.Locale

@Composable
fun JobRecordSummaryCard(
    jobRecord: JobRecord,
    exportedFilePath: String?,
    onExportJson: () -> Unit,
    onShareJson: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Job Record: ${jobRecord.jobId}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tech: ${jobRecord.technicianName} • Completed: ${jobRecord.completedAt ?: "In Progress"}",
                style = MaterialTheme.typography.bodySmall
            )
            jobRecord.jobAddress?.takeIf { it.isNotBlank() }?.let { addr ->
                Text(text = "Address: $addr", style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider()

            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            val s = jobRecord.summary
            Text(
                text = "Captures: ${s.totalCaptures} (${s.nameplateCaptures} nameplate, ${s.damageCaptures} damage, ${s.noteCaptures} note)",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Line Items: ${s.totalLineItems} • Subtotal: ${String.format(Locale.US, "$%,.2f", s.estimateSubtotal)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Avg Confidence: ${String.format(Locale.US, "%.2f", s.averageExtractionConfidence)}",
                style = MaterialTheme.typography.bodySmall
            )

            EquipmentListSection(equipment = jobRecord.equipment)

            LineItemsSection(lineItems = jobRecord.lineItems)

            ExportButton(
                onExportJson = onExportJson,
                onShareJson = { onShareJson(context) }
            )

            exportedFilePath?.let { path ->
                Text(
                    text = "Saved: $path",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EquipmentListSection(
    equipment: List<EquipmentEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Equipment (${equipment.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (equipment.isEmpty()) {
            Text(text = "None recorded", style = MaterialTheme.typography.bodySmall)
        } else {
            equipment.forEach { eq ->
                Text(
                    text = "• ${eq.manufacturer ?: "Unknown"} ${eq.equipmentType ?: "Equipment"} — ${eq.modelNumber ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun LineItemsSection(
    lineItems: List<LineItemEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Line Items (${lineItems.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        LineItemList(lineItems = lineItems)
    }
}

@Composable
fun ExportButton(
    onExportJson: () -> Unit,
    onShareJson: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onExportJson,
            modifier = Modifier.weight(1f)
        ) {
            Text("Export JSON")
        }
        OutlinedButton(
            onClick = onShareJson,
            modifier = Modifier.weight(1f)
        ) {
            Text("Share")
        }
    }
}

fun shareJobRecordJson(context: Context, jobId: String, jsonContent: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "FieldCapture Job Record $jobId")
        putExtra(Intent.EXTRA_TEXT, jsonContent)
    }
    context.startActivity(Intent.createChooser(intent, "Share Job Record JSON"))
}
