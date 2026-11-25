package com.fieldcapture.domain.repository

import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.domain.model.Capture
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun observeCapturesForJob(jobId: String): Flow<List<Capture>>
    suspend fun getCaptureById(id: String): Capture?
    suspend fun saveCapture(capture: Capture): Result<Unit>
    suspend fun updateCaptureStatus(id: String, status: CaptureStatus): Result<Unit>
    suspend fun getPendingCaptures(): List<Capture>
    suspend fun deleteCapture(id: String): Result<Unit>
}
