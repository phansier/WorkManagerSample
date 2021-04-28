# WorkManager (WM)

## Work Creation

### Work class

- fun doWork(): Result

	- asynchronously
	- on background thread
	- Result

		- success()
		- failure()
		- retry()

			- Retry policies

- what to run

### abstract WorkRequest class

- how and when to run
- subtypes

	- OneTimeWorkRequest

		- OneTimeWorkRequest.from(MyWork::class.java)
		- OneTimeWorkRequestBuilder<MyWork>()
       // Additional configuration
       .build()
		- Delay

			- Builder.setInitialDelay(10, TimeUnit.MINUTES)

	- PeriodicWorkRequest

		- PeriodicWorkRequestBuilder<SaveImageToFileWorker>(1, TimeUnit.HOURS)
    // Additional configuration
   .build()

			- interval = minimum time between repetitions
			- flex period = repeatInterval - flexInterval

				- Что будет, если flexInterval > repeatInterval?

			- Both intervals >=  PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS

		- Work Constraints > Periodic

			- Work may be skipped

		- Delay

			- Will be applied only to first launch

- corresponding builders

	- Work subclass used as type parameter

### Submitting

- WorkManager
    .getInstance(myContext)
    .enqueue(uploadWorkRequest)

## Work constraints

### NetworkType

### BatteryNotLow

### RequiresCharging

### DeviceIdle

### StorageNotLow

### Constraints.Builder()

- Use in WorkRequest.Builder()

	- val myWorkRequest: WorkRequest =
   OneTimeWorkRequestBuilder<MyWork>()
       .setConstraints(constraints)
       .build()

- val constraints = Constraints.Builder()
   .setRequiredNetworkType(NetworkType.UNMETERED)
   .setRequiresCharging(true)
   .build()
- All of Constraints (and not or)
- Constraint may stop the work in the middle

## Retry and Backoff policies

### return Result.retry() from worker

### Backoff delay

- minimum amount of time to wait 
- default = 10 seconds

### Backoff policy

- how the backoff delay should increase
- 2 types

	- LINEAR

		- 10->20->30->40

	-  EXPONENTIAL

		- default
		- 10->20->40->80

### Builder..setBackoffCriteria(
       BackoffPolicy.LINEAR,
       OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
       TimeUnit.MILLISECONDS)

### Delays are always >= to given

## Tagging

### Each Work has Id

- Used for

	- Cancel
	- Observe progress

### Use Tag

- Group of logically related work
- Builder.addTag("cleanup")
- WorkRequest.getTags()

	- Set<String>
	- Multiple tags possible

- Usage

	- WorkManager.cancelAllWorkByTag(String)
	- WorkManager.getWorkInfosByTag(String)

## Input & Output Data

### key-value pairs in a Data object

### Input

- Worker.getInputData()
- Builder.setInputData()

## Work States

### One-time work

- https://developer.android.com/images/topic/libraries/architecture/workmanager/how-to/one-time-work-flow.png
- WorkInfo.State.isFinished() returns true

### Periodic work

- https://developer.android.com/images/topic/libraries/architecture/workmanager/how-to/periodic-work-states.png

### Enqueued

### Running

### Succeeded

### Failed

### Cancelled

### Blocked

- For W orchestrated in a series, or chain

## Managing Work

### .enqueue(myWork)

### Enqueue Unique (by name)

- .enqueueUniqueWork()
- .enqueueUniquePeriodicWork()

### Observing your work

- WM.getWorkInfoById(): WorkInfo
- WM. getWorkInfosForUniqueWork(name): List<WorkInfo>
- WM.getWorkInfosByTag(): List<WorkInfo>

### WorkQuery

- For complex requests
- WM.getWorkInfos(workQuery)

	- or getWorkInfosLiveData()

- val workQuery = WorkQuery.Builder
       .fromTags(listOf("syncTag"))
       .addStates(listOf(WorkInfo.State.FAILED, WorkInfo.State.CANCELLED))
       .addUniqueWorkNames(listOf("preProcess", "sync"))
   .build()
- AND in builder, OR in lists

	- (name1 OR name2 OR ...) AND (tag1 OR tag2 OR ...)

### Cancelling work

- WM.cancelWorkById()
- WM.cancelUniqueWork(name)
- WM.cancelAllWorkByTag()
- Any WorkRequests depending on this Work will also be cancelled
- Running work receives callback

	- ListenableWorker.onStopped()
	- may be needed for cleanup

### Stop a running Worker

- caused by

	- explicitly cancelling
	- unique work with Replace policy
	- work constraints are no longer met
	- asked by system

		- execution deadline is 10 minutes

- should stop work an close resources ASAP

	- onStopped() callback
	- isStopped() property

- WM ignores the Result

### Observing progress (since 2.3.0-alpha01)

- Updating progress

	- Java

		- ListenableWorker or Worker
		- setProgressAsync()
		- returns ListenableFuture<Void>
		- RxWorker

	- Kotlin

		- CoroutineWorker
		- suspend setProgress()

- progress is Data like Input&Output
- applicable while W is Running
- ObservingProgress

	- getWorkInfoBy…()
	- getWorkInfoBy…LiveData() 

## Chaining Work

### WM.beginWith(OneTimeWorkRequest or list<>): WorkContinuation

### WorkContinuation.then(OTWR or list<>): WorkContinuation

### WC.enqueue()

### example code

- WorkManager.getInstance(myContext)
   // Candidates to run in parallel
   .beginWith(listOf(plantName1, plantName2, plantName3))
   // Dependent work (only runs after all previous work in chain)
   .then(cache)
   .then(upload)
   // Call enqueue to kick things off
   .enqueue()

