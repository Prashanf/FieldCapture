/*
 * Copyright (c) 2026 Prashant Sharma. All Rights Reserved.
 * This code is submitted as part of a technical assessment and may not be
 * used, copied, or distributed without explicit written permission.
 */
package com.fieldcapture.extraction

import android.util.Log
import com.fieldcapture.domain.model.CaptureType
import com.fieldcapture.network.GeminiApiClient
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractionService @Inject constructor(
    private val geminiApiClient: GeminiApiClient
) {
    private val gson = Gson()

    companion object {
        private const val TAG = "ExtractionService"
    }

    /**
     * Run AI extraction on a captured image or spoken note.
     */
    suspend fun extract(
        imageBytes: ByteArray? = null,
        captureType: CaptureType,
        metadata: ExtractionMetadata
    ): Result<ExtractionResult> {
        val prompt = when (captureType) {
            CaptureType.NAMEPLATE -> ExtractionPrompts.nameplatePrompt()
            CaptureType.DAMAGE -> ExtractionPrompts.damagePrompt(
                room = metadata.room,
                surface = metadata.surface
            )
            CaptureType.NOTE -> ExtractionPrompts.notePrompt(
                spokenText = metadata.spokenText ?: "No spoken note provided"
            )
        }

        Log.d(TAG, "Starting ${captureType.name} extraction")

        return geminiApiClient.analyzeImage(
            imageBytes = imageBytes,
            mimeType = "image/jpeg",
            prompt = prompt
        ).mapCatching { responseJson ->
            Log.d(TAG, "Raw response: $responseJson")
            parseResponse(responseJson, captureType)
        }.onFailure { e ->
            Log.e(TAG, "Extraction failed for ${captureType.name}", e)
        }
    }

    /**
     * Parse the JSON response into the appropriate typed result.
     * Strips markdown fences and extra surrounding text gracefully.
     */
    fun parseResponse(json: String, captureType: CaptureType): ExtractionResult {
        val cleanJson = cleanJsonResponse(json)
        if (cleanJson.isBlank()) {
            return createErrorResult(captureType, json, "Empty response")
        }

        return try {
            when (captureType) {
                CaptureType.NAMEPLATE -> {
                    val result = gson.fromJson(cleanJson, NameplateExtraction::class.java)
                        ?: return createErrorResult(captureType, cleanJson, "Null parsed nameplate")
                    result.copy(rawJson = cleanJson)
                }
                CaptureType.DAMAGE -> {
                    val result = gson.fromJson(cleanJson, DamageExtraction::class.java)
                        ?: return createErrorResult(captureType, cleanJson, "Null parsed damage")
                    result.copy(rawJson = cleanJson)
                }
                CaptureType.NOTE -> {
                    val result = gson.fromJson(cleanJson, NoteExtraction::class.java)
                        ?: return createErrorResult(captureType, cleanJson, "Null parsed note")
                    result.copy(rawJson = cleanJson)
                }
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse $captureType JSON: $cleanJson", e)
            createErrorResult(captureType, cleanJson, e.message ?: "JSON parse error")
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        val stripped = raw
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val firstBrace = stripped.indexOf('{')
        val lastBrace = stripped.lastIndexOf('}')
        return if (firstBrace != -1 && lastBrace > firstBrace) {
            stripped.substring(firstBrace, lastBrace + 1)
        } else {
            stripped
        }
    }

    private fun createErrorResult(
        captureType: CaptureType,
        rawJson: String,
        error: String
    ): ExtractionResult {
        return when (captureType) {
            CaptureType.NAMEPLATE -> NameplateExtraction(
                notes = "Extraction failed: $error",
                error = "parse_error",
                rawJson = rawJson
            )
            CaptureType.DAMAGE -> DamageExtraction(
                description = "Extraction failed: $error",
                notes = "Extraction failed: $error",
                error = "parse_error",
                rawJson = rawJson
            )
            CaptureType.NOTE -> NoteExtraction(
                error = "parse_error",
                rawJson = rawJson
            )
        }
    }
}

data class ExtractionMetadata(
    val room: String? = null,
    val surface: String? = null,
    val spokenText: String? = null,
    val captureTimestamp: Long = System.currentTimeMillis()
)
