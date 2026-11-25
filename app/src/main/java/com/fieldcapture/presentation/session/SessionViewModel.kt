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
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.model.QualityResult
import com.fieldcapture.domain.model.VoiceCommand
import com.fieldcapture.domain.repository.CaptureRepository
import com.fieldcapture.domain.repository.JobRecordRepository
import com.fieldcapture.domain.usecase.BuildJobRecordUseCase
import com.fieldcapture.domain.usecase.CheckCaptureQualityUseCase
import com.fieldcapture.domain.usecase.ExtractCaptureDataUseCase
import com.fieldcapture.extraction.ExtractionMetadata
import com.fieldcapture.extraction.ExtractionResult
import com.fieldcapture.extraction.NameplateExtraction
import com.fieldcapture.glasses.CameraStreamManager
import com.fieldcapture.glasses.GlassesSessionManager
import com.fieldcapture.glasses.GlassesState
import com.fieldcapture.glasses.MockDeviceManager
import com.fieldcapture.network.ConnectivityObserver
import com.fieldcapture.sync.SyncManager
import com.fieldcapture.util.JsonExporter
import com.fieldcapture.voice.AudioFeedbackManager
import com.fieldcapture.voice.VoiceCommandParser
import com.fieldcapture.voice.VoiceCommandService
import com.meta.wearable.dat.camera.types.StreamState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val buildJobRecordUseCase: BuildJobRecordUseCase,
    private val captureRepository: CaptureRepository,
    private val jobRecordRepository: JobRecordRepository,
    private val jsonExporter: JsonExporter,
    private val connectivityObserver: ConnectivityObserver,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    val glassesState: StateFlow<GlassesState> = glassesSessionManager.state
    val streamState: StateFlow<StreamState?> = glassesSessionManager.streamState
    val cameraFrames: SharedFlow<Bitmap> = glassesSessionManager.frames
    val latestFrame: StateFlow<Bitmap?> = cameraStreamManager.latestFrame
    val listeningState: StateFlow<VoiceCommandService.ListeningState> = voiceCommandService.listeningState

    private val _isOnline = MutableStateFlow(connectivityObserver.isOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    val queuedCount: StateFlow<Int> = captureRepository.getPendingExtractionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _captures = MutableStateFlow<List<Capture>>(emptyList())
    val captures: StateFlow<List<Capture>> = _captures.asStateFlow()

    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand.asStateFlow()

    private val _currentJobId = MutableStateFlow<String?>(null)
    val currentJobId: StateFlow<String?> = _currentJobId.asStateFlow()

    private val _jobStartTime = MutableStateFlow<Long?>(null)
    private val _technicianName = MutableStateFlow("Field Technician")
    private val _jobAddress = MutableStateFlow<String?>(null)
    private val _rejectedCapturesCount = MutableStateFlow(0)

    private val _jobRecord = MutableStateFlow<JobRecord?>(null)
    val jobRecord: StateFlow<JobRecord?> = _jobRecord.asStateFlow()

    private val _exportedFilePath = MutableStateFlow<String?>(null)
    val exportedFilePath: StateFlow<String?> = _exportedFilePath.asStateFlow()

    private var wasPreviouslyConnected = false
    private var keepVoiceListeningActive = false
    private var capturesObserverJob: Job? = null

    init {
        audioFeedbackManager.initialize()
        observeConnectivity()
        observeVoiceCommands()
        observeGlassesStateForFeedback()
        observeJobCapturesFromRoom(_uiState.value.jobId)
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.observe().collectLatest { status ->
                _isOnline.value = (status == ConnectivityObserver.Status.AVAILABLE)
            }
        }
    }

    private fun observeJobCapturesFromRoom(jobId: String) {
        capturesObserverJob?.cancel()
        capturesObserverJob = viewModelScope.launch {
            captureRepository.observeCapturesForJob(jobId).collect { dbCaptures ->
                val inMemoryBytes = _captures.value.associate { it.id to it.imageBytes }
                _captures.value = dbCaptures.map { dbCap ->
                    dbCap.copy(imageBytes = dbCap.imageBytes ?: inMemoryBytes[dbCap.id])
                }
            }
        }
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

    fun startJob(technicianName: String = "Field Technician", jobAddress: String? = null) {
        val newJobId = "JOB-${System.currentTimeMillis()}"
        _currentJobId.value = newJobId
        _jobStartTime.value = System.currentTimeMillis()
        _technicianName.value = technicianName
        _jobAddress.value = jobAddress
        _rejectedCapturesCount.value = 0
        _captures.value = emptyList()
        _jobRecord.value = null
        _exportedFilePath.value = null
        _uiState.value = _uiState.value.copy(jobId = newJobId, errorMessage = null)
        observeJobCapturesFromRoom(newJobId)
        audioFeedbackManager.speak("Job started. Ready for captures.")
    }

    fun completeJob() {
        val jobId = _currentJobId.value ?: return
        val startTime = _jobStartTime.value ?: System.currentTimeMillis()

        viewModelScope.launch {
            val pendingExtractions = _captures.value.any { it.status == CaptureStatus.EXTRACTING }
            if (pendingExtractions) {
                audioFeedbackManager.speak("Waiting for extractions to complete.")
                var waited = 0
                while (_captures.value.any { it.status == CaptureStatus.EXTRACTING } && waited < 30) {
                    delay(1000)
                    waited++
                }
            }

            val record = buildJobRecordUseCase.execute(
                jobId = jobId,
                technicianName = _technicianName.value,
                jobAddress = _jobAddress.value,
                captures = _captures.value,
                jobStartTime = startTime,
                jobEndTime = System.currentTimeMillis(),
                rejectedCaptures = _rejectedCapturesCount.value
            )

            jobRecordRepository.saveJobRecord(record)
            val file = jsonExporter.exportJobRecord(record)
            _jobRecord.value = record
            _exportedFilePath.value = file.absolutePath
            _currentJobId.value = null

            audioFeedbackManager.speak(
                "Job complete. ${record.summary.totalCaptures} captures, " +
                    "${record.summary.totalLineItems} line items, " +
                    "estimated total ${String.format(Locale.US, "%.0f", record.summary.estimateSubtotal)} dollars."
            )

            Log.i(TAG, "Job exported to: ${file.absolutePath}")
            Log.i(TAG, "JSON preview:\n${jsonExporter.toJsonString(record).take(500)}")
        }
    }

    fun retryQueuedExtractions() {
        syncManager.triggerQueueProcessing()
    }

    fun exportCompletedJob() {
        val record = _jobRecord.value ?: return
        val file = jsonExporter.exportJobRecord(record)
        _exportedFilePath.value = file.absolutePath
    }

    fun getCompletedJobJson(): String? {
        val record = _jobRecord.value ?: return null
        return jsonExporter.toJsonString(record)
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

                    viewModelScope.launch {
                        runExtraction(capture)
                    }
                } else {
                    _rejectedCapturesCount.value += 1
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
        if (connectivityObserver.isOnline()) {
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
                    Log.w(TAG, "Extraction failed, queued for retry: ${error.message}")
                    updateCaptureStatus(capture.id, CaptureStatus.QUEUED)
                    syncManager.triggerQueueProcessing()
                }
            )
        } else {
            updateCaptureStatus(capture.id, CaptureStatus.QUEUED)
            audioFeedbackManager.speak("Offline. Capture saved. Will extract when connected.")
            Log.i(TAG, "Offline — capture ${capture.id} queued")
        }
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
