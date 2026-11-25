package com.fieldcapture.data

import android.app.Application
import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.CaptureMetadata
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.domain.model.JobMetadata
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.model.JobSummary
import com.fieldcapture.domain.model.QualityResult
import com.fieldcapture.domain.usecase.BuildJobRecordUseCase
import com.fieldcapture.domain.usecase.MapToLineItemsUseCase
import com.fieldcapture.extraction.DamageConfidence
import com.fieldcapture.extraction.DamageExtraction
import com.fieldcapture.extraction.NameplateConfidence
import com.fieldcapture.extraction.NameplateExtraction
import com.fieldcapture.extraction.NoteConfidence
import com.fieldcapture.extraction.NoteExtraction
import com.fieldcapture.extraction.RecommendedLineItem
import com.fieldcapture.util.JsonExporter
import com.fieldcapture.util.SampleDataGenerator
import com.google.gson.GsonBuilder
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JobRecordSchemaTest {

    private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    @Test
    fun `job record serializes to valid JSON with all required fields`() {
        val record = JobRecord(
            jobId = "TEST-001",
            createdAt = "2024-01-01T00:00:00.000Z",
            completedAt = "2024-01-01T01:00:00.000Z",
            technicianName = "Test Tech",
            jobAddress = "123 Test St",
            equipment = emptyList(),
            damageAssessments = emptyList(),
            notes = emptyList(),
            lineItems = emptyList(),
            summary = JobSummary(0, 0, 0, 0, 0, 0, 0.0, 0.0, emptyList(), 3600),
            metadata = JobMetadata()
        )

        val json = gson.toJson(record)
        assertTrue(json.contains("schema_version"))
        assertTrue(json.contains("job_id"))
        assertTrue(json.contains("line_items"))
        assertTrue(json.contains("summary"))
        assertTrue(json.contains("metadata"))

        val parsed = gson.fromJson(json, JobRecord::class.java)
        assertEquals("TEST-001", parsed.jobId)
        assertEquals("1.0.0", parsed.schemaVersion)
    }

    @Test
    fun `BuildJobRecordUseCase assembles captures into traceable job record`() {
        val builder = BuildJobRecordUseCase(MapToLineItemsUseCase())
        val captures = listOf(
            Capture(
                id = "cap-eq-1",
                jobId = "JOB-100",
                type = CaptureType.NAMEPLATE,
                imagePath = "/tmp/nameplate.jpg",
                qualityResult = QualityResult(
                    isAcceptable = true,
                    blurScore = 420.0,
                    brightnessScore = 140,
                    edgeDensity = 10.5,
                    isBlurry = false,
                    isDark = false,
                    isOverexposed = false,
                    isBadlyFramed = false,
                    rejectionReason = null
                ),
                extractionResult = NameplateExtraction(
                    equipmentType = "Water Heater",
                    manufacturer = "Rheem",
                    modelNumber = "XG40",
                    confidence = NameplateConfidence(equipmentType = 0.9, manufacturer = 0.9),
                    overallConfidence = 0.9
                )
            ),
            Capture(
                id = "cap-dmg-1",
                jobId = "JOB-100",
                type = CaptureType.DAMAGE,
                imagePath = "/tmp/damage.jpg",
                metadata = CaptureMetadata(room = "kitchen", surface = "floor"),
                qualityResult = QualityResult(
                    isAcceptable = true,
                    blurScore = 300.0,
                    brightnessScore = 150,
                    edgeDensity = 8.0,
                    isBlurry = false,
                    isDark = false,
                    isOverexposed = false,
                    isBadlyFramed = false,
                    rejectionReason = null
                ),
                extractionResult = DamageExtraction(
                    damageType = "water_damage",
                    severity = "high",
                    description = "Buckled flooring",
                    affectedMaterials = listOf("vinyl flooring"),
                    recommendedLineItems = listOf(
                        RecommendedLineItem("WATER_EXTRACTION", "Standing water", "high", "20 sq ft")
                    ),
                    confidence = DamageConfidence(0.9, 0.85, 0.8, 0.85),
                    overallConfidence = 0.85
                )
            ),
            Capture(
                id = "cap-note-1",
                jobId = "JOB-100",
                type = CaptureType.NOTE,
                extractionResult = NoteExtraction(
                    transcribedNote = "Valve turned off.",
                    category = "homeowner_statement",
                    keyFacts = listOf("Valve off"),
                    confidence = NoteConfidence(0.9, 0.9, 0.0),
                    overallConfidence = 0.9
                )
            )
        )

        val record = builder.execute(
            jobId = "JOB-100",
            technicianName = "Alex",
            jobAddress = "100 Main St",
            captures = captures,
            jobStartTime = 1_700_000_000_000L,
            jobEndTime = 1_700_000_600_000L,
            rejectedCaptures = 1
        )

        assertEquals(1, record.equipment.size)
        assertEquals(1, record.damageAssessments.size)
        assertEquals(1, record.notes.size)
        assertEquals(1, record.lineItems.size)
        assertEquals("cap-dmg-1", record.lineItems[0].sourceCaptureId)
        assertEquals(70.0, record.summary.estimateSubtotal, 0.01)
        assertEquals(1, record.summary.rejectedCaptures)
        assertEquals(600L, record.summary.durationSeconds)
    }

    @Test
    fun `SampleDataGenerator creates and exports 3 valid sample records`() {
        val context = RuntimeEnvironment.getApplication()
        val generator = SampleDataGenerator(context)
        val exporter = JsonExporter(context)

        val files = generator.generateAll()
        assertEquals(3, files.size)
        files.forEach { file ->
            assertTrue(file.exists())
            val loaded = exporter.loadJobRecord(file)
            assertNotNull(loaded)
            assertEquals("1.0.0", loaded?.schemaVersion)
            assertTrue((loaded?.lineItems?.size ?: 0) >= 5)
        }

        // Also validate deliverable files in samples/ and eval/sample_outputs/
        listOf("../samples", "../eval/sample_outputs").forEach { relDir ->
            val dir = File(relDir)
            if (dir.exists()) {
                (1..3).forEach { idx ->
                    val sampleFile = File(dir, "job_record_$idx.json")
                    assertTrue("${sampleFile.path} should exist", sampleFile.exists())
                    val loaded = exporter.loadJobRecord(sampleFile)
                    assertNotNull(loaded)
                    assertEquals("1.0.0", loaded?.schemaVersion)
                }
            }
        }
    }
}
