package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.example.ui.StudyBloomApp
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private var openClockTrigger by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create Android notification channels for study reminders and focus sessions
        NotificationHelper.createNotificationChannels(this)

        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_ACTIVE_CLOCK, false) == true) {
            openClockTrigger = System.currentTimeMillis()
        }

        setContent {
            StudyBloomApp(openActiveClockTrigger = openClockTrigger)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_ACTIVE_CLOCK, false)) {
            openClockTrigger = System.currentTimeMillis()
        }
    }
}
