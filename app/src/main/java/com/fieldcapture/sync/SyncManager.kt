package com.fieldcapture.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.fieldcapture.network.ConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates WorkManager sync:
 * - Schedules periodic 15-minute queue check as a safety net
 * - Triggers immediate queue processing when connectivity returns
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityObserver: ConnectivityObserver
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    fun initialize(scope: CoroutineScope) {
        schedulePeriodicSync()

        scope.launch {
            connectivityObserver.observe().collectLatest { status ->
                when (status) {
                    ConnectivityObserver.Status.AVAILABLE -> {
                        Log.i(TAG, "Network available — triggering queue processing")
                        triggerQueueProcessing()
                    }
                    ConnectivityObserver.Status.LOST,
                    ConnectivityObserver.Status.UNAVAILABLE -> {
                        Log.i(TAG, "Network lost — captures will be queued")
                    }
                    ConnectivityObserver.Status.LOSING -> {
                        Log.d(TAG, "Network losing...")
                    }
                }
            }
        }
    }

    fun triggerQueueProcessing() {
        val workRequest = OneTimeWorkRequestBuilder<ExtractionQueueWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                ExtractionQueueWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
    }

    private fun schedulePeriodicSync() {
        val periodicRequest = PeriodicWorkRequestBuilder<ExtractionQueueWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                ExtractionQueueWorker.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
    }
}
