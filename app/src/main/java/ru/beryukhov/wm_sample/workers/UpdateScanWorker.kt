package ru.beryukhov.wm_sample.workers

import android.content.Context
import androidx.work.*
import androidx.work.ListenableWorker.Result
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

fun enqueueUpdateScan(context: Context, isOnlyWhileCharging: Boolean) {
    val updateConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED)
        .setRequiresStorageNotLow(true)
        .build()
    val scanConstraints = Constraints.Builder().apply {
        // java.lang.IllegalArgumentException: Cannot set backoff criteria on an idle mode job
        // java.lang.IllegalArgumentException: Cannot run in foreground with an idle mode constraint
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setRequiresDeviceIdle(true)
        }
        */
        if (isOnlyWhileCharging) {
            setRequiresCharging(isOnlyWhileCharging)
        } else {
            setRequiresBatteryNotLow(true)
        }
    }.build()

    val updateWorkRequest = OneTimeWorkRequestBuilder<UpdateWorker>()
        .setConstraints(updateConstraints)
        .setBackoffCriteria(
            BackoffPolicy.LINEAR,
            10,
            TimeUnit.MINUTES
        )
        .build()

    val scanWorkRequest = OneTimeWorkRequestBuilder<ScanWorker>()
        .setConstraints(scanConstraints)
        .setBackoffCriteria(
            BackoffPolicy.LINEAR,
            10,
            TimeUnit.MINUTES
        )
        .build()

    WorkManager.getInstance(context)
        .beginUniqueWork(
            UpdateWorker::class.java.simpleName,
            ExistingWorkPolicy.REPLACE,
            updateWorkRequest
        )
        .then(scanWorkRequest)
        .enqueue()

    //To make a "diamond" in graph
    /*
    WorkManager.getInstance(context)
        .beginWith(updateWorkRequest.build())
        .then(listOf(scanWorkRequest.build(), workRequest.build()))
        .then(listOf(workRequest.build(), scanWorkRequest.build()))
        .then(updateWorkRequest.build())
        .enqueue()
    */

}


class UpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    RxWorker(appContext, workerParams) {
    override fun createWork(): Single<Result> {
        return doFakeSomething().subscribeOn(Schedulers.io())
    }
}

class ScanWorker(appContext: Context, workerParams: WorkerParameters) :
    RxWorker(appContext, workerParams) {
    override fun createWork(): Single<Result> {
        return doFakeSomething().subscribeOn(Schedulers.computation())
    }
}

/**
 * Fakes
 */

private fun doFakeSomething(): Single<Result> {
    return Single.timer(10, TimeUnit.SECONDS).map { Result.success() }
}