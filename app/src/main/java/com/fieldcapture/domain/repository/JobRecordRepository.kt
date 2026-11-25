package com.fieldcapture.domain.repository

import com.fieldcapture.domain.model.JobRecord
import kotlinx.coroutines.flow.Flow

interface JobRecordRepository {
    fun observeJobRecord(jobId: String): Flow<JobRecord?>
    fun observeAllJobRecords(): Flow<List<JobRecord>>
    suspend fun getJobRecord(jobId: String): JobRecord?
    suspend fun saveJobRecord(jobRecord: JobRecord): Result<Unit>
    suspend fun deleteJobRecord(jobId: String): Result<Unit>
}
