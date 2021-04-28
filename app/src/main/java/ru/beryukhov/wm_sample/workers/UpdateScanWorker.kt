package ru.beryukhov.wm_sample.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.work.*
import androidx.work.ListenableWorker.Result
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

import ru.beryukhov.wm_sample.R

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
            .take(10)
            .map { it * 10 }
            .doOnEach {
                setProgressAsync(workDataOf(PROGRESS to it.value))
                setForegroundAsync(createForegroundInfo("In progress... ${it.value} %"))
            }
            .collectInto(Result.success()) { result, _ -> result }
            .subscribeOn(Schedulers.computation())
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val id = applicationContext.getString(R.string.notification_channel_id)
        val title = applicationContext.getString(R.string.notification_title)
        val cancel = applicationContext.getString(R.string.cancel_download)
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(id)
        }

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(42,notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(id: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(NotificationChannel(id, "Scan notifications", NotificationManager.IMPORTANCE_HIGH))
    }
}



/**
 * Fakes
 */

private fun doFakeUpdate(): Single<Result> {
    return Single.timer(10, TimeUnit.SECONDS).map { Result.success() }
}