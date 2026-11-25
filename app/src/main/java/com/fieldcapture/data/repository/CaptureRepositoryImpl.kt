package com.fieldcapture.data.repository

import com.fieldcapture.data.local.dao.CaptureDao
import com.fieldcapture.data.local.entity.CaptureEntity
import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.QualityResult
import com.fieldcapture.domain.repository.CaptureRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CaptureRepositoryImpl @Inject constructor(
    private val captureDao: CaptureDao,
    private val gson: Gson
) : CaptureRepository {

    override fun observeCapturesForJob(jobId: String): Flow<List<Capture>> {
        return captureDao.observeCapturesForJob(jobId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCaptureById(id: String): Capture? {
        return captureDao.getCaptureById(id)?.toDomain()
    }

    override suspend fun saveCapture(capture: Capture): Result<Unit> = runCatching {
        captureDao.upsertCapture(capture.toEntity())
    }

    override suspend fun updateCaptureStatus(id: String, status: CaptureStatus): Result<Unit> = runCatching {
        captureDao.updateStatus(id, status)
    }

    override suspend fun getPendingCaptures(): List<Capture> {
        return captureDao.getCapturesByStatus(
            listOf(CaptureStatus.PENDING, CaptureStatus.FAILED)
        ).map { it.toDomain() }
    }

    override suspend fun deleteCapture(id: String): Result<Unit> = runCatching {
        captureDao.deleteCapture(id)
    }

    private fun CaptureEntity.toDomain(): Capture {
        val quality = qualityJson?.let {
            runCatching { gson.fromJson(it, QualityResult::class.java) }.getOrNull()
        }
        val metadataType = object : TypeToken<Map<String, String>>() {}.type
        val metadata: Map<String, String> = metadataJson?.let {
            runCatching { gson.fromJson<Map<String, String>>(it, metadataType) }.getOrNull()
        } ?: emptyMap()

        // TODO: Deserialize polymorphic ExtractionResult from extractionJson
        return Capture(
            id = id,
            jobId = jobId,
            type = CaptureType.valueOf(type),
            timestampIso = timestamp,
            imagePath = imagePath,
            noteText = noteText,
            status = status,
            quality = quality,
            extraction = null,
            metadata = metadata
        )
    }

    private fun Capture.toEntity(): CaptureEntity {
        return CaptureEntity(
            id = id,
            jobId = jobId,
            type = type.name,
            imagePath = imagePath,
            noteText = noteText,
            status = status,
            qualityJson = quality?.let { gson.toJson(it) },
            extractionJson = extraction?.let { gson.toJson(it) },
            metadataJson = gson.toJson(metadata),
            timestamp = timestampIso
        )
    }
}
