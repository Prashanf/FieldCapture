package com.fieldcapture.domain.usecase

import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.JobLocation
import com.fieldcapture.domain.model.JobNote
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.model.SessionMetadata
import com.fieldcapture.domain.repository.JobRecordRepository
import com.fieldcapture.extraction.NoteExtraction
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
        val lineItems = mapToLineItemsUseCase(captures)
        val notes = captures
            .filter { it.type == CaptureType.NOTE }
            .map { capture ->
                val noteData = capture.extractionResult as? NoteExtraction
                val structured = if (noteData != null) {
                    mapOf(
                        "category" to noteData.category,
                        "transcribed_note" to noteData.transcribedNote,
                        "key_facts" to noteData.keyFacts.joinToString("; ")
                    )
                } else {
                    emptyMap()
                }
                JobNote(
                    text = capture.metadata.noteText.orEmpty(),
                    structured = structured,
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
