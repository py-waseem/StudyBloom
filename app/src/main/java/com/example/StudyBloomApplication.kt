package com.example

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.icon.WeekdayIconManager
import com.example.icon.WeekdayIconScheduler

class StudyBloomApplication : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()
        WeekdayIconManager.syncLauncherIcon(this)
        WeekdayIconScheduler.ensureScheduled(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        WeekdayIconManager.syncLauncherIcon(this)
        WeekdayIconScheduler.ensureScheduled(this)
    }
}
