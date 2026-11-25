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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fieldcapture.glasses.GlassesState
import com.fieldcapture.presentation.components.ConnectionStatusBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    onNavigateToReview: (String) -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val glassesState by viewModel.glassesState.collectAsState()
    val streamState by viewModel.streamState.collectAsState()
    val latestFrame by viewModel.latestFrame.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val captures by viewModel.captures.collectAsState()

    val isSessionActive = glassesState is GlassesState.Connected ||
        glassesState is GlassesState.Connecting ||
        glassesState is GlassesState.Paused

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
                // Live camera preview from Mock Device Kit / Meta DAT stream
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val frame = latestFrame
                    if (frame != null) {
                        Image(
                            bitmap = frame.asImageBitmap(),
                            contentDescription = "Glasses camera preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "Tap Start Session to preview mock glasses feed",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Connection + stream status summary
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Session: ${glassesState::class.simpleName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Stream: ${streamState?.name ?: "IDLE"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    val errorText = uiState.errorMessage ?: (glassesState as? GlassesState.Error)?.message
                    if (!errorText.isNullOrBlank()) {
                        Text(
                            text = "Error: $errorText",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Start / Stop session controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isSessionActive) {
                                viewModel.stopSession()
                            } else {
                                viewModel.initializeSession(useMock = true)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isSessionActive) "Stop Session" else "Start Session")
                    }

                    OutlinedButton(
                        onClick = { onNavigateToReview(uiState.jobId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Review (${captures.size})")
                    }
                }
            }
        }
    }
}
