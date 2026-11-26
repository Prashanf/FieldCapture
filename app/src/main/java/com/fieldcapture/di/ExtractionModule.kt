/*
 * Copyright (c) 2026 Prashant Sharma. All Rights Reserved.
 * This code is submitted as part of a technical assessment and may not be
 * used, copied, or distributed without explicit written permission.
 */
package com.fieldcapture.di

import android.util.Log
import com.fieldcapture.data.local.ApiKeyManager
import com.fieldcapture.extraction.ExtractionService
import com.fieldcapture.network.GeminiApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExtractionModule {

    @Provides
    @Singleton
    fun provideGeminiApiClient(
        apiKeyManager: ApiKeyManager
    ): GeminiApiClient {
        val keys = apiKeyManager.getAllAvailableKeys()
        Log.i("ExtractionModule", "GeminiApiClient initialized via ApiKeyManager (${keys.size} key(s))")
        return GeminiApiClient(apiKeyManager = apiKeyManager)
    }

    @Provides
    @Singleton
    fun provideExtractionService(
        geminiApiClient: GeminiApiClient
    ): ExtractionService = ExtractionService(geminiApiClient)
}
