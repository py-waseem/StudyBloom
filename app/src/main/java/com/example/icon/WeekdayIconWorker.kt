package com.example.icon

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class WeekdayIconWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            WeekdayIconManager.syncLauncherIcon(applicationContext)
            WeekdayIconScheduler.scheduleNextMidnightCheck(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object WeekdayIconScheduler {
    const val PERIODIC_WORK_NAME = "studybloom_weekday_icon_periodic"
    const val MIDNIGHT_WORK_NAME = "studybloom_weekday_icon_midnight"
    private const val MIDNIGHT_BUFFER_MILLIS = 60_000L

    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext ?: context
        runCatching {
            val workManager = WorkManager.getInstance(appContext)
            val periodicRequest = PeriodicWorkRequestBuilder<WeekdayIconWorker>(
                6,
                TimeUnit.HOURS
            ).build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            scheduleNextMidnightCheck(appContext)
        }
    }

    fun scheduleNextMidnightCheck(
        context: Context,
        nowMillis: Long = WeekdayIconManager.currentEffectiveTimestampMillis(),
        timeZone: TimeZone = WeekdayIconManager.currentEffectiveTimeZone()
    ) {
        val appContext = context.applicationContext ?: context
        val delayMillis = computeDelayUntilNextMidnightMillis(nowMillis, timeZone)
        runCatching {
            val midnightRequest = OneTimeWorkRequestBuilder<WeekdayIconWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(appContext).enqueueUniqueWork(
                MIDNIGHT_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                midnightRequest
            )
        }
    }

    internal fun computeDelayUntilNextMidnightMillis(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val nextMidnight = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return (nextMidnight.timeInMillis - nowMillis + MIDNIGHT_BUFFER_MILLIS)
            .coerceAtLeast(MIDNIGHT_BUFFER_MILLIS)
    }
}
