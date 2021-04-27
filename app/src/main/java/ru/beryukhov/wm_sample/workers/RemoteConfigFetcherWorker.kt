package ru.beryukhov.wm_sample.workers

import android.content.Context
import android.preference.PreferenceManager
import androidx.work.*
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class RemoteConfigFetcherWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    companion object {

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequestBuilder = OneTimeWorkRequestBuilder<RemoteConfigFetcherWorker>()
                .setConstraints(constraints)

            WorkManager.getInstance(context).enqueueUniqueWork(
                RemoteConfigFetcherWorker::class.java.simpleName,
                ExistingWorkPolicy.KEEP, workRequestBuilder.build()
            )
        }
    }

    override fun doWork(): Result = try {
        // Block on the task for a maximum of 60 seconds, otherwise time out.
        val taskResult = Tasks.await(frcFetchAndActivate(), 60, TimeUnit.SECONDS)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        sharedPreferences.edit().putBoolean("remote_config_stale", false).apply()
        Result.success()
    } catch (e: ExecutionException) {
        // The Task failed, this is the same exception you'd get in a non-blocking failure handler.
        if (e.cause is FirebaseRemoteConfigClientException && e.cause?.cause is IOException) {
            Result.retry()
        } else {
            // TODO Log the error here
            Result.failure()
        }
    } catch (e: InterruptedException) {
        // An interrupt occurred while waiting for the task to complete.
        Result.retry()
    } catch (e: TimeoutException) {
        // Task timed out before it could complete.
        Result.retry()
    }
}

/**
 * Fakes
 */
private fun frcFetchAndActivate(): Task<Boolean> {//Firebase.remoteConfig.fetchAndActivate()
    return Tasks.forResult(false);
}

class FirebaseRemoteConfigClientException : Exception()
