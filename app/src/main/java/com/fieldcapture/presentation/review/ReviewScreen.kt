package com.fieldcapture.presentation.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fieldcapture.presentation.components.CaptureCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onNavigateToJobRecord: (String) -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val captures by viewModel.captures.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Review Captures") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(captures, key = { it.id }) { capture ->
                    CaptureCard(capture = capture)
                }
            }

            Button(
                onClick = { onNavigateToJobRecord(viewModel.jobId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Job Record")
            }
        }
    }
}
