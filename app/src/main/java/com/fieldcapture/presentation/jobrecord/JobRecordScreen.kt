package com.fieldcapture.presentation.jobrecord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fieldcapture.presentation.components.LineItemList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobRecordScreen(
    viewModel: JobRecordViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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
            Text(
                text = "Job ID: ${viewModel.jobId}",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Draft Line Items",
                style = MaterialTheme.typography.titleSmall
            )
            LineItemList(lineItems = jobRecord?.lineItems ?: emptyList())

            Button(
                onClick = { viewModel.exportJson(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Job Record JSON")
            }

            exportStatus?.let { status ->
                Text(text = status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
