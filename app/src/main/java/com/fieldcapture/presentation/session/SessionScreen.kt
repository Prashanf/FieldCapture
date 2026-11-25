package com.fieldcapture.presentation.session

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.presentation.components.CaptureCard
import com.fieldcapture.presentation.components.ConnectionStatusBar
import com.fieldcapture.presentation.components.VoiceCommandIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    onNavigateToReview: (String) -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val glassesState by viewModel.glassesState.collectAsState()
    val latestFrame by viewModel.latestFrame.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val captures by viewModel.captures.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FieldCapture Session") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ConnectionStatusBar(glassesState = glassesState, isOnline = isOnline)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val frame = latestFrame
                    if (frame != null) {
                        Image(
                            bitmap = frame.asImageBitmap(),
                            contentDescription = "Mock glasses camera stream",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "Mock Glasses Camera Preview",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                VoiceCommandIndicator(
                    isListening = isListening,
                    lastCommandText = uiState.lastCommand
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.connectMockGlasses() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Connect Mock")
                    }
                    OutlinedButton(
                        onClick = { viewModel.toggleVoiceListening() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isListening) "Stop Mic" else "Start Mic")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.triggerManualCapture(CaptureType.NAMEPLATE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Nameplate")
                    }
                    OutlinedButton(
                        onClick = { viewModel.triggerManualCapture(CaptureType.DAMAGE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Damage")
                    }
                    Button(
                        onClick = { onNavigateToReview(uiState.jobId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Review (${captures.size})")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(captures, key = { it.id }) { capture ->
                        CaptureCard(capture = capture)
                    }
                }
            }
        }
    }
}
