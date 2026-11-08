package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_REMINDERS = "study_reminders"
    const val CHANNEL_PLANNING = "daily_planning"
    const val CHANNEL_FOCUS = "focus_timer"
    const val CHANNEL_ACTIVE_TASK = "active_task_timer"

    const val NOTIFICATION_ID_ACTIVE_TASK = 2001
    const val EXTRA_OPEN_ACTIVE_CLOCK = "OPEN_ACTIVE_CLOCK"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Study Session Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you when study sessions and coding practice start"
                enableVibration(true)
            }

            val planningChannel = NotificationChannel(
                CHANNEL_PLANNING,
                "Daily Planning & Summaries",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning study goals and evening achievement summaries"
            }

            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS,
                "Focus Timer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pomodoro and break completion alerts"
                enableVibration(true)
            }

            val activeTaskChannel = NotificationChannel(
                CHANNEL_ACTIVE_TASK,
                "Active Task Live Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live ongoing task study clock and elapsed time"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(reminderChannel, planningChannel, focusChannel, activeTaskChannel)
            )
        }
    }

    fun showActiveTaskNotification(
        context: Context,
        taskTitle: String,
        subject: String,
        elapsedFormatted: String,
        isPaused: Boolean
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_ACTIVE_CLOCK", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_ACTIVE_TASK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) "⏸️ Paused at $elapsedFormatted" else "⏱️ Focused: $elapsedFormatted"

        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVE_TASK)
            .setSmallIcon(R.drawable.ic_studybloom_icon)
            .setContentTitle("StudyBloom • $taskTitle")
            .setContentText("$statusText ($subject)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ACTIVE_TASK, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun cancelActiveTaskNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_ACTIVE_TASK)
        } catch (_: SecurityException) {
        }
    }

    fun showTaskReminder(
        context: Context,
        taskTitle: String,
        subject: String,
        startTime: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_studybloom_icon)
            .setContentTitle("🌿 Time to bloom: $taskTitle")
            .setContentText("Your $subject session starts at $startTime. Plan gently, learn consistently.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showDailyPlanningReminder(
        context: Context,
        userName: String,
        plannedTasksCount: Int
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PLANNING)
            .setSmallIcon(R.drawable.ic_studybloom_icon)
            .setContentTitle("🌸 Good morning, $userName!")
            .setContentText("You have $plannedTasksCount sessions planned for today. Ready to learn?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(1001, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showFocusCompleteNotification(
        context: Context,
        sessionTitle: String,
        minutesSpent: Int
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_studybloom_icon)
            .setContentTitle("✨ Study Session Complete!")
            .setContentText("Your planned time for $sessionTitle is finished ($minutesSpent min). Would you like to finish or continue?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(1002, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
