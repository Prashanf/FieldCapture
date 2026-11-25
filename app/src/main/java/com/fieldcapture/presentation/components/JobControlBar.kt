package com.fieldcapture.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun JobControlBar(
    currentJobId: String?,
    captureCount: Int,
    onStartJob: (technicianName: String, jobAddress: String?) -> Unit,
    onCompleteJob: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var techName by remember { mutableStateOf("Field Technician") }
    var jobAddress by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentJobId == null) {
                Text(
                    text = "No active job",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(onClick = { showStartDialog = true }) {
                    Text("Start Job")
                }
            } else {
                Text(
                    text = "Recording... $captureCount captures",
                    style = MaterialTheme.typography.labelLarge
                )
                OutlinedButton(onClick = { showCompleteDialog = true }) {
                    Text("Complete Job")
                }
            }
        }
    }

    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Start New Job") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = techName,
                        onValueChange = { techName = it },
                        label = { Text("Technician Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = jobAddress,
                        onValueChange = { jobAddress = it },
                        label = { Text("Job Address (Optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStartDialog = false
                        onStartJob(
                            techName.ifBlank { "Field Technician" },
                            jobAddress.takeIf { it.isNotBlank() }
                        )
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text("Complete Job?") },
            text = {
                Text("Build the Job Record and export JSON for $captureCount captures?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteDialog = false
                        onCompleteJob()
                    }
                ) {
                    Text("Complete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
