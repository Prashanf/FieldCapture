package com.fieldcapture.presentation.session

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.fieldcapture.glasses.GlassesState
import com.fieldcapture.presentation.components.CaptureCard
import com.fieldcapture.presentation.components.ConnectionStatusBar
import com.fieldcapture.presentation.components.JobControlBar
import com.fieldcapture.presentation.components.JobRecordSummaryCard
import com.fieldcapture.presentation.components.OfflineIndicator
import com.fieldcapture.presentation.components.VoiceCommandIndicator
import com.fieldcapture.presentation.components.shareJobRecordJson
import com.fieldcapture.voice.VoiceCommandService.ListeningState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    onNavigateToReview: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: SessionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val hasApiKey by viewModel.hasApiKey.collectAsState()
    val glassesState by viewModel.glassesState.collectAsState()
    val streamState by viewModel.streamState.collectAsState()
    val latestFrame by viewModel.latestFrame.collectAsState()
    val listeningState by viewModel.listeningState.collectAsState()
    val lastCommand by viewModel.lastCommand.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val queuedCount by viewModel.queuedCount.collectAsState()
    val captures by viewModel.captures.collectAsState()
    val currentJobId by viewModel.currentJobId.collectAsState()
    val jobRecord by viewModel.jobRecord.collectAsState()
    val exportedFilePath by viewModel.exportedFilePath.collectAsState()

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceListening()
        }
    }

    val isSessionActive = glassesState is GlassesState.Connected ||
        glassesState is GlassesState.Connecting ||
        glassesState is GlassesState.Paused

    val isVoiceActive = listeningState == ListeningState.LISTENING ||
        listeningState == ListeningState.PROCESSING

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FieldCapture Session") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "API Settings"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ConnectionStatusBar(
                glassesState = glassesState,
                listeningState = listeningState,
                isOnline = isOnline
            )

            if (!hasApiKey) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No Gemini API key configured",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = onNavigateToSettings) {
                            Text("Enter Key")
                        }
                    }
                }
            }

            OfflineIndicator(
                isOnline = isOnline,
                queuedCount = queuedCount,
                onRetrySync = { viewModel.retryQueuedExtractions() }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                JobControlBar(
                    currentJobId = currentJobId,
                    captureCount = captures.size,
                    onStartJob = { tech, address -> viewModel.startJob(tech, address) },
                    onCompleteJob = { viewModel.completeJob() }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
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

                VoiceCommandIndicator(
                    listeningState = listeningState,
                    lastRecognizedText = uiState.lastRecognizedText,
                    lastCommand = lastCommand
                )

                Text(
                    text = "Stream: ${streamState?.name ?: "IDLE"} • Captures: ${captures.size}",
                    style = MaterialTheme.typography.labelMedium
                )

                val errorText = uiState.errorMessage ?: (glassesState as? GlassesState.Error)?.message
                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = "Error: $errorText",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    jobRecord?.let { completed ->
                        item(key = "job_summary_${completed.jobId}") {
                            JobRecordSummaryCard(
                                jobRecord = completed,
                                exportedFilePath = exportedFilePath,
                                onExportJson = { viewModel.exportCompletedJob() },
                                onShareJson = { ctx ->
                                    viewModel.getCompletedJobJson()?.let { json ->
                                        shareJobRecordJson(ctx, completed.jobId, json)
                                    }
                                }
                            )
                        }
                    }

                    items(captures, key = { it.id }) { capture ->
                        CaptureCard(capture = capture)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.initializeSession(useMock = true) },
                        enabled = !isSessionActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Session")
                    }
                    OutlinedButton(
                        onClick = { viewModel.stopSession() },
                        enabled = isSessionActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop")
                    }
                    OutlinedButton(
                        onClick = { onNavigateToReview(uiState.jobId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Review (${captures.size})")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel.startVoiceListening()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        enabled = !isVoiceActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Listening")
                    }
                    OutlinedButton(
                        onClick = { viewModel.stopVoiceListening() },
                        enabled = isVoiceActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop Listen")
                    }
                }
            }
        }
    }
}
