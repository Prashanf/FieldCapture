package com.fieldcapture.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fieldcapture.domain.model.LineItemEntry
import java.util.Locale

@Composable
fun LineItemList(
    lineItems: List<LineItemEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (lineItems.isEmpty()) {
            Text(
                text = "No draft line items generated yet.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            lineItems.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.itemNumber}. ${item.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = String.format(Locale.US, "$%.2f", item.total),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "${item.quantity} ${item.unit} × ${String.format(Locale.US, "$%.2f", item.unitPrice)}/${item.unit} • ${item.priority}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (item.reason.isNotBlank()) {
                            Text(
                                text = item.reason,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "Code: ${item.code}${if (!item.isValidCode) " (unrecognized)" else ""} • Source: ${item.sourceCaptureId.take(8)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
