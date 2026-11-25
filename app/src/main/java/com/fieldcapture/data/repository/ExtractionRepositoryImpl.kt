package com.fieldcapture.data.repository

import android.graphics.Bitmap
import com.fieldcapture.data.remote.GeminiApiService
import com.fieldcapture.domain.model.ExtractionResult
import com.fieldcapture.domain.model.FieldWithConfidence
import com.fieldcapture.domain.repository.ExtractionRepository
import com.fieldcapture.util.Constants
import com.google.gson.Gson
import javax.inject.Inject

class ExtractionRepositoryImpl @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val gson: Gson
) : ExtractionRepository {

    override suspend fun extractNameplate(bitmap: Bitmap): Result<ExtractionResult.NameplateData> {
        // TODO: Encode bitmap to base64 JPEG, call geminiApiService.generateContent with Constants.NAMEPLATE_PROMPT,
        //       measure processingTimeMs, and parse JSON into ExtractionResult.NameplateData
        return Result.success(
            ExtractionResult.NameplateData(
                make = FieldWithConfidence(null, 0.0),
                model = FieldWithConfidence(null, 0.0),
                serialNumber = FieldWithConfidence(null, 0.0),
                manufactureDate = FieldWithConfidence(null, 0.0),
                rawResponse = "{}",
                modelUsed = Constants.GEMINI_MODEL
            )
        )
    }

    override suspend fun extractDamage(bitmap: Bitmap): Result<ExtractionResult.DamageData> {
        // TODO: Encode bitmap to base64 JPEG, call geminiApiService.generateContent with Constants.DAMAGE_PROMPT,
        //       measure processingTimeMs, and parse JSON into ExtractionResult.DamageData
        return Result.success(
            ExtractionResult.DamageData(
                room = FieldWithConfidence(null, 0.0),
                surface = FieldWithConfidence(null, 0.0),
                damageType = FieldWithConfidence(null, 0.0),
                estimatedSizeSqft = FieldWithConfidence(null, 0.0),
                severity = FieldWithConfidence(null, 0.0),
                rawResponse = "{}",
                modelUsed = Constants.GEMINI_MODEL
            )
        )
    }

    override suspend fun parseNote(rawText: String): Result<ExtractionResult.NoteData> {
        // TODO: Send free-text technician note to Gemini API to extract structured key-value attributes
        return Result.success(
            ExtractionResult.NoteData(
                text = rawText,
                structuredFields = emptyMap(),
                rawResponse = "{}",
                modelUsed = Constants.GEMINI_MODEL
            )
        )
    }
}
