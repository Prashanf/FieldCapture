package com.fieldcapture.presentation.jobrecord

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.repository.CaptureRepository
import com.fieldcapture.domain.repository.JobRecordRepository
import com.fieldcapture.domain.usecase.GenerateJobRecordUseCase
import com.fieldcapture.util.JsonExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobRecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val captureRepository: CaptureRepository,
    jobRecordRepository: JobRecordRepository,
    private val generateJobRecordUseCase: GenerateJobRecordUseCase,
    private val jsonExporter: JsonExporter
) : ViewModel() {

    val jobId: String = savedStateHandle["jobId"] ?: ""

    val jobRecord: StateFlow<JobRecord?> = jobRecordRepository
        .observeJobRecord(jobId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    init {
        generateRecord()
    }

    fun generateRecord() {
        viewModelScope.launch {
            val captures = captureRepository.observeCapturesForJob(jobId).first()
            generateJobRecordUseCase(
                jobId = jobId,
                technicianName = "Field Technician",
                captures = captures
            )
        }
    }

    fun exportJson() {
        val current = jobRecord.value ?: return
        runCatching { jsonExporter.exportJobRecord(current) }
            .onSuccess { file -> _exportStatus.value = file.absolutePath }
            .onFailure { err -> _exportStatus.value = "Export failed: ${err.message}" }
    }

    fun currentRecordJson(): String? {
        val current = jobRecord.value ?: return null
        return jsonExporter.toJsonString(current)
    }
}
