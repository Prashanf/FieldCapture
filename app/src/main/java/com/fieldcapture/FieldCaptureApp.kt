package com.fieldcapture

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fieldcapture.glasses.GlassesSessionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point for FieldCapture, enabling Hilt DI, DAT SDK init, and WorkManager.
 */
@HiltAndroidApp
class FieldCaptureApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var glassesSessionManager: GlassesSessionManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        glassesSessionManager.initialize()
    }
}
