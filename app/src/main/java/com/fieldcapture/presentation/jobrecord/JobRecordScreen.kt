package com.fieldcapture.presentation.jobrecord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fieldcapture.presentation.components.JobRecordSummaryCard
import com.fieldcapture.presentation.components.shareJobRecordJson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobRecordScreen(
    viewModel: JobRecordViewModel = hiltViewModel()
) {
    val jobRecord by viewModel.jobRecord.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Job Record & Estimate") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val record = jobRecord
            if (record == null) {
                Text(
                    text = "Building Job Record...",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                JobRecordSummaryCard(
                    jobRecord = record,
                    exportedFilePath = exportStatus,
                    onExportJson = { viewModel.exportJson() },
                    onShareJson = { ctx ->
                        viewModel.currentRecordJson()?.let { json ->
                            shareJobRecordJson(ctx, record.jobId, json)
                        }
                    }
                )
            }
        }
    }
}
