package ru.beryukhov.wm_sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.work.Data
import androidx.work.WorkInfo
import ru.beryukhov.wm_sample.ui.theme.WM_SampleTheme
import ru.beryukhov.wm_sample.workers.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WM_SampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    val list: List<WorkInfo>? by getScanProgressWorkInfoItems(this).observeAsState()
                    val value = list?.firstOrNull()
                    if (value != null) {
                        Text("Scan Progress")
                        LinearProgressIndicator(
                            color = MaterialTheme.colors.secondary,
                            progress = value.progress.getLong(
                                PROGRESS,
                                0
                            ) / 100f
                        )
                    }

                }
            }
        }

        enqueWorkRequests()
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
        PeriodicScanWorker.enqueue(this,
            repeatInterval=15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 5,
            flexTimeIntervalUnit = TimeUnit.MINUTES,
            initialDelay = 10, initialDelayTimeUnit = TimeUnit.SECONDS)
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    WM_SampleTheme {
        Greeting("Android")
    }
}