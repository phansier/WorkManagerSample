package ru.beryukhov.wm_sample.workers

import android.content.Context
import android.os.Build
import androidx.work.*
import androidx.work.ListenableWorker.Result
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

fun enqueue(context: Context, isOnlyWhileCharging: Boolean) {
    val updateConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED)
        .setRequiresStorageNotLow(true)
        .build()
    val scanConstraints = Constraints.Builder().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setRequiresDeviceIdle(true)
        }
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
            UPDATE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            updateWorkRequest
        )
        .then(scanWorkRequest)
        .enqueue()

}

private const val UPDATE_WORK_NAME = "update_work_name"

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

fun doFakeSomething(): Single<Result> {
    return Single.timer(10, TimeUnit.SECONDS).map { Result.success() }
}