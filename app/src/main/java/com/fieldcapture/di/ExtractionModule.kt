package com.fieldcapture.di

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
    fun provideGeminiApiClient(): GeminiApiClient = GeminiApiClient()

    @Provides
    @Singleton
    fun provideExtractionService(
        geminiApiClient: GeminiApiClient
    ): ExtractionService = ExtractionService(geminiApiClient)
}
