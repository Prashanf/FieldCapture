package com.fieldcapture.data.repository

import com.fieldcapture.data.local.dao.CaptureDao
import com.fieldcapture.data.local.entity.CaptureEntity
import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureMetadata
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.QualityResult
import com.fieldcapture.domain.repository.CaptureRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
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
            listOf(CaptureStatus.PENDING, CaptureStatus.QUEUED, CaptureStatus.FAILED)
        ).map { it.toDomain() }
    }

    override suspend fun deleteCapture(id: String): Result<Unit> = runCatching {
        captureDao.deleteCapture(id)
    }

    private fun CaptureEntity.toDomain(): Capture {
        val quality = qualityJson?.let {
            runCatching { gson.fromJson(it, QualityResult::class.java) }.getOrNull()
        }
        val parsedMetadata = metadataJson?.let {
            runCatching { gson.fromJson(it, CaptureMetadata::class.java) }.getOrNull()
        } ?: CaptureMetadata(noteText = noteText)

        val epochMillis = timestamp.toLongOrNull()
            ?: runCatching { Instant.parse(timestamp).toEpochMilli() }.getOrElse { System.currentTimeMillis() }

        // TODO: Deserialize polymorphic ExtractionResult from extractionJson
        return Capture(
            id = id,
            jobId = jobId,
            type = CaptureType.valueOf(type),
            timestamp = epochMillis,
            imagePath = imagePath,
            metadata = if (parsedMetadata.noteText == null && noteText != null) {
                parsedMetadata.copy(noteText = noteText)
            } else {
                parsedMetadata
            },
            status = status,
            qualityResult = quality,
            extractionResult = null
        )
    }

    private fun Capture.toEntity(): CaptureEntity {
        return CaptureEntity(
            id = id,
            jobId = jobId,
            type = type.name,
            imagePath = imagePath,
            noteText = metadata.noteText,
            status = status,
            qualityJson = qualityResult?.let { gson.toJson(it) },
            extractionJson = extractionResult?.let { gson.toJson(it) },
            metadataJson = gson.toJson(metadata),
            timestamp = timestampIso
        )
    }
}
