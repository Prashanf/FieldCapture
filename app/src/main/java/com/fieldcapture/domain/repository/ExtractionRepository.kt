package com.fieldcapture.domain.repository

import android.graphics.Bitmap
import com.fieldcapture.domain.model.ExtractionResult

interface ExtractionRepository {
    suspend fun extractNameplate(bitmap: Bitmap): Result<ExtractionResult.NameplateData>
    suspend fun extractDamage(bitmap: Bitmap): Result<ExtractionResult.DamageData>
    suspend fun parseNote(rawText: String): Result<ExtractionResult.NoteData>
}
