package com.fieldcapture.data.repository

import com.fieldcapture.data.local.dao.CaptureDao
import com.fieldcapture.data.local.entity.CaptureEntity
import com.fieldcapture.data.local.entity.CaptureStatus
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureMetadata
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.QualityResult
import com.fieldcapture.domain.repository.CaptureRepository
import com.fieldcapture.extraction.DamageExtraction
import com.fieldcapture.extraction.ExtractionResult
import com.fieldcapture.extraction.NameplateExtraction
import com.fieldcapture.extraction.NoteExtraction
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
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

    override suspend fun updateExtraction(
        id: String,
        result: ExtractionResult,
        status: CaptureStatus
    ): Result<Unit> = runCatching {
        captureDao.updateExtraction(id, gson.toJson(result), status)
    }

    override suspend fun getCapturesByStatus(status: CaptureStatus): List<Capture> {
        return captureDao.getCapturesByStatus(status).map { it.toDomain() }
    }

    override fun getPendingExtractionCount(): Flow<Int> {
        return captureDao.getPendingExtractionCount()
    }

    override suspend fun loadImageBytes(captureId: String): ByteArray? {
        val capture = captureDao.getCaptureById(captureId) ?: return null
        val path = capture.imagePath ?: return null
        val file = File(path)
        return if (file.exists()) file.readBytes() else null
    }

    override suspend fun getPendingCaptures(): List<Capture> {
        return captureDao.getCapturesByStatuses(
            listOf(CaptureStatus.CAPTURED, CaptureStatus.EXTRACTION_FAILED, CaptureStatus.QUEUED)
        ).map { it.toDomain() }
    }

    override suspend fun deleteCapture(id: String): Result<Unit> = runCatching {
        captureDao.deleteCapture(id)
    }

    private fun CaptureEntity.toDomain(): Capture {
        val captureType = CaptureType.valueOf(type)
        val quality = qualityJson?.let {
            runCatching { gson.fromJson(it, QualityResult::class.java) }.getOrNull()
        }
        val parsedMetadata = metadataJson?.let {
            runCatching { gson.fromJson(it, CaptureMetadata::class.java) }.getOrNull()
        } ?: CaptureMetadata(noteText = noteText)

        val extraction: ExtractionResult? = extractionJson?.let { json ->
            runCatching {
                when (captureType) {
                    CaptureType.NAMEPLATE -> gson.fromJson(json, NameplateExtraction::class.java)
                    CaptureType.DAMAGE -> gson.fromJson(json, DamageExtraction::class.java)
                    CaptureType.NOTE -> gson.fromJson(json, NoteExtraction::class.java)
                }
            }.getOrNull()
        }

        val epochMillis = timestamp.toLongOrNull()
            ?: runCatching { Instant.parse(timestamp).toEpochMilli() }.getOrElse { System.currentTimeMillis() }

        return Capture(
            id = id,
            jobId = jobId,
            type = captureType,
            timestamp = epochMillis,
            imagePath = imagePath,
            metadata = if (parsedMetadata.noteText == null && noteText != null) {
                parsedMetadata.copy(noteText = noteText)
            } else {
                parsedMetadata
            },
            status = status,
            qualityResult = quality,
            extractionResult = extraction
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
