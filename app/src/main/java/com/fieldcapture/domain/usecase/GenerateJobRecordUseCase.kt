package com.fieldcapture.domain.usecase

import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.repository.JobRecordRepository
import javax.inject.Inject

class GenerateJobRecordUseCase @Inject constructor(
    private val buildJobRecordUseCase: BuildJobRecordUseCase,
    private val jobRecordRepository: JobRecordRepository
) {
    suspend operator fun invoke(
        jobId: String,
        technicianName: String,
        jobAddress: String? = null,
        captures: List<Capture>,
        jobStartTime: Long = System.currentTimeMillis(),
        jobEndTime: Long? = null,
        rejectedCaptures: Int = 0
    ): Result<JobRecord> {
        val record = buildJobRecordUseCase.execute(
            jobId = jobId,
            technicianName = technicianName,
            jobAddress = jobAddress,
            captures = captures,
            jobStartTime = jobStartTime,
            jobEndTime = jobEndTime,
            rejectedCaptures = rejectedCaptures
        )
        return jobRecordRepository.saveJobRecord(record).map { record }
    }
}
