package com.fieldcapture.presentation.session

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureMetadata
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.QualityResult
import com.fieldcapture.domain.model.VoiceCommand
import com.fieldcapture.domain.repository.CaptureRepository
import com.fieldcapture.domain.usecase.CheckCaptureQualityUseCase
import com.fieldcapture.domain.usecase.ExtractCaptureDataUseCase
import com.fieldcapture.extraction.ExtractionMetadata
import com.fieldcapture.extraction.ExtractionResult
import com.fieldcapture.extraction.NameplateExtraction
import com.fieldcapture.glasses.CameraStreamManager
import com.fieldcapture.glasses.GlassesSessionManager
import com.fieldcapture.glasses.GlassesState
import com.fieldcapture.glasses.MockDeviceManager
import com.fieldcapture.sync.ConnectivityMonitor
import com.fieldcapture.voice.AudioFeedbackManager
import com.fieldcapture.voice.VoiceCommandParser
import com.fieldcapture.voice.VoiceCommandService
import com.meta.wearable.dat.camera.types.StreamState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class SessionUiState(
    val jobId: String = UUID.randomUUID().toString(),
    val lastRecognizedText: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val glassesSessionManager: GlassesSessionManager,
    private val mockDeviceManager: MockDeviceManager,
    private val cameraStreamManager: CameraStreamManager,
    private val voiceCommandService: VoiceCommandService,
    private val voiceCommandParser: VoiceCommandParser,
    private val audioFeedbackManager: AudioFeedbackManager,
    private val checkCaptureQualityUseCase: CheckCaptureQualityUseCase,
    private val extractCaptureDataUseCase: ExtractCaptureDataUseCase,
    private val captureRepository: CaptureRepository,
    connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    val glassesState: StateFlow<GlassesState> = glassesSessionManager.state
    val streamState: StateFlow<StreamState?> = glassesSessionManager.streamState
    val cameraFrames: SharedFlow<Bitmap> = glassesSessionManager.frames
    val latestFrame: StateFlow<Bitmap?> = cameraStreamManager.latestFrame
    val listeningState: StateFlow<VoiceCommandService.ListeningState> = voiceCommandService.listeningState

    val isOnline: StateFlow<Boolean> = connectivityMonitor.observeConnectivity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _captures = MutableStateFlow<List<Capture>>(emptyList())
    val captures: StateFlow<List<Capture>> = _captures.asStateFlow()

    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand.asStateFlow()

    private var wasPreviouslyConnected = false
    private var keepVoiceListeningActive = false

    init {
        audioFeedbackManager.initialize()
        observeVoiceCommands()
        observeGlassesStateForFeedback()
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

    fun startVoiceListening() {
        keepVoiceListeningActive = true
        voiceCommandService.startListening()
    }

    fun stopVoiceListening() {
        keepVoiceListeningActive = false
        voiceCommandService.stopListening()
    }

    private fun observeVoiceCommands() {
        viewModelScope.launch {
            voiceCommandService.recognizedText.collect { rawText ->
                val command = voiceCommandParser.parse(rawText)
                _uiState.value = _uiState.value.copy(lastRecognizedText = rawText)
                _lastCommand.value = command
                handleCommand(command, rawText)
            }
        }
    }

    private fun observeGlassesStateForFeedback() {
        viewModelScope.launch {
            glassesState.collect { state ->
                when (state) {
                    is GlassesState.Connected -> {
                        wasPreviouslyConnected = true
                        audioFeedbackManager.sessionConnected()
                    }
                    is GlassesState.Disconnected -> {
                        if (wasPreviouslyConnected) {
                            wasPreviouslyConnected = false
                            audioFeedbackManager.sessionDisconnected()
                        }
                    }
                    is GlassesState.Error -> {
                        audioFeedbackManager.sessionDisconnected()
                    }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun handleCommand(command: VoiceCommand, rawText: String) {
        when (command) {
            is VoiceCommand.CaptureNameplate -> {
                captureFrame(
                    type = CaptureType.NAMEPLATE,
                    metadata = CaptureMetadata(voiceCommandRaw = rawText)
                )
            }
            is VoiceCommand.CaptureDamage -> {
                captureFrame(
                    type = CaptureType.DAMAGE,
                    metadata = CaptureMetadata(
                        room = command.room,
                        surface = command.surface,
                        voiceCommandRaw = rawText
                    )
                )
            }
            is VoiceCommand.Note -> {
                val capture = Capture(
                    jobId = _uiState.value.jobId,
                    type = CaptureType.NOTE,
                    metadata = CaptureMetadata(
                        noteText = command.text,
                        voiceCommandRaw = rawText
                    ),
                    status = CaptureStatus.CAPTURED
                )
                addCapture(capture)
                audioFeedbackManager.noteRecorded()
                viewModelScope.launch {
                    runExtraction(capture)
                }
            }
            is VoiceCommand.Unknown -> {
                audioFeedbackManager.commandNotRecognized()
            }
        }
    }

    private suspend fun captureFrame(type: CaptureType, metadata: CaptureMetadata) {
        val resumeAfterCapture = keepVoiceListeningActive
        // Pause voice recognition during capture/TTS so SpeechRecognizer doesn't pick up TTS audio
        voiceCommandService.stopListening()

        val result = glassesSessionManager.capturePhoto()
        result.fold(
            onSuccess = { imageBytes ->
                val qualityResult = checkCaptureQualityUseCase.execute(imageBytes, type)
                if (qualityResult.isAcceptable) {
                    val imagePath = saveImageToFile(imageBytes, type)
                    val capture = Capture(
                        jobId = _uiState.value.jobId,
                        type = type,
                        imagePath = imagePath,
                        imageBytes = imageBytes,
                        metadata = metadata,
                        status = CaptureStatus.CAPTURED,
                        qualityResult = qualityResult,
                        extractionResult = null
                    )
                    addCapture(capture)
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                    audioFeedbackManager.confirmCapture(type.name.lowercase())

                    // Trigger Gemini AI extraction asynchronously so capture flow stays non-blocking
                    viewModelScope.launch {
                        runExtraction(capture)
                    }
                } else {
                    val reason = qualityResult.rejectionReason ?: "poor quality"
                    _uiState.value = _uiState.value.copy(errorMessage = "Retake needed: $reason")
                    audioFeedbackManager.requestRetake(reason)
                    logQualityRejection(type, qualityResult)
                }
            },
            onFailure = { error ->
                Log.w(TAG, "Capture failed: ${error.message}")
                _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Capture failed")
                audioFeedbackManager.requestRetake("capture failed")
            }
        )

        if (resumeAfterCapture) {
            viewModelScope.launch {
                delay(TTS_RESUME_DELAY_MS)
                if (keepVoiceListeningActive) {
                    voiceCommandService.startListening()
                }
            }
        }
    }

    private suspend fun runExtraction(capture: Capture) {
        updateCaptureStatus(capture.id, CaptureStatus.EXTRACTING)

        val extractionMetadata = ExtractionMetadata(
            room = capture.metadata.room,
            surface = capture.metadata.surface,
            spokenText = capture.metadata.noteText,
            captureTimestamp = capture.timestamp
        )

        val result = extractCaptureDataUseCase.execute(
            imageBytes = capture.imageBytes,
            captureType = capture.type,
            metadata = extractionMetadata
        )

        result.fold(
            onSuccess = { extractionResult ->
                updateCaptureWithExtraction(capture.id, extractionResult, CaptureStatus.EXTRACTED)
                Log.d(
                    TAG,
                    "Extraction complete for ${capture.type}: confidence=${extractionResult.overallConfidence}"
                )
                if (capture.type == CaptureType.NAMEPLATE &&
                    extractionResult is NameplateExtraction &&
                    extractionResult.error == null
                ) {
                    val mfg = extractionResult.manufacturer ?: "unknown"
                    audioFeedbackManager.speak(
                        "Nameplate extracted. $mfg ${extractionResult.equipmentType ?: "equipment"}"
                    )
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Extraction failed for ${capture.id}", error)
                updateCaptureStatus(capture.id, CaptureStatus.EXTRACTION_FAILED)
            }
        )
    }

    private fun updateCaptureStatus(captureId: String, status: CaptureStatus) {
        _captures.value = _captures.value.map { capture ->
            if (capture.id == captureId) capture.copy(status = status) else capture
        }
        viewModelScope.launch {
            captureRepository.updateCaptureStatus(captureId, status)
        }
    }

    private fun updateCaptureWithExtraction(
        captureId: String,
        result: ExtractionResult,
        status: CaptureStatus
    ) {
        _captures.value = _captures.value.map { capture ->
            if (capture.id == captureId) {
                capture.copy(status = status, extractionResult = result)
            } else {
                capture
            }
        }
        viewModelScope.launch {
            captureRepository.updateExtraction(captureId, result, status)
        }
    }

    private fun logQualityRejection(type: CaptureType, result: QualityResult) {
        Log.w("QualityCheck", buildString {
            append("REJECTED ${type.name}: ")
            append("blur=${result.blurScore.format(1)}, ")
            append("brightness=${result.brightnessScore}, ")
            append("edges=${result.edgeDensity.format(1)}%, ")
            append("reason=${result.rejectionReason}")
        })
    }

    private fun Double.format(decimals: Int): String =
        String.format(Locale.US, "%.${decimals}f", this)

    private suspend fun addCapture(capture: Capture) {
        _captures.value = _captures.value + capture
        captureRepository.saveCapture(capture)
    }

    private fun saveImageToFile(imageBytes: ByteArray, type: CaptureType): String {
        val dir = File(context.filesDir, "captures")
        dir.mkdirs()
        val fileName = "${type.name.lowercase()}_${System.currentTimeMillis()}.jpg"
        val file = File(dir, fileName)
        file.writeBytes(imageBytes)
        return file.absolutePath
    }

    fun stopSession() {
        stopVoiceListening()
        glassesSessionManager.stopSession()
    }

    override fun onCleared() {
        super.onCleared()
        stopVoiceListening()
        audioFeedbackManager.shutdown()
        glassesSessionManager.cleanup()
        mockDeviceManager.disable()
    }

    companion object {
        private const val TAG = "SessionViewModel"
        private const val TTS_RESUME_DELAY_MS = 1500L
    }
}
