package com.fieldcapture.eval

import android.app.Application
import android.graphics.BitmapFactory
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.extraction.DamageConfidence
import com.fieldcapture.extraction.DamageExtraction
import com.fieldcapture.extraction.ExtractionService
import com.fieldcapture.extraction.NameplateConfidence
import com.fieldcapture.extraction.NameplateExtraction
import com.fieldcapture.extraction.NoteConfidence
import com.fieldcapture.extraction.NoteExtraction
import com.fieldcapture.extraction.RecommendedLineItem
import com.fieldcapture.network.GeminiApiClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EvalRunnerTest {

    private val evalRunner = EvalRunner(
        extractionService = ExtractionService(GeminiApiClient()),
        groundTruthPath = "eval/ground_truth.json"
    )

    @Test
    fun `fuzzy match handles exact match`() {
        assertTrue(evalRunner.fuzzyMatch("Rheem", "Rheem"))
    }

    @Test
    fun `fuzzy match handles case differences`() {
        assertTrue(evalRunner.fuzzyMatch("RHEEM", "rheem"))
        assertTrue(evalRunner.fuzzyMatch("Water Heater", "water heater"))
    }

    @Test
    fun `fuzzy match handles question mark wildcards`() {
        assertTrue(evalRunner.fuzzyMatch("MP1234567890", "MP12??567890"))
        assertTrue(evalRunner.fuzzyMatch("0B4N5CDJ200481", "0B4N5CDJ200??1"))
    }

    @Test
    fun `fuzzy match handles 1 char OCR error`() {
        assertTrue(evalRunner.fuzzyMatch("XG40T06EC36U1", "XG40T06EC36UI"))
    }

    @Test
    fun `fuzzy match rejects completely different strings`() {
        assertFalse(evalRunner.fuzzyMatch("Rheem", "Carrier"))
        assertFalse(evalRunner.fuzzyMatch("XG40T06EC36U1", "59SC5A080S17"))
    }

    @Test
    fun `fuzzy match handles nulls`() {
        assertTrue(evalRunner.fuzzyMatch(null, null))
        assertFalse(evalRunner.fuzzyMatch("Rheem", null))
        assertFalse(evalRunner.fuzzyMatch(null, "Rheem"))
    }

    @Test
    fun `line item recall calculation`() {
        val (recall, precision, f1) = evalRunner.computeLineItemMetrics(
            expected = listOf("REMOVE_REPLACE_DRYWALL", "REMOVE_REPLACE_BASEBOARD", "PAINT_PRIME_WALLS"),
            actual = listOf("REMOVE_REPLACE_DRYWALL", "REMOVE_REPLACE_BASEBOARD", "STRUCTURAL_DRY_OUT")
        )
        assertEquals(0.667, recall, 0.01)
        assertEquals(0.667, precision, 0.01)
        assertEquals(0.667, f1, 0.01)
    }

    @Test
    fun `line item perfect match`() {
        val (recall, precision, f1) = evalRunner.computeLineItemMetrics(
            expected = listOf("WATER_EXTRACTION", "REMOVE_REPLACE_FLOORING"),
            actual = listOf("WATER_EXTRACTION", "REMOVE_REPLACE_FLOORING")
        )
        assertEquals(1.0, recall, 0.01)
        assertEquals(1.0, precision, 0.01)
        assertEquals(1.0, f1, 0.01)
    }

    @Test
    fun `line item empty lists`() {
        val (recall, precision, f1) = evalRunner.computeLineItemMetrics(
            expected = emptyList(),
            actual = emptyList()
        )
        assertEquals(1.0, recall, 0.01)
        assertEquals(1.0, precision, 0.01)
        assertEquals(1.0, f1, 0.01)
    }

    @Test
    fun `compute field accuracy calculates fuzzy pass ratio`() {
        val results = listOf(
            EvalRunner.FieldResult("serial_number", "ABC1234", "ABC1234", true, true, 0.9, "NAMEPLATE"),
            EvalRunner.FieldResult("serial_number", "XYZ9876", "XYZ9875", false, true, 0.7, "NAMEPLATE"),
            EvalRunner.FieldResult("serial_number", "MNO5555", "WRONG", false, false, 0.6, "NAMEPLATE"),
            EvalRunner.FieldResult("model_number", "MOD1", "MOD1", true, true, 0.95, "NAMEPLATE")
        )
        assertEquals(2.0 / 3.0, evalRunner.computeFieldAccuracy(results, "serial_number"), 0.001)
        assertEquals(1.0, evalRunner.computeFieldAccuracy(results, "model_number"), 0.001)
        assertEquals(0.0, evalRunner.computeFieldAccuracy(results, "missing_field"), 0.001)
    }

    @Test
    fun `programmatic test images generate valid JPEGs`() {
        val sampleSet = ProgrammaticTestImageGenerator.generateSamplePipelineSet()
        assertEquals(3, sampleSet.size)

        for ((filename, bytes) in sampleSet) {
            assertTrue("Expected non-empty JPEG for $filename", bytes.size > 1000)
            assertEquals(0xFF.toByte(), bytes[0])
            assertEquals(0xD8.toByte(), bytes[1])

            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertNotNull("Decoded bitmap null for $filename", decoded)
            assertEquals(640, decoded.width)
            assertEquals(480, decoded.height)
        }
    }

    @Test
    fun `runEvaluation processes all 40 ground truth entries with pipeline runner`() = runTest {
        val runner = EvalRunner(groundTruthPath = "eval/ground_truth.json") { _, captureType, metadata ->
            when (captureType) {
                CaptureType.NAMEPLATE -> Result.success(
                    NameplateExtraction(
                        equipmentType = "Water Heater",
                        manufacturer = "Rheem",
                        modelNumber = "XG40T06EC36U1",
                        serialNumber = "WRONG_SERIAL",
                        manufactureDate = "2019",
                        capacity = "40 GAL",
                        confidence = NameplateConfidence(
                            equipmentType = 0.94,
                            manufacturer = 0.91,
                            modelNumber = 0.82,
                            serialNumber = 0.65,
                            manufactureDate = 0.85,
                            capacity = 0.88
                        ),
                        overallConfidence = 0.84
                    )
                )
                CaptureType.DAMAGE -> Result.success(
                    DamageExtraction(
                        damageType = "water_damage",
                        severity = "medium",
                        affectedMaterials = listOf("drywall", "baseboard"),
                        room = metadata.room,
                        surface = metadata.surface,
                        recommendedLineItems = listOf(
                            RecommendedLineItem(code = "REMOVE_REPLACE_DRYWALL", reason = "Wet drywall"),
                            RecommendedLineItem(code = "REMOVE_REPLACE_BASEBOARD", reason = "Swollen trim"),
                            RecommendedLineItem(code = "PAINT_PRIME_WALLS", reason = "Seal wall")
                        ),
                        confidence = DamageConfidence(
                            damageType = 0.92,
                            severity = 0.78,
                            affectedMaterials = 0.85,
                            lineItems = 0.76
                        ),
                        overallConfidence = 0.83
                    )
                )
                CaptureType.NOTE -> Result.success(
                    NoteExtraction(
                        transcribedNote = metadata.spokenText ?: "",
                        category = "observation",
                        keyFacts = listOf("musty odor", "main shutoff under kitchen sink"),
                        confidence = NoteConfidence(
                            transcription = 0.89,
                            category = 0.84,
                            imageContext = 0.81
                        ),
                        overallConfidence = 0.85
                    )
                )
            }
        }

        val sampleImages = ProgrammaticTestImageGenerator.generateSamplePipelineSet()
        val fallbackDamage = ProgrammaticTestImageGenerator.generateDamageJpeg()
        val fallbackNameplate = ProgrammaticTestImageGenerator.generateNameplateJpeg()

        val summary = runner.runEvaluation { filename ->
            sampleImages[filename] ?: when {
                filename.startsWith("nameplate_") -> fallbackNameplate
                filename.startsWith("damage_") -> fallbackDamage
                else -> ProgrammaticTestImageGenerator.generateNoteContextJpeg()
            }
        }

        assertEquals(40, summary.totalFrames)
        assertEquals(40, summary.successfulExtractions)
        assertEquals(0, summary.failedExtractions)
        assertEquals(15, summary.lineItemResults.size)
        assertEquals(5, summary.worstFailures.size)
        assertEquals(6, summary.confidenceCalibration.size)
        assertTrue(summary.accuracyByDifficulty.containsKey("easy"))
        assertTrue(summary.accuracyByDifficulty.containsKey("medium"))
        assertTrue(summary.accuracyByDifficulty.containsKey("hard"))
    }
}
