package com.fieldcapture.domain.usecase

import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.ExtractionResult
import com.fieldcapture.domain.model.JobLocation
import com.fieldcapture.domain.model.JobNote
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.model.SessionMetadata
import com.fieldcapture.domain.repository.JobRecordRepository
import java.time.Instant
import javax.inject.Inject

class GenerateJobRecordUseCase @Inject constructor(
    private val mapToLineItemsUseCase: MapToLineItemsUseCase,
    private val jobRecordRepository: JobRecordRepository
) {
    suspend operator fun invoke(
        jobId: String,
        technicianId: String,
        session: SessionMetadata,
        captures: List<Capture>,
        location: JobLocation = JobLocation()
    ): Result<JobRecord> {
        // TODO: Aggregate captures, build JobNotes, map line items, persist via JobRecordRepository
        val lineItems = mapToLineItemsUseCase(captures)
        val notes = captures
            .filter { it.type == CaptureType.NOTE }
            .map { capture ->
                val noteData = capture.extraction as? ExtractionResult.NoteData
                JobNote(
                    text = capture.noteText.orEmpty(),
                    structured = noteData?.structuredFields ?: emptyMap(),
                    timestamp = capture.timestampIso
                )
            }

        val record = JobRecord(
            jobId = jobId,
            createdAt = Instant.now().toString(),
            technicianId = technicianId,
            location = location,
            session = session.copy(totalCaptures = captures.size),
            captures = captures,
            lineItems = lineItems,
            notes = notes
        )

        return jobRecordRepository.saveJobRecord(record).map { record }
    }
}
