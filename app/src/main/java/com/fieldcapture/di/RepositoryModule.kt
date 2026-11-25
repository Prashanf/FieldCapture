package com.fieldcapture.di

import com.fieldcapture.data.repository.CaptureRepositoryImpl
import com.fieldcapture.data.repository.ExtractionRepositoryImpl
import com.fieldcapture.data.repository.JobRecordRepositoryImpl
import com.fieldcapture.domain.repository.CaptureRepository
import com.fieldcapture.domain.repository.ExtractionRepository
import com.fieldcapture.domain.repository.JobRecordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCaptureRepository(impl: CaptureRepositoryImpl): CaptureRepository

    @Binds
    @Singleton
    abstract fun bindExtractionRepository(impl: ExtractionRepositoryImpl): ExtractionRepository

    @Binds
    @Singleton
    abstract fun bindJobRecordRepository(impl: JobRecordRepositoryImpl): JobRecordRepository
}
