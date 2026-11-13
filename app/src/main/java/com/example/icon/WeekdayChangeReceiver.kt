package com.example.icon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WeekdayChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action ?: return

        if (action in SUPPORTED_ACTIONS) {
            WeekdayIconManager.syncLauncherIcon(context)
            WeekdayIconScheduler.ensureScheduled(context)
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
