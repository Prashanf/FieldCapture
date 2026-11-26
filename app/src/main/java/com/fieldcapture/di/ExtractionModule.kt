package com.fieldcapture.di

import android.util.Log
import com.fieldcapture.BuildConfig
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
    fun provideGeminiApiClient(): GeminiApiClient {
        val keys = GeminiApiClient.parseKeys(
            BuildConfig.GEMINI_API_KEYS.ifBlank { BuildConfig.GEMINI_API_KEY }
        )
        Log.i("ExtractionModule", "GeminiApiClient initialized with ${keys.size} API key(s)")
        return GeminiApiClient(apiKeys = keys)
    }

    @Provides
    @Singleton
    fun provideExtractionService(
        geminiApiClient: GeminiApiClient
    ): ExtractionService = ExtractionService(geminiApiClient)
}
