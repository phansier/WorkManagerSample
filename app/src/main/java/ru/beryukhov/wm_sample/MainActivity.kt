package ru.beryukhov.wm_sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.*
import ru.beryukhov.wm_sample.ui.theme.WM_SampleTheme
import ru.beryukhov.wm_sample.workers.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WM_SampleTheme {
                val list: List<WorkInfo>? by getScanProgressWorkInfoItems(this).observeAsState()
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Column {

                        Button(modifier = Modifier.padding(16.dp),
                            onClick = enqueueScan()
                        ) {
                            Text("Start scan")
                        }
                        val value = list?.firstOrNull { it.state == WorkInfo.State.RUNNING }
                        if (value != null) {
                            ScanProgress(
                                value.progress.getLong(
                                    PROGRESS,
                                    0
                                ) / 100f
                            )
                        }

                    }

                }
            }
        }

        //enqueWorkRequests()

    }

    private fun enqueueScan(): () -> Unit = {
        val scanWorkRequest = OneTimeWorkRequestBuilder<ScanWorker>()
            .addTag(TAG_SCAN_PROGRESS)
            .build()
        WorkManager.getInstance(this@MainActivity)
            .enqueue(scanWorkRequest)
    }

    private fun enqueWorkRequests() {
        // Works are launched from here only for example
        // In real app should be launched from some business logic classes
        val billingData = Data.Builder().putString(BILLING_WORK_DATA_KEY, "test").build()

        //enqueueUpdateScan(this, false)
        RemoteConfigFetcherWorker.enqueue(this)
        BillingTicketWorker.enqueue(this, billingData)
        /*if (resultReceived) {
            BillingTicketWorker.cancel(this)
        }*/

        // Let user setup regular scan params (repetition period and time) to set up them in enque params
        // This periodic work starts Update&Scan OneTimeWorkRequest
        PeriodicScanWorker.enqueue(
            this,
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 5,
            flexTimeIntervalUnit = TimeUnit.MINUTES,
            initialDelay = 10, initialDelayTimeUnit = TimeUnit.SECONDS
        )
    }
}

@Composable
private fun ScanProgress(progress: Float) {

    Card(
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Scan Progress")
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
            )

            LinearProgressIndicator(
                color = MaterialTheme.colors.secondary,
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    WM_SampleTheme {
        ScanProgress(0.5f)
    }
}