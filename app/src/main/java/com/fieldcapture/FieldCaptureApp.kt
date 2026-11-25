package com.fieldcapture

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fieldcapture.glasses.GlassesSessionManager
import com.fieldcapture.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

/**
 * Application entry point for FieldCapture, enabling Hilt DI, DAT SDK init, WorkManager, and SyncManager.
 */
@HiltAndroidApp
class FieldCaptureApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var glassesSessionManager: GlassesSessionManager

    @Inject
    lateinit var syncManager: SyncManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        glassesSessionManager.initialize()
        syncManager.initialize(appScope)
    }
}
