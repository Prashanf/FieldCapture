package com.fieldcapture

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point for FieldCapture, enabling Hilt DI and custom WorkManager initialization.
 */
@HiltAndroidApp
class FieldCaptureApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // TODO: Initialize Mock Device Kit / Meta DAT SDK lifecycle hooks and sync scheduler if needed
    }
}
