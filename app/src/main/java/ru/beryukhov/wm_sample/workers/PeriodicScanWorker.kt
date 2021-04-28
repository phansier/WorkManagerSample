package ru.beryukhov.wm_sample.workers

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

private const val PERIODIC_SCAN_WORK_TAG = "periodic_scan_work_tag"

class PeriodicScanWorker(private val appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    companion object {
        fun enqueue(
            context: Context,
            repeatInterval: Long = 1,
            repeatIntervalTimeUnit: TimeUnit = TimeUnit.DAYS,
            flexTimeInterval: Long = 1,
            flexTimeIntervalUnit: TimeUnit = TimeUnit.HOURS,
            initialDelay: Long = 10,
            initialDelayTimeUnit: TimeUnit = TimeUnit.MINUTES,
        ) {
            val periodicWorkRequest = PeriodicWorkRequestBuilder<PeriodicScanWorker>(
                repeatInterval = repeatInterval, // MIN_PERIODIC_INTERVAL_MILLIS = 15 minutes.
                repeatIntervalTimeUnit = repeatIntervalTimeUnit,
                flexTimeInterval = flexTimeInterval, // MIN_PERIODIC_FLEX_MILLIS =  5 minutes.
                flexTimeIntervalUnit = flexTimeIntervalUnit
            )
                .setInitialDelay(initialDelay, initialDelayTimeUnit)
                .addTag(PERIODIC_SCAN_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PeriodicScanWorker::class.java.simpleName,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(PERIODIC_SCAN_WORK_TAG)
        }
    }

    override suspend fun doWork(): Result {
        enqueueUpdateScan(appContext, false)
        return Result.success()
    }
}