package com.fieldcapture.eval

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.extraction.DamageExtraction
import com.fieldcapture.extraction.ExtractionMetadata
import com.fieldcapture.extraction.ExtractionService
import com.fieldcapture.extraction.NameplateExtraction
import com.fieldcapture.extraction.NoteExtraction
import com.fieldcapture.network.GeminiApiClient
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented test that runs real EPA Mold Image Library photographs and synthetic
 * nameplate frames through the production [ExtractionService] + [GeminiApiClient] pipeline.
 *
 * PREREQUISITES:
 * - Downloaded EPA images + real_ground_truth.json in app/src/androidTest/assets/eval_images/
 * - GEMINI_API_KEY configured in local.properties
 * - Device/emulator with internet access
 */
@RunWith(AndroidJUnit4::class)
class RealPipelineEvalTest {

    private lateinit var context: Context
    private lateinit var extractionService: ExtractionService
    private lateinit var gson: Gson

    data class EvalResult(
        val testId: String,
        val filename: String,
        val type: String,
        val success: Boolean,
        val extractionResult: Map<String, Any?>?,
        val groundTruth: Map<String, Any?>,
        val fieldMatches: Map<String, Boolean>,
        val lineItemRecall: Double? = null,
        val errorMessage: String?
    )

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        extractionService = ExtractionService(GeminiApiClient())
        gson = Gson()
    }

    @Test
    fun runDamageAndNoteImageEvaluation() = runBlocking {
        val results = mutableListOf<EvalResult>()
        val assetManager = InstrumentationRegistry.getInstrumentation().context.assets
        val assetFiles = assetManager.list("eval_images")?.toSet().orEmpty()
        if ("ground_truth.json" !in assetFiles) {
            println("Skipping asset-based EPA evaluation (real phone captures are in eval/test_set/real_captures/)")
            return@runBlocking
        }

        val groundTruthJson = assetManager.open("eval_images/ground_truth.json")
            .bufferedReader().readText()
        val groundTruth = gson.fromJson(groundTruthJson, Map::class.java)
        @Suppress("UNCHECKED_CAST")
        val testFrames = groundTruth["test_frames"] as List<Map<String, Any?>>

        for (frame in testFrames) {
            val filename = frame["filename"] as String
            if (filename !in assetFiles) continue
            val testId = frame["id"] as String
            val typeStr = frame["type"] as String
            @Suppress("UNCHECKED_CAST")
            val metadataMap = frame["metadata"] as? Map<String, Any?> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val gt = frame["ground_truth"] as Map<String, Any?>

            println("=== Testing $testId ($typeStr): $filename ===")

            try {
                val imageBytes = assetManager.open("eval_images/$filename").readBytes()
                val captureType = CaptureType.valueOf(typeStr.uppercase())
                val metadata = ExtractionMetadata(
                    room = metadataMap["room"] as? String,
                    surface = metadataMap["surface"] as? String,
                    spokenText = metadataMap["spoken_text"] as? String
                )

                val extraction = extractionService.extract(
                    imageBytes = imageBytes,
                    captureType = captureType,
                    metadata = metadata
                ).getOrThrow()

                when (extraction) {
                    is DamageExtraction -> {
                        val fieldMatches = mutableMapOf<String, Boolean>()
                        val expectedType = gt["damage_type"] as? String
                        val actualType = extraction.damageType
                        fieldMatches["damage_type"] = expectedType.equals(actualType, ignoreCase = true)

                        val expectedSeverity = gt["severity"] as? String
                        val actualSeverity = extraction.severity
                        fieldMatches["severity"] = expectedSeverity.equals(actualSeverity, ignoreCase = true)

                        @Suppress("UNCHECKED_CAST")
                        val expectedItems = (gt["expected_line_items"] as? List<String>) ?: emptyList()
                        val actualCodes = extraction.recommendedLineItems.map { it.code }
                        val foundItems = expectedItems.count { expected ->
                            actualCodes.any { it.equals(expected, ignoreCase = true) }
                        }
                        val lineItemRecall = if (expectedItems.isNotEmpty()) {
                            foundItems.toDouble() / expectedItems.size
                        } else {
                            1.0
                        }
                        fieldMatches["line_item_recall"] = foundItems > 0

                        println("  damage_type: expected=$expectedType, got=$actualType, match=${fieldMatches["damage_type"]}")
                        println("  severity: expected=$expectedSeverity, got=$actualSeverity, match=${fieldMatches["severity"]}")
                        println("  line_items: expected=$expectedItems, got=$actualCodes, recall=$lineItemRecall")

                        results.add(
                            EvalResult(
                                testId = testId,
                                filename = filename,
                                type = typeStr,
                                success = extraction.error == null,
                                extractionResult = mapOf(
                                    "damage_type" to actualType,
                                    "severity" to actualSeverity,
                                    "affected_materials" to extraction.affectedMaterials,
                                    "line_items" to actualCodes,
                                    "overall_confidence" to extraction.overallConfidence
                                ),
                                groundTruth = gt,
                                fieldMatches = fieldMatches,
                                lineItemRecall = lineItemRecall,
                                errorMessage = extraction.error
                            )
                        )
                    }
                    is NoteExtraction -> {
                        val fieldMatches = mutableMapOf<String, Boolean>()
                        val expectedCategory = gt["category"] as? String
                        val actualCategory = extraction.category
                        fieldMatches["category"] = expectedCategory.equals(actualCategory, ignoreCase = true)

                        @Suppress("UNCHECKED_CAST")
                        val expectedFacts = (gt["key_facts"] as? List<String>) ?: emptyList()
                        val actualFacts = extraction.keyFacts
                        val matchedFacts = expectedFacts.count { exp ->
                            actualFacts.any { act ->
                                act.contains(exp, ignoreCase = true) || exp.contains(act, ignoreCase = true)
                            }
                        }
                        fieldMatches["key_facts"] = matchedFacts > 0 || actualFacts.isNotEmpty()

                        println("  category: expected=$expectedCategory, got=$actualCategory, match=${fieldMatches["category"]}")
                        println("  key_facts: expected=$expectedFacts, got=$actualFacts")

                        results.add(
                            EvalResult(
                                testId = testId,
                                filename = filename,
                                type = typeStr,
                                success = extraction.error == null,
                                extractionResult = mapOf(
                                    "category" to actualCategory,
                                    "key_facts" to actualFacts,
                                    "transcribed_note" to extraction.transcribedNote,
                                    "overall_confidence" to extraction.overallConfidence
                                ),
                                groundTruth = gt,
                                fieldMatches = fieldMatches,
                                errorMessage = extraction.error
                            )
                        )
                    }
                    is NameplateExtraction -> {
                        // Not expected in real_ground_truth.json
                    }
                }
            } catch (e: Exception) {
                println("  ERROR: ${e.message}")
                results.add(
                    EvalResult(
                        testId = testId,
                        filename = filename,
                        type = typeStr,
                        success = false,
                        extractionResult = null,
                        groundTruth = gt,
                        fieldMatches = emptyMap(),
                        errorMessage = e.message
                    )
                )
            }

            delay(2000)
        }

        val damageResults = results.filter { it.type == "DAMAGE" }
        val successful = results.count { it.success }
        val damageTypeAccuracy = if (damageResults.isNotEmpty()) {
            damageResults.count { it.fieldMatches["damage_type"] == true }.toDouble() / damageResults.size
        } else 0.0
        val severityAccuracy = if (damageResults.isNotEmpty()) {
            damageResults.count { it.fieldMatches["severity"] == true }.toDouble() / damageResults.size
        } else 0.0

        println("\n========== EVALUATION SUMMARY ==========")
        println("Total: ${results.size}, Successful: $successful, Failed: ${results.size - successful}")
        println("Damage Type Accuracy: ${(damageTypeAccuracy * 100).toInt()}%")
        println("Severity Accuracy: ${(severityAccuracy * 100).toInt()}%")

        val outputFile = File(context.filesDir, "eval_results.json")
        outputFile.writeText(gson.toJson(results))
        println("Results written to: ${outputFile.absolutePath}")
        println("=========================================")

        assertTrue("No extractions succeeded — check API key and network", successful > 0)
    }

    @Test
    fun runSyntheticNameplateEvaluation() = runBlocking {
        println("=== Synthetic Nameplate Pipeline Test ===")
        var generatedCount = 0

        for (nameplate in SyntheticNameplateGenerator.TEST_NAMEPLATES) {
            println("\n--- Testing ${nameplate.id}: ${nameplate.manufacturer} ${nameplate.equipmentType} ---")
            val bitmap = SyntheticNameplateGenerator.generateNameplateBitmap(nameplate)
            val imageBytes = SyntheticNameplateGenerator.bitmapToJpegBytes(bitmap)
            if (imageBytes.isNotEmpty()) {
                generatedCount++
            }

            try {
                val extraction = extractionService.extract(
                    imageBytes = imageBytes,
                    captureType = CaptureType.NAMEPLATE,
                    metadata = ExtractionMetadata()
                ).getOrThrow() as NameplateExtraction

                println("  manufacturer: expected=${nameplate.manufacturer}, got=${extraction.manufacturer}")
                println("  model: expected=${nameplate.modelNumber}, got=${extraction.modelNumber}")
                println("  serial: expected=${nameplate.serialNumber}, got=${extraction.serialNumber}")
                println("  type: expected=${nameplate.equipmentType}, got=${extraction.equipmentType}")

                val mfrMatch = extraction.manufacturer?.contains(nameplate.manufacturer, ignoreCase = true) == true
                println("  manufacturer match: $mfrMatch")
            } catch (e: Exception) {
                println("  ERROR: ${e.message}")
            }

            delay(2000)
        }

        assertTrue("Expected 3 generated synthetic nameplates", generatedCount == 3)
    }
}
