package com.fieldcapture.di

import com.fieldcapture.domain.repository.CaptureRepository
import com.fieldcapture.domain.repository.ExtractionRepository
import com.fieldcapture.domain.repository.JobRecordRepository
import com.fieldcapture.domain.usecase.CheckCaptureQualityUseCase
import com.fieldcapture.domain.usecase.ExtractCaptureDataUseCase
import com.fieldcapture.domain.usecase.ExtractDamageUseCase
import com.fieldcapture.domain.usecase.ExtractNameplateUseCase
import com.fieldcapture.domain.usecase.GenerateJobRecordUseCase
import com.fieldcapture.domain.usecase.MapToLineItemsUseCase
import com.fieldcapture.domain.usecase.ParseNoteUseCase
import com.fieldcapture.domain.usecase.ProcessCaptureUseCase
import com.fieldcapture.domain.usecase.SyncCapturesUseCase
import com.fieldcapture.extraction.ExtractionService
import com.fieldcapture.quality.ImageQualityAnalyzer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideCheckCaptureQualityUseCase(
        imageQualityAnalyzer: ImageQualityAnalyzer
    ): CheckCaptureQualityUseCase = CheckCaptureQualityUseCase(imageQualityAnalyzer)

    @Provides
    fun provideExtractCaptureDataUseCase(
        extractionService: ExtractionService
    ): ExtractCaptureDataUseCase = ExtractCaptureDataUseCase(extractionService)

    @Provides
    fun provideExtractNameplateUseCase(
        extractionRepository: ExtractionRepository
    ): ExtractNameplateUseCase = ExtractNameplateUseCase(extractionRepository)

    @Provides
    fun provideExtractDamageUseCase(
        extractionRepository: ExtractionRepository
    ): ExtractDamageUseCase = ExtractDamageUseCase(extractionRepository)

    @Provides
    fun provideParseNoteUseCase(
        extractionRepository: ExtractionRepository
    ): ParseNoteUseCase = ParseNoteUseCase(extractionRepository)

    @Provides
    fun provideMapToLineItemsUseCase(): MapToLineItemsUseCase = MapToLineItemsUseCase()

    @Provides
    fun provideGenerateJobRecordUseCase(
        mapToLineItemsUseCase: MapToLineItemsUseCase,
        jobRecordRepository: JobRecordRepository
    ): GenerateJobRecordUseCase = GenerateJobRecordUseCase(mapToLineItemsUseCase, jobRecordRepository)

    @Provides
    fun provideProcessCaptureUseCase(
        checkCaptureQualityUseCase: CheckCaptureQualityUseCase,
        extractCaptureDataUseCase: ExtractCaptureDataUseCase,
        captureRepository: CaptureRepository
    ): ProcessCaptureUseCase = ProcessCaptureUseCase(
        checkCaptureQualityUseCase = checkCaptureQualityUseCase,
        extractCaptureDataUseCase = extractCaptureDataUseCase,
        captureRepository = captureRepository
    )

    @Provides
    fun provideSyncCapturesUseCase(
        captureRepository: CaptureRepository,
        extractCaptureDataUseCase: ExtractCaptureDataUseCase
    ): SyncCapturesUseCase = SyncCapturesUseCase(
        captureRepository = captureRepository,
        extractCaptureDataUseCase = extractCaptureDataUseCase
    )
}