### InputMerger

- for handling results of previous parallel Works
- OverwritingInputMerger

	- merges keys and overwrites (by last completed) if repeated

		- no guarantees of order

- ArrayCreatingInputMerger

	- merges keys and creates array (of values for a key) if repeated

- Custom implementation possible

## Testing

### Unit testing

- TestWorkerBuilder

	- params

		- context
		- executor
		- inputData = workDataOf (key to value)

- TestListenableWorkerBuilder

	- no executor param

### Integration tests

- work-testing artifact

	- androidTestImplementation

- replaced by TestWorkerBuilder

## Debugging

### WorkManager 2.4.0+

- adb shell am broadcast -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" -p "<your_app_package_name>"


	- Work requests that were completed in the last 24 hours.
	- Work requests that are currently running.
	- Scheduled work requests

### logs with  log-tag prefix WM-

- default WorkManagerInitializer 

	- <provider
    android:name="androidx.work.impl.WorkManagerInitializer"
    android:authorities="${applicationId}.workmanager-init"
    tools:node="remove"/> 
	- Need to be removed from AndroidManifest

- custom initialization

	- on-demand configuration

		-  WM 2.1.0+
		- class MyApplication() : Application(), Configuration.Provider {  
    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
 .setMinimumLoggingLevel(android.util.Log.DEBUG)            
        .build()
} 
		- Need to implicitly call WorkManager.getInstance(Context)

			- do not need to call WorkManager.initialize() yourself

		- May be also useful for improving app start time

### API level 23+

- adb shell dumpsys jobscheduler
- SystemJobService

	- androidx.work.impl.background.systemjob.SystemJobService
	- For every job, the output from the command lists required, satisfied, and unsatisfied constraints.
	- required = satisfied + not

## Threading

### WM performs background work asynchronously by itself

### 4 work primitives

- Worker

	- by default on background thread

		- it comes from the Executor specified in WM's Configuration
		- by changing the Configuration the Executor can be customized

	- thread can be overwritten
	- needs to override onStopped() or call isStopped() to handle stoppages
	- expects to work be done in blocking fashion

- CoroutineWorker

	- by default on dispatcher Default
	- dispatcher can be customized

		- e.g. withContext(Dispatchers.IO) {} in doWork()

	- needs  work-runtime-ktx artifact
	- handles stoppages automatically

		- cancels the coroutine

- RxWorker

	- override fun createWork(): Single<Result>

		- called on main thread
		- subscribed on a background thread by default
		- override RxWorker.getBackgroundScheduler() to change the subscribing thread

	- work-rxjava2 artifact

		- or work-rxjava3

	- handles stoppages automatically

		- disposes the subscription

- Listenable Worker

	- Base class for 3 above
	- for use with Java callback-based asynchronous API
	- startWork() is calling on main thread

		- returns ListenableFuture<Result>

			- needs Guava or concurrent-futures artifact

## Support for long-running workers

### WM 2.3.0-alpha02 +

### can run longer than 10 minutes

- bulk uploads or downloads
- crunching on an ML model locally

### manages and runs a foreground service

- showing a configurable notification

### ListenableWorker -> setForegroundAsync()

### CoroutineWorker -> setForeground() 

### createCancelPendingIntent()

- WM 2.3.0-alpha03+
- add a notification action to cancel the Worker

### target 29+

- location access

### target 30+

- camera or microphone access

## More materials

### https://www.youtube.com/playlist?list=PLWz5rJ2EKKc_J88-h0PhCO_aV0HIAs9Qk

- Workmanager - MAD Skills, video series

## Dependencies

### dependencies {
  def work_version = "2.5.0"

    // (Java only)
    implementation "androidx.work:work-runtime:$work_version"

    // Kotlin + coroutines
    implementation "androidx.work:work-runtime-ktx:$work_version"

    // optional - RxJava2 support
    implementation "androidx.work:work-rxjava2:$work_version"

    // optional - GCMNetworkManager support
    implementation "androidx.work:work-gcm:$work_version"

    // optional - Test helpers
    androidTestImplementation "androidx.work:work-testing:$work_version"

    // optional - Multiprocess support
    implementation "androidx.work:work-multiprocess:$work_version"
  }

## Advantages

### API 14+

### consistent API

### recommended replacement for all previous Android background scheduling APIs

- FirebaseJobDispatcher,
- GcmNetworkManager

	- Still used inside on API 14-22 with Google Services and work-gcm

		- work-gcm?

- Job Scheduler

	- Still used inside on API 23+

- Custom alarm manager & broadcast receiver

	- Used inside on API 14-22 without Google Services

### Work Constraints

- run when

	- the network is Wi-Fi
	- the device idle
	- it has sufficient storage space
	- ...

### Robust Scheduling

- One-time
- Repeatedly
- Works

	- Tags
	- Names
	- Groups of works

- Scheduled work kept in SQLite

	- WM ensures 

		- W persists
		- W rescheduled after reboot
		- Doze mode

### Flexible retry policies

- Exponential backoff policies

### Work chaining

- Sequentially
- Parallelly
- WorkManager.getInstance(...)
    .beginWith(listOf(workA,workB))
    .then(workC)
    .enqueue()

### Threading Interoperability

- RxJava
- Coroutines

## Purpose

### Deferrable tasks

- not required to run immediately
- required to run reliably
- Examples

	- Sending logs or analytics to backend services
	- Periodically syncing application data with a server

### Asynchronous tasks

### Runs after app exits or device restarts

### https://developer.android.com/guide/background

- Not for that
