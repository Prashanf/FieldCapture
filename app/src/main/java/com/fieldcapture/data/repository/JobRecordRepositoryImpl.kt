package com.fieldcapture.data.repository

import com.fieldcapture.data.local.dao.JobRecordDao
import com.fieldcapture.data.local.entity.JobRecordEntity
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.repository.JobRecordRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class JobRecordRepositoryImpl @Inject constructor(
    private val jobRecordDao: JobRecordDao,
    private val gson: Gson
) : JobRecordRepository {

    override fun observeJobRecord(jobId: String): Flow<JobRecord?> {
        return jobRecordDao.observeJobRecord(jobId).map { it?.toDomain() }
    }

    override fun observeAllJobRecords(): Flow<List<JobRecord>> {
        return jobRecordDao.observeAllJobRecords().map { list ->
            list.mapNotNull { it.toDomain() }
        }
    }

    override suspend fun getJobRecord(jobId: String): JobRecord? {
        return jobRecordDao.getJobRecord(jobId)?.toDomain()
    }

    override suspend fun saveJobRecord(jobRecord: JobRecord): Result<Unit> = runCatching {
        jobRecordDao.upsertJobRecord(jobRecord.toEntity())
    }

    override suspend fun deleteJobRecord(jobId: String): Result<Unit> = runCatching {
        jobRecordDao.deleteJobRecord(jobId)
    }

    private fun JobRecordEntity.toDomain(): JobRecord? {
        return runCatching {
            gson.fromJson(fullRecordJson, JobRecord::class.java)
        }.getOrNull()
    }

    private fun JobRecord.toEntity(): JobRecordEntity {
        return JobRecordEntity(
            jobId = jobId,
            createdAt = createdAt,
            technicianId = technicianName,
            locationJson = gson.toJson(mapOf("job_address" to jobAddress)),
            sessionJson = gson.toJson(metadata),
            lineItemsJson = gson.toJson(lineItems),
            notesJson = gson.toJson(notes),
            fullRecordJson = gson.toJson(this)
        )
    }
}
