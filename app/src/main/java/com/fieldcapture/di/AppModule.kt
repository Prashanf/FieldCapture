package com.fieldcapture.di

import android.content.Context
import androidx.room.Room
import com.fieldcapture.data.local.FieldCaptureDatabase
import com.fieldcapture.data.local.dao.CaptureDao
import com.fieldcapture.data.local.dao.JobRecordDao
import com.fieldcapture.data.remote.GeminiApiService
import com.fieldcapture.util.Constants
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FieldCaptureDatabase {
        return Room.databaseBuilder(
            context,
            FieldCaptureDatabase::class.java,
            "fieldcapture.db"
        ).build()
    }

    @Provides
    fun provideCaptureDao(db: FieldCaptureDatabase): CaptureDao = db.captureDao()

    @Provides
    fun provideJobRecordDao(db: FieldCaptureDatabase): JobRecordDao = db.jobRecordDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): GeminiApiService {
        return Retrofit.Builder()
            .baseUrl(Constants.GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(GeminiApiService::class.java)
    }
}
