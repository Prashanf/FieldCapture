package com.fieldcapture.presentation.session

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.VoiceCommand
import com.fieldcapture.domain.repository.CaptureRepository
import com.fieldcapture.domain.usecase.ProcessCaptureUseCase
import com.fieldcapture.glasses.CameraStreamManager
import com.fieldcapture.glasses.GlassesSessionManager
import com.fieldcapture.glasses.GlassesState
import com.fieldcapture.glasses.MockDeviceManager
import com.fieldcapture.sync.ConnectivityMonitor
import com.fieldcapture.voice.VoiceCommandCallback
import com.fieldcapture.voice.VoiceCommandService
import com.meta.wearable.dat.camera.types.StreamState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SessionUiState(
    val jobId: String = UUID.randomUUID().toString(),
    val lastCommand: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val glassesSessionManager: GlassesSessionManager,
    private val mockDeviceManager: MockDeviceManager,
    private val cameraStreamManager: CameraStreamManager,
    private val voiceCommandService: VoiceCommandService,
    private val processCaptureUseCase: ProcessCaptureUseCase,
    captureRepository: CaptureRepository,
    connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    val glassesState: StateFlow<GlassesState> = glassesSessionManager.state
    val streamState: StateFlow<StreamState?> = glassesSessionManager.streamState
    val cameraFrames: SharedFlow<Bitmap> = glassesSessionManager.frames
    val latestFrame: StateFlow<Bitmap?> = cameraStreamManager.latestFrame
    val isListening: StateFlow<Boolean> = voiceCommandService.isListening

    val isOnline: StateFlow<Boolean> = connectivityMonitor.observeConnectivity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val captures: StateFlow<List<Capture>> = captureRepository
        .observeCapturesForJob(_uiState.value.jobId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        voiceCommandService.setCallback(object : VoiceCommandCallback {
            override fun onCommandRecognized(command: VoiceCommand) {
                handleVoiceCommand(command)
            }

            override fun onPartialText(text: String) {
                _uiState.value = _uiState.value.copy(lastCommand = text)
            }

            override fun onListeningStateChanged(isListening: Boolean) = Unit

            override fun onError(errorCode: Int, message: String) {
                _uiState.value = _uiState.value.copy(errorMessage = message)
            }
        })
    }

    fun initializeSession(useMock: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = null)
            if (useMock) {
                mockDeviceManager.enableMockDevice()
                val setupResult = mockDeviceManager.pairAndSetup()
                if (setupResult.isFailure) {
                    val errMsg = setupResult.exceptionOrNull()?.message ?: "Mock device setup failed"
                    Log.e(TAG, errMsg)
                    _uiState.value = _uiState.value.copy(errorMessage = errMsg)
                    return@launch
                }
            }
            glassesSessionManager.startSession()
        }
    }

    fun stopSession() {
        glassesSessionManager.stopSession()
    }

    fun toggleVoiceListening() {
        if (isListening.value) {
            voiceCommandService.stopListening()
        } else {
            voiceCommandService.startListening()
        }
    }

    fun triggerManualCapture(type: CaptureType) {
        viewModelScope.launch {
            val frame = cameraStreamManager.captureSnapshot()
            processCaptureUseCase(
                jobId = _uiState.value.jobId,
                type = type,
                bitmap = frame
            )
        }
    }

    private fun handleVoiceCommand(command: VoiceCommand) {
        _uiState.value = _uiState.value.copy(lastCommand = command.toString())
        viewModelScope.launch {
            val frame = cameraStreamManager.captureSnapshot()
            when (command) {
                is VoiceCommand.CaptureNameplate -> {
                    processCaptureUseCase(
                        jobId = _uiState.value.jobId,
                        type = CaptureType.NAMEPLATE,
                        bitmap = frame
                    )
                }
                is VoiceCommand.CaptureDamage -> {
                    processCaptureUseCase(
                        jobId = _uiState.value.jobId,
                        type = CaptureType.DAMAGE,
                        bitmap = frame,
                        metadata = mapOf("room" to command.room, "surface" to command.surface)
                    )
                }
                is VoiceCommand.Note -> {
                    processCaptureUseCase(
                        jobId = _uiState.value.jobId,
                        type = CaptureType.NOTE,
                        noteText = command.text
                    )
                }
                is VoiceCommand.Unknown -> Unit
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceCommandService.setCallback(null)
        glassesSessionManager.cleanup()
        mockDeviceManager.disable()
    }

    companion object {
        private const val TAG = "SessionViewModel"
    }
}
