package com.fieldcapture.data.repository

import com.fieldcapture.data.local.dao.JobRecordDao
import com.fieldcapture.data.local.entity.JobRecordEntity
import com.fieldcapture.domain.model.JobLocation
import com.fieldcapture.domain.model.JobNote
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.model.LineItem
import com.fieldcapture.domain.model.SessionMetadata
import com.fieldcapture.domain.repository.JobRecordRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
            list.map { it.toDomain() }
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

    private fun JobRecordEntity.toDomain(): JobRecord {
        val location = runCatching {
            gson.fromJson(locationJson, JobLocation::class.java)
        }.getOrDefault(JobLocation())
        val session = runCatching {
            gson.fromJson(sessionJson, SessionMetadata::class.java)
        }.getOrDefault(SessionMetadata(startedAt = createdAt))
        val lineItemsType = object : TypeToken<List<LineItem>>() {}.type
        val lineItems: List<LineItem> = runCatching {
            gson.fromJson<List<LineItem>>(lineItemsJson, lineItemsType)
        }.getOrDefault(emptyList())
        val notesType = object : TypeToken<List<JobNote>>() {}.type
        val notes: List<JobNote> = runCatching {
            gson.fromJson<List<JobNote>>(notesJson, notesType)
        }.getOrDefault(emptyList())

        // TODO: Also hydrate captures list from CaptureRepository or fullRecordJson
        return JobRecord(
            jobId = jobId,
            createdAt = createdAt,
            technicianId = technicianId,
            location = location,
            session = session,
            captures = emptyList(),
            lineItems = lineItems,
            notes = notes
        )
    }

    private fun JobRecord.toEntity(): JobRecordEntity {
        return JobRecordEntity(
            jobId = jobId,
            createdAt = createdAt,
            technicianId = technicianId,
            locationJson = gson.toJson(location),
            sessionJson = gson.toJson(session),
            lineItemsJson = gson.toJson(lineItems),
            notesJson = gson.toJson(notes),
            fullRecordJson = gson.toJson(this)
        )
    }
}
