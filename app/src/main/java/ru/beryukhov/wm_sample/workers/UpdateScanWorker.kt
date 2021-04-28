package ru.beryukhov.wm_sample.workers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import androidx.work.ListenableWorker.Result
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

const val PROGRESS = "PROGRESS"
internal const val TAG_SCAN_PROGRESS = "tag_progress"

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
        .addTag(TAG_SCAN_PROGRESS)
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

internal fun getScanProgressWorkInfoItems(context: Context): LiveData<List<WorkInfo>> =
    WorkManager.getInstance(context).getWorkInfosByTagLiveData(TAG_SCAN_PROGRESS)


class UpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    RxWorker(appContext, workerParams) {
    override fun createWork(): Single<Result> {
        return doFakeUpdate().subscribeOn(Schedulers.io())
    }
}

class ScanWorker(appContext: Context, workerParams: WorkerParameters) :
    RxWorker(appContext, workerParams) {
    override fun createWork(): Single<Result> {
        return Observable.interval(1, TimeUnit.SECONDS)
            .take(100)
            .doOnEach { setProgressAsync(workDataOf(PROGRESS to it.value)) }
            .collectInto(Result.success()) { result, _ -> result }
            .subscribeOn(Schedulers.computation())
    }
}

/**
 * Fakes
 */

private fun doFakeUpdate(): Single<Result> {
    return Single.timer(10, TimeUnit.SECONDS).map { Result.success() }
}