package ru.beryukhov.wm_sample.workers

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

const val BILLING_WORK_DATA_KEY = "billing_work_data_key"
private const val BILLING_WORK_TAG = "billing_work_tag"

class BillingTicketWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    companion object {
        fun enqueue(context: Context, billingData: Data) {
            val billingWorkRequest = PeriodicWorkRequestBuilder<BillingTicketWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
                flexTimeInterval = 1,
                flexTimeIntervalUnit = TimeUnit.HOURS
            )
                .setInputData(billingData)
                .setInitialDelay(10, TimeUnit.MINUTES)
                .addTag(BILLING_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                BillingTicketWorker::class.java.simpleName,
                ExistingPeriodicWorkPolicy.REPLACE,
                billingWorkRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(BILLING_WORK_TAG)
        }
    }

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            inputData.getString(BILLING_WORK_DATA_KEY) ?: return@withContext Result.failure()
            doFakeServerRequest()
        }
}

/**
 * Fakes
 */


@OptIn(ExperimentalTime::class)
private suspend fun doFakeServerRequest(): ListenableWorker.Result {
    delay(10.toDuration(DurationUnit.SECONDS))
    return ListenableWorker.Result.success()
}