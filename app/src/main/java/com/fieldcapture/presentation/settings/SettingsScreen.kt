/*
 * Copyright (c) 2026 Prashant Sharma. All Rights Reserved.
 * This code is submitted as part of a technical assessment and may not be
 * used, copied, or distributed without explicit written permission.
 */
package com.fieldcapture.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showRawInput by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Key Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        state.isKeyInvalid -> MaterialTheme.colorScheme.errorContainer
                        state.hasKey -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Status: ${state.keyStatus}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    state.maskedKey?.let { masked ->
                        Text(
                            text = "Active key: $masked",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = if (state.hasUserKey) {
                            "Stored securely in AES-256-GCM EncryptedSharedPreferences (overrides developer fallback keys)."
                        } else if (state.hasKey) {
                            "Using BuildConfig key(s) from local.properties. Enter a key below to override."
                        } else {
                            "No API key configured. Get a free key at aistudio.google.com/apikey."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            OutlinedTextField(
                value = state.inputKey,
                onValueChange = { viewModel.onKeyInputChange(it) },
                label = { Text("Gemini API Key") },
                placeholder = { Text("Paste key from Google AI Studio") },
                singleLine = true,
                visualTransformation = if (showRawInput) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showRawInput = !showRawInput }) {
                        Icon(
                            imageVector = if (showRawInput) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (showRawInput) "Hide key" else "Show key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.saveKey() },
                    enabled = !state.isTesting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = { viewModel.testKey() },
                    enabled = !state.isTesting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Test Key")
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.clearKey() },
                    enabled = !state.isTesting && state.hasUserKey,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Key")
                }
            }

            state.testResultMessage?.let { message ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    color = when (state.testSuccess) {
                        false -> MaterialTheme.colorScheme.error
                        true -> MaterialTheme.colorScheme.primary
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
