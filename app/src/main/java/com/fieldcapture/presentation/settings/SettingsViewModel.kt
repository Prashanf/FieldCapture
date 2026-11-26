/*
 * Copyright (c) 2026 Prashant Sharma. All Rights Reserved.
 * This code is submitted as part of a technical assessment and may not be
 * used, copied, or distributed without explicit written permission.
 */
package com.fieldcapture.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcapture.data.local.ApiKeyManager
import com.fieldcapture.network.GeminiApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val inputKey: String = "",
    val hasUserKey: Boolean = false,
    val hasKey: Boolean = false,
    val maskedKey: String? = null,
    val keyStatus: String = "No key set",
    val isKeyInvalid: Boolean = false,
    val isTesting: Boolean = false,
    val testResultMessage: String? = null,
    val testSuccess: Boolean? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val geminiApiClient: GeminiApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun buildInitialState(): SettingsUiState {
        val userKeySet = apiKeyManager.hasUserKey()
        val activeKey = apiKeyManager.getActiveKey()
        return SettingsUiState(
            hasUserKey = userKeySet,
            hasKey = activeKey != null,
            maskedKey = apiKeyManager.getMaskedActiveKey(),
            keyStatus = when {
                userKeySet -> "Key saved ✓"
                activeKey != null -> "Developer key active ✓"
                else -> "No key set"
            }
        )
    }

    fun onKeyInputChange(newValue: String) {
        _uiState.value = _uiState.value.copy(
            inputKey = newValue,
            testResultMessage = null
        )
    }

    fun saveKey() {
        val trimmed = _uiState.value.inputKey.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(
                testSuccess = false,
                testResultMessage = "Please enter a non-empty Gemini API key."
            )
            return
        }
        apiKeyManager.saveUserKey(trimmed)
        _uiState.value = SettingsUiState(
            inputKey = "",
            hasUserKey = true,
            hasKey = true,
            maskedKey = apiKeyManager.getMaskedActiveKey(),
            keyStatus = "Key saved ✓",
            isKeyInvalid = false,
            testSuccess = true,
            testResultMessage = "API key encrypted and saved on device."
        )
    }

    fun testKey() {
        val candidate = _uiState.value.inputKey.trim().ifBlank {
            apiKeyManager.getActiveKey().orEmpty()
        }
        if (candidate.isBlank()) {
            _uiState.value = _uiState.value.copy(
                keyStatus = "No key set",
                isKeyInvalid = true,
                testSuccess = false,
                testResultMessage = "Enter or save a Gemini API key first."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isTesting = true,
            testResultMessage = null
        )

        viewModelScope.launch {
            val result = geminiApiClient.testApiKey(candidate)
            result.fold(
                onSuccess = { message ->
                    val status = if (apiKeyManager.hasUserKey()) "Key saved ✓" else "Key valid ✓"
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        isKeyInvalid = false,
                        keyStatus = status,
                        testSuccess = true,
                        testResultMessage = message
                    )
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to verify key"
                    val invalid = msg.contains("invalid", ignoreCase = true) ||
                        msg.contains("400") || msg.contains("401") || msg.contains("403")
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        isKeyInvalid = invalid,
                        keyStatus = if (invalid) "Key invalid ✗" else _uiState.value.keyStatus,
                        testSuccess = false,
                        testResultMessage = msg
                    )
                }
            )
        }
    }

    fun clearKey() {
        apiKeyManager.clearUserKey()
        val fallbackActive = apiKeyManager.getActiveKey()
        _uiState.value = SettingsUiState(
            inputKey = "",
            hasUserKey = false,
            hasKey = fallbackActive != null,
            maskedKey = apiKeyManager.getMaskedActiveKey(),
            keyStatus = if (fallbackActive != null) "Developer key active ✓" else "No key set",
            isKeyInvalid = false,
            testResultMessage = if (fallbackActive != null) {
                "User key cleared; falling back to local.properties key."
            } else {
                "Saved API key cleared."
            }
        )
    }
}
