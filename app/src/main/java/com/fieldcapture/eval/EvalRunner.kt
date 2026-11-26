package com.fieldcapture.eval

import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.extraction.DamageExtraction
import com.fieldcapture.extraction.ExtractionMetadata
import com.fieldcapture.extraction.ExtractionResult
import com.fieldcapture.extraction.ExtractionService
import com.fieldcapture.extraction.NameplateExtraction
import com.fieldcapture.extraction.NoteExtraction
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.io.File

/**
 * Evaluation runner that measures extraction accuracy against ground truth.
 *
 * Usage:
 * - For full evaluation: place real images in eval/test_set/ matching filenames
 *   in ground_truth.json, then run this via instrumented or unit test.
 * - For pipeline verification: use [ProgrammaticTestImageGenerator] images.
 *
 * Metrics computed:
 * - Per-field accuracy (exact match and fuzzy match)
 * - Confidence calibration (does confidence predict accuracy?)
 * - Line item precision, recall, and F1 score
 * - Overall accuracy by difficulty level
 * - Top 5 worst failures with root cause summary
 */
class EvalRunner(
    private val groundTruthPath: String = "eval/ground_truth.json",
    private val extractor: suspend (ByteArray?, CaptureType, ExtractionMetadata) -> Result<ExtractionResult>
) {
    constructor(
        extractionService: ExtractionService,
        groundTruthPath: String = "eval/ground_truth.json"
    ) : this(groundTruthPath, extractionService::extract)

    private val gson = Gson()

    data class GroundTruthDataset(
        @SerializedName("schema_version") val schemaVersion: String = "1.0.0",
        val description: String = "",
        @SerializedName("created_at") val createdAt: String = "",
        @SerializedName("test_frames") val testFrames: List<TestFrameEntry> = emptyList()
    )

    data class TestFrameEntry(
        val id: String,
        val filename: String,
        val type: String,
        val description: String = "",
        val difficulty: String? = null,
        val metadata: FrameMetadata? = null,
        @SerializedName("ground_truth") val groundTruth: JsonObject = JsonObject()
    )

    data class FrameMetadata(
        val room: String? = null,
        val surface: String? = null,
        @SerializedName("spoken_text") val spokenText: String? = null
    )

    data class FieldResult(
        val fieldName: String,
        val expected: String?,
        val actual: String?,
        val exactMatch: Boolean,
        val fuzzyMatch: Boolean,
        val confidence: Double,
        val captureType: String
    )

    data class LineItemResult(
        val testId: String,
        val expectedCodes: List<String>,
        val actualCodes: List<String>,
        val recall: Double,
        val precision: Double,
        val f1Score: Double
    )

    data class EvalSummary(
        val totalFrames: Int,
        val successfulExtractions: Int,
        val failedExtractions: Int,
        val fieldResults: List<FieldResult>,
        val lineItemResults: List<LineItemResult>,
        val nameplateFieldAccuracy: Map<String, Double>,
        val damageFieldAccuracy: Map<String, Double>,
        val noteFieldAccuracy: Map<String, Double>,
        val confidenceCalibration: List<CalibrationBucket>,
        val accuracyByDifficulty: Map<String, Double>,
        val worstFailures: List<FailureAnalysis>
    )

    data class CalibrationBucket(
        val confidenceRange: String,
        val predictedAccuracy: Double,
        val actualAccuracy: Double,
        val count: Int
    )

    data class FailureAnalysis(
        val testId: String,
        val captureType: String,
        val difficulty: String,
        val description: String,
        val fieldName: String,
        val expected: String?,
        val actual: String?,
        val confidence: Double,
        val rootCause: String
    )

    /**
     * Load [groundTruthPath] and run evaluation across all annotated test frames.
     */
    suspend fun runEvaluation(imageLoader: (String) -> ByteArray?): EvalSummary {
        val json = loadGroundTruthJson()
        return runEvaluationFromJson(json, imageLoader)
    }

    /**
     * Run evaluation directly from a ground-truth JSON string (useful for standalone/test runs).
     */
    suspend fun runEvaluationFromJson(
        groundTruthJson: String,
        imageLoader: (String) -> ByteArray?
    ): EvalSummary {
        val dataset = gson.fromJson(groundTruthJson, GroundTruthDataset::class.java)
            ?: throw IllegalArgumentException("Unable to parse ground_truth.json")

        val allFieldResults = mutableListOf<FieldResult>()
        val resultsByDifficulty = mutableMapOf<String, MutableList<FieldResult>>()
        val lineItemResults = mutableListOf<LineItemResult>()
        val scoredFailures = mutableListOf<Pair<Double, FailureAnalysis>>()

        var successfulExtractions = 0
        var failedExtractions = 0

        for (entry in dataset.testFrames) {
            val captureType = CaptureType.valueOf(entry.type.uppercase())
            val difficulty = entry.difficulty?.lowercase() ?: "medium"
            val imageBytes = imageLoader(entry.filename)

            if (imageBytes == null && captureType != CaptureType.NOTE) {
                failedExtractions++
                scoredFailures += 1.0 to FailureAnalysis(
                    testId = entry.id,
                    captureType = captureType.name,
                    difficulty = difficulty,
                    description = entry.description,
                    fieldName = "image_load",
                    expected = entry.filename,
                    actual = null,
                    confidence = 0.0,
                    rootCause = "Test image missing or unreadable for ${entry.filename}"
                )
                continue
            }

            val metadata = ExtractionMetadata(
                room = entry.metadata?.room,
                surface = entry.metadata?.surface,
                spokenText = entry.metadata?.spokenText
            )

            val extractionResult = extractor(imageBytes, captureType, metadata)
            if (extractionResult.isFailure) {
                failedExtractions++
                val errorMessage = extractionResult.exceptionOrNull()?.message ?: "Extraction error"
                scoredFailures += 1.0 to FailureAnalysis(
                    testId = entry.id,
                    captureType = captureType.name,
                    difficulty = difficulty,
                    description = entry.description,
                    fieldName = "extraction",
                    expected = "valid ${captureType.name} extraction",
                    actual = errorMessage,
                    confidence = 0.0,
                    rootCause = "Extraction pipeline error: $errorMessage"
                )
                continue
            }

            val extracted = extractionResult.getOrThrow()
            val entryError = extractErrorField(extracted)
            if (entryError != null) {
                failedExtractions++
                scoredFailures += 1.0 to FailureAnalysis(
                    testId = entry.id,
                    captureType = captureType.name,
                    difficulty = difficulty,
                    description = entry.description,
                    fieldName = "extraction_response",
                    expected = "non-error response",
                    actual = entryError,
                    confidence = extracted.overallConfidence,
                    rootCause = "Model returned extraction error: $entryError"
                )
                continue
            }

            successfulExtractions++
            val entryFieldResults = mutableListOf<FieldResult>()

            when (extracted) {
                is NameplateExtraction -> evaluateNameplate(entry, extracted, entryFieldResults)
                is DamageExtraction -> {
                    evaluateDamage(entry, extracted, entryFieldResults)
                    val expectedCodes = entry.groundTruth.stringList("expected_line_items")
                    val actualCodes = extracted.recommendedLineItems.map { it.code }.filter { it.isNotBlank() }
                    val (recall, precision, f1) = computeLineItemMetrics(expectedCodes, actualCodes)
                    lineItemResults += LineItemResult(
                        testId = entry.id,
                        expectedCodes = expectedCodes,
                        actualCodes = actualCodes,
                        recall = recall,
                        precision = precision,
                        f1Score = f1
                    )
                    if (recall < 1.0) {
                        val missing = expectedCodes.filter { it !in actualCodes }
                        scoredFailures += (1.0 - f1 + extracted.confidence.lineItems * 0.25) to FailureAnalysis(
                            testId = entry.id,
                            captureType = captureType.name,
                            difficulty = difficulty,
                            description = entry.description,
                            fieldName = "recommended_line_items",
                            expected = expectedCodes.joinToString(", "),
                            actual = actualCodes.joinToString(", "),
                            confidence = extracted.confidence.lineItems,
                            rootCause = "Missed expected line item(s): ${missing.joinToString(", ")}"
                        )
                    }
                }
                is NoteExtraction -> evaluateNote(entry, extracted, entryFieldResults)
            }

            allFieldResults += entryFieldResults
            resultsByDifficulty.getOrPut(difficulty) { mutableListOf() } += entryFieldResults

            for (fieldResult in entryFieldResults) {
                if (!fieldResult.fuzzyMatch) {
                    val severityScore = 1.0 + fieldResult.confidence
                    scoredFailures += severityScore to FailureAnalysis(
                        testId = entry.id,
                        captureType = captureType.name,
                        difficulty = difficulty,
                        description = entry.description,
                        fieldName = fieldResult.fieldName,
                        expected = fieldResult.expected,
                        actual = fieldResult.actual,
                        confidence = fieldResult.confidence,
                        rootCause = inferRootCause(entry, fieldResult)
                    )
                }
            }
        }

        val nameplateResults = allFieldResults.filter { it.captureType == CaptureType.NAMEPLATE.name }
        val nameplateFields = listOf(
            "equipment_type", "manufacturer", "model_number",
            "serial_number", "manufacture_date", "capacity"
        )
        val nameplateFieldAccuracy = nameplateFields.associateWith {
            computeFieldAccuracy(nameplateResults, it)
        }

        val damageResults = allFieldResults.filter { it.captureType == CaptureType.DAMAGE.name }
        val damageFields = listOf("damage_type", "severity", "affected_materials")
        val damageFieldAccuracy = damageFields.associateWith {
            computeFieldAccuracy(damageResults, it)
        }

        val noteResults = allFieldResults.filter { it.captureType == CaptureType.NOTE.name }
        val noteFields = listOf("category", "key_facts")
        val noteFieldAccuracy = noteFields.associateWith {
            computeFieldAccuracy(noteResults, it)
        }

        val calibrationBuckets = buildCalibrationBuckets(allFieldResults)
        val accuracyByDifficulty = resultsByDifficulty.mapValues { (_, list) ->
            if (list.isEmpty()) 0.0 else list.count { it.fuzzyMatch }.toDouble() / list.size
        }

        val worstFailures = scoredFailures
            .sortedByDescending { it.first }
            .map { it.second }
            .distinctBy { "${it.testId}:${it.fieldName}" }
            .take(5)

        return EvalSummary(
            totalFrames = dataset.testFrames.size,
            successfulExtractions = successfulExtractions,
            failedExtractions = failedExtractions,
            fieldResults = allFieldResults,
            lineItemResults = lineItemResults,
            nameplateFieldAccuracy = nameplateFieldAccuracy,
            damageFieldAccuracy = damageFieldAccuracy,
            noteFieldAccuracy = noteFieldAccuracy,
            confidenceCalibration = calibrationBuckets,
            accuracyByDifficulty = accuracyByDifficulty,
            worstFailures = worstFailures
        )
    }

    /**
     * Compute per-field accuracy (fuzzy match rate) from field results.
     */
    fun computeFieldAccuracy(results: List<FieldResult>, fieldName: String): Double {
        val relevant = results.filter { it.fieldName == fieldName }
        if (relevant.isEmpty()) return 0.0
        return relevant.count { it.fuzzyMatch }.toDouble() / relevant.size
    }

    /**
     * Compute line item recall, precision, and F1 score.
     */
    fun computeLineItemMetrics(expected: List<String>, actual: List<String>): Triple<Double, Double, Double> {
        if (expected.isEmpty() && actual.isEmpty()) return Triple(1.0, 1.0, 1.0)
        if (expected.isEmpty()) return Triple(1.0, 0.0, 0.0)
        if (actual.isEmpty()) return Triple(0.0, 0.0, 0.0)

        val normalizedExpected = expected.map { it.trim().uppercase() }.distinct()
        val normalizedActual = actual.map { it.trim().uppercase() }.distinct()
        val tp = normalizedActual.count { it in normalizedExpected }
        val recall = tp.toDouble() / normalizedExpected.size
        val precision = tp.toDouble() / normalizedActual.size
        val f1 = if (recall + precision > 0) 2 * recall * precision / (recall + precision) else 0.0

        return Triple(recall, precision, f1)
    }

    /**
     * String comparison with fuzzy matching for model/serial numbers.
     * Handles: case differences, extra whitespace, partial matches with ? wildcards,
     * and single-character OCR substitutions on strings longer than 5 characters.
     */
    fun fuzzyMatch(expected: String?, actual: String?): Boolean {
        if (expected == null && actual == null) return true
        if (expected == null || actual == null) return false

        val e = expected.trim().uppercase().replace("\\s+".toRegex(), " ")
        val a = actual.trim().uppercase().replace("\\s+".toRegex(), " ")

        if (e == a) return true

        if (a.contains("?")) {
            val regex = buildString {
                append('^')
                for (ch in a) {
                    if (ch == '?') append('.') else append(Regex.escape(ch.toString()))
                }
                append('$')
            }.toRegex()
            if (regex.matches(e)) return true
        }

        if (e.length > 5 && a.length == e.length) {
            val diffs = e.zip(a).count { (c1, c2) -> c1 != c2 }
            if (diffs <= 1) return true
        }

        return false
    }

    private fun evaluateNameplate(
        entry: TestFrameEntry,
        extracted: NameplateExtraction,
        out: MutableList<FieldResult>
    ) {
        val gt = entry.groundTruth
        out += compareScalarField(
            fieldName = "equipment_type",
            expected = gt.nullableString("equipment_type"),
            actual = extracted.equipmentType,
            confidence = extracted.confidence.equipmentType,
            captureType = CaptureType.NAMEPLATE.name
        )
        out += compareScalarField(
            fieldName = "manufacturer",
            expected = gt.nullableString("manufacturer"),
            actual = extracted.manufacturer,
            confidence = extracted.confidence.manufacturer,
            captureType = CaptureType.NAMEPLATE.name
        )
        out += compareScalarField(
            fieldName = "model_number",
            expected = gt.nullableString("model_number"),
            actual = extracted.modelNumber,
            confidence = extracted.confidence.modelNumber,
            captureType = CaptureType.NAMEPLATE.name
        )
        out += compareScalarField(
            fieldName = "serial_number",
            expected = gt.nullableString("serial_number"),
            actual = extracted.serialNumber,
            confidence = extracted.confidence.serialNumber,
            captureType = CaptureType.NAMEPLATE.name
        )
        out += compareScalarField(
            fieldName = "manufacture_date",
            expected = gt.nullableString("manufacture_date"),
            actual = extracted.manufactureDate,
            confidence = extracted.confidence.manufactureDate,
            captureType = CaptureType.NAMEPLATE.name
        )
        out += compareScalarField(
            fieldName = "capacity",
            expected = gt.nullableString("capacity"),
            actual = extracted.capacity,
            confidence = extracted.confidence.capacity,
            captureType = CaptureType.NAMEPLATE.name
        )
    }

    private fun evaluateDamage(
        entry: TestFrameEntry,
        extracted: DamageExtraction,
        out: MutableList<FieldResult>
    ) {
        val gt = entry.groundTruth
        out += compareScalarField(
            fieldName = "damage_type",
            expected = gt.nullableString("damage_type"),
            actual = extracted.damageType,
            confidence = extracted.confidence.damageType,
            captureType = CaptureType.DAMAGE.name
        )
        out += compareScalarField(
            fieldName = "severity",
            expected = gt.nullableString("severity"),
            actual = extracted.severity,
            confidence = extracted.confidence.severity,
            captureType = CaptureType.DAMAGE.name
        )

        val expectedMaterials = gt.stringList("affected_materials")
        val actualMaterials = extracted.affectedMaterials
        val anyMaterialMatch = expectedMaterials.any { exp ->
            actualMaterials.any { act -> fuzzyMatch(exp, act) || act.contains(exp, ignoreCase = true) || exp.contains(act, ignoreCase = true) }
        }
        val exactMaterialMatch = expectedMaterials.map { it.trim().lowercase() }.toSet() ==
            actualMaterials.map { it.trim().lowercase() }.toSet()

        out += FieldResult(
            fieldName = "affected_materials",
            expected = expectedMaterials.joinToString(", "),
            actual = actualMaterials.joinToString(", "),
            exactMatch = exactMaterialMatch,
            fuzzyMatch = anyMaterialMatch,
            confidence = extracted.confidence.affectedMaterials,
            captureType = CaptureType.DAMAGE.name
        )
    }

    private fun evaluateNote(
        entry: TestFrameEntry,
        extracted: NoteExtraction,
        out: MutableList<FieldResult>
    ) {
        val gt = entry.groundTruth
        out += compareScalarField(
            fieldName = "category",
            expected = gt.nullableString("category"),
            actual = extracted.category,
            confidence = extracted.confidence.category,
            captureType = CaptureType.NOTE.name
        )

        val expectedFacts = gt.stringList("key_facts")
        val actualFacts = extracted.keyFacts
        val anyFactMatch = expectedFacts.any { exp ->
            actualFacts.any { act ->
                fuzzyMatch(exp, act) || act.contains(exp, ignoreCase = true) || exp.contains(act, ignoreCase = true)
            }
        }
        val exactFacts = expectedFacts.map { it.trim().lowercase() }.toSet() ==
            actualFacts.map { it.trim().lowercase() }.toSet()

        out += FieldResult(
            fieldName = "key_facts",
            expected = expectedFacts.joinToString("; "),
            actual = actualFacts.joinToString("; "),
            exactMatch = exactFacts,
            fuzzyMatch = anyFactMatch,
            confidence = extracted.confidence.transcription,
            captureType = CaptureType.NOTE.name
        )
    }

    private fun compareScalarField(
        fieldName: String,
        expected: String?,
        actual: String?,
        confidence: Double,
        captureType: String
    ): FieldResult {
        val exact = if (expected == null && actual == null) {
            true
        } else if (expected == null || actual == null) {
            false
        } else {
            expected.trim().equals(actual.trim(), ignoreCase = true)
        }
        return FieldResult(
            fieldName = fieldName,
            expected = expected,
            actual = actual,
            exactMatch = exact,
            fuzzyMatch = fuzzyMatch(expected, actual),
            confidence = confidence,
            captureType = captureType
        )
    }

    private fun buildCalibrationBuckets(results: List<FieldResult>): List<CalibrationBucket> {
        val specs = listOf(
            "0.9 - 1.0" to { c: Double -> c >= 0.9 },
            "0.8 - 0.9" to { c: Double -> c >= 0.8 && c < 0.9 },
            "0.7 - 0.8" to { c: Double -> c >= 0.7 && c < 0.8 },
            "0.6 - 0.7" to { c: Double -> c >= 0.6 && c < 0.7 },
            "0.5 - 0.6" to { c: Double -> c >= 0.5 && c < 0.6 },
            "< 0.5" to { c: Double -> c < 0.5 }
        )
        return specs.map { (label, predicate) ->
            val inBucket = results.filter { predicate(it.confidence) }
            if (inBucket.isEmpty()) {
                CalibrationBucket(
                    confidenceRange = label,
                    predictedAccuracy = 0.0,
                    actualAccuracy = 0.0,
                    count = 0
                )
            } else {
                CalibrationBucket(
                    confidenceRange = label,
                    predictedAccuracy = inBucket.map { it.confidence }.average(),
                    actualAccuracy = inBucket.count { it.fuzzyMatch }.toDouble() / inBucket.size,
                    count = inBucket.size
                )
            }
        }
    }

    private fun extractErrorField(result: ExtractionResult): String? = when (result) {
        is NameplateExtraction -> result.error
        is DamageExtraction -> result.error
        is NoteExtraction -> result.error
    }

    private fun inferRootCause(entry: TestFrameEntry, result: FieldResult): String {
        val descLower = entry.description.lowercase()
        return when {
            result.actual == null -> "Model returned null for ${result.fieldName} (${entry.description})"
            descLower.contains("blur") || descLower.contains("dim") || descLower.contains("flashlight") ->
                "Degraded image readability (lighting/blur): ${entry.description}"
            descLower.contains("weathered") || descLower.contains("faded") || descLower.contains("sun-bleached") ->
                "Label degradation / low character contrast: ${entry.description}"
            descLower.contains("wide-angle") ->
                "Wide-angle perspective obscured damage scale and spatial layout"
            else ->
                "Mismatch on ${result.fieldName}: expected '${result.expected}', got '${result.actual}' (confidence=${result.confidence})"
        }
    }

    private fun loadGroundTruthJson(): String {
        val candidates = listOf(File(groundTruthPath), File("../$groundTruthPath"))
        val resolved = candidates.firstOrNull { it.exists() && it.isFile }
            ?: throw IllegalStateException(
                "Ground truth file not found at $groundTruthPath (checked ${candidates.joinToString { it.path }})"
            )
        return resolved.readText(Charsets.UTF_8)
    }

    private fun JsonObject.nullableString(key: String): String? {
        val el = get(key) ?: return null
        return if (el.isJsonNull) null else el.asString
    }

    private fun JsonObject.stringList(key: String): List<String> {
        val el = get(key) ?: return emptyList()
        if (!el.isJsonArray) return emptyList()
        return (el as JsonArray).mapNotNull { if (it.isJsonNull) null else it.asString }
    }
}
