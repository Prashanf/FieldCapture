package com.fieldcapture.presentation.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.repository.CaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val captureRepository: CaptureRepository
) : ViewModel() {

    val jobId: String = savedStateHandle["jobId"] ?: ""

    val captures: StateFlow<List<Capture>> = captureRepository
        .observeCapturesForJob(jobId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteCapture(captureId: String) {
        viewModelScope.launch {
            captureRepository.deleteCapture(captureId)
        }
    }

    // TODO: Add field editing methods so technicians can correct extracted values before finalizing
}
