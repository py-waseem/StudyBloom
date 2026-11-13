package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.entity.StudyTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActiveTimerSnapshot(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val taskId: Long = 0,
    val taskTitle: String = "",
    val taskSubject: String = "",
    val scheduledDurationMinutes: Int = 0,
    val scheduledStartTime: String = "",
    val scheduledEndTime: String = "",
    val actualStartTimestamp: Long = 0L,
    val actualFocusedMillis: Long = 0L,
    val totalPausedMillis: Long = 0L,
    val remainingMillis: Long = 0L,
    val overtimeMillis: Long = 0L,
    val progressFraction: Float = 0f,
    val isPlannedDurationCompleted: Boolean = false
)

data class TimerFinishResult(
    val taskId: Long,
    val taskTitle: String,
    val taskSubject: String,
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int,
    val overtimeMinutes: Int,
    val pausedMinutes: Int,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isValid: Boolean = true,
    val submissionKey: String = "TASK_CLOCK:${taskId}:${startTimestamp}"
)

class TaskTimerManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _timerState = MutableStateFlow(loadSnapshotFromStorage())
    val timerState: StateFlow<ActiveTimerSnapshot> = _timerState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        // If recovered while running, restart live ticker
        if (_timerState.value.isActive) {
            startTicker()
        }
    }

    fun startTask(task: StudyTaskEntity) {
        val now = System.currentTimeMillis()
        val scheduledMinutes = if (task.durationMinutes > 0) task.durationMinutes else 45

        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE, true)
            .putBoolean(KEY_IS_PAUSED, false)
            .putLong(KEY_TASK_ID, task.id)
            .putString(KEY_TASK_TITLE, task.title)
            .putString(KEY_TASK_SUBJECT, task.subject)
            .putInt(KEY_SCHEDULED_DURATION, scheduledMinutes)
            .putString(KEY_SCHEDULED_START, task.startTime)
            .putString(KEY_SCHEDULED_END, task.endTime)
            .putLong(KEY_START_TIMESTAMP, now)
            .putLong(KEY_LAST_RESUME_TIMESTAMP, now)
            .putLong(KEY_ACCUMULATED_ACTIVE_MILLIS, 0L)
            .putLong(KEY_TOTAL_PAUSED_MILLIS, 0L)
            .putLong(KEY_LAST_PAUSE_TIMESTAMP, 0L)
            .putBoolean(KEY_ALERT_SHOWN, false)
            .commit()

        refreshSnapshot()
        startTicker()
    }

    fun pause() {
        if (!_timerState.value.isActive || _timerState.value.isPaused) return

        val now = System.currentTimeMillis()
        val lastResume = prefs.getLong(KEY_LAST_RESUME_TIMESTAMP, now)
        val prevAccumulated = prefs.getLong(KEY_ACCUMULATED_ACTIVE_MILLIS, 0L)
        val newAccumulated = prevAccumulated + (now - lastResume).coerceAtLeast(0L)

        prefs.edit()
            .putBoolean(KEY_IS_PAUSED, true)
            .putLong(KEY_ACCUMULATED_ACTIVE_MILLIS, newAccumulated)
            .putLong(KEY_LAST_PAUSE_TIMESTAMP, now)
            .commit()

        refreshSnapshot()
        NotificationHelper.showActiveTaskNotification(
            appContext,
            _timerState.value.taskTitle,
            _timerState.value.taskSubject,
            formatMillis(_timerState.value.actualFocusedMillis),
            isPaused = true
        )
    }

    fun resume() {
        if (!_timerState.value.isActive || !_timerState.value.isPaused) return

        val now = System.currentTimeMillis()
        val lastPause = prefs.getLong(KEY_LAST_PAUSE_TIMESTAMP, now)
        val prevPaused = prefs.getLong(KEY_TOTAL_PAUSED_MILLIS, 0L)
        val newPaused = prevPaused + (now - lastPause).coerceAtLeast(0L)

        prefs.edit()
            .putBoolean(KEY_IS_PAUSED, false)
            .putLong(KEY_LAST_RESUME_TIMESTAMP, now)
            .putLong(KEY_TOTAL_PAUSED_MILLIS, newPaused)
            .commit()

        refreshSnapshot()
        startTicker()
    }

    fun extendSession(additionalMinutes: Int) {
        if (!_timerState.value.isActive) return
        val currentDuration = prefs.getInt(KEY_SCHEDULED_DURATION, 45)
        val newDuration = currentDuration + additionalMinutes
        prefs.edit()
            .putInt(KEY_SCHEDULED_DURATION, newDuration)
            .putBoolean(KEY_ALERT_SHOWN, false)
            .commit()

        refreshSnapshot()
    }

    fun finishSession(): TimerFinishResult {
        val now = System.currentTimeMillis()
        val snapshot = _timerState.value
        if (!snapshot.isActive || snapshot.taskId <= 0L || snapshot.actualStartTimestamp <= 0L) {
            clearStorage()
            NotificationHelper.cancelActiveTaskNotification(appContext)
            return TimerFinishResult(
                taskId = 0L,
                taskTitle = "",
                taskSubject = "",
                plannedDurationMinutes = 0,
                actualDurationMinutes = 0,
                overtimeMinutes = 0,
                pausedMinutes = 0,
                startTimestamp = 0L,
                endTimestamp = now,
                isValid = false,
                submissionKey = ""
            )
        }

        val actualFocusedMillis = calculateCurrentActiveMillis()
        val actualMinutes = (actualFocusedMillis / 60000L).toInt()
        val plannedMinutes = snapshot.scheduledDurationMinutes
        val overtimeMinutes = (actualMinutes - plannedMinutes).coerceAtLeast(0)
        val pausedMinutes = (snapshot.totalPausedMillis / 60000L).toInt()

        val result = TimerFinishResult(
            taskId = snapshot.taskId,
            taskTitle = snapshot.taskTitle,
            taskSubject = snapshot.taskSubject,
            plannedDurationMinutes = plannedMinutes,
            actualDurationMinutes = if (actualMinutes > 0) actualMinutes else 1,
            overtimeMinutes = overtimeMinutes,
            pausedMinutes = pausedMinutes,
            startTimestamp = snapshot.actualStartTimestamp,
            endTimestamp = now,
            isValid = true,
            submissionKey = "TASK_CLOCK:${snapshot.taskId}:${snapshot.actualStartTimestamp}"
        )

        clearStorage()
        NotificationHelper.cancelActiveTaskNotification(appContext)
        return result
    }

    fun stopWithoutCompleting(): TimerFinishResult {
        return finishSession()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _timerState.value.isActive) {
                refreshSnapshot()

                val snap = _timerState.value
                if (!snap.isPaused) {
                    NotificationHelper.showActiveTaskNotification(
                        appContext,
                        snap.taskTitle,
                        snap.taskSubject,
                        formatMillis(snap.actualFocusedMillis),
                        isPaused = false
                    )

                    // Check if planned duration was just completed and notify once
                    if (snap.isPlannedDurationCompleted && !prefs.getBoolean(KEY_ALERT_SHOWN, false)) {
                        prefs.edit().putBoolean(KEY_ALERT_SHOWN, true).commit()
                        NotificationHelper.showFocusCompleteNotification(
                            appContext,
                            snap.taskTitle,
                            snap.scheduledDurationMinutes
                        )
                    }
                }

                delay(1000)
            }
        }
    }

    private fun refreshSnapshot() {
        _timerState.value = loadSnapshotFromStorage()
    }

    private fun calculateCurrentActiveMillis(): Long {
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        if (!isActive) return 0L
        val isPaused = prefs.getBoolean(KEY_IS_PAUSED, false)
        val accumulated = prefs.getLong(KEY_ACCUMULATED_ACTIVE_MILLIS, 0L)
        if (isPaused) return accumulated

        val now = System.currentTimeMillis()
        val lastResume = prefs.getLong(KEY_LAST_RESUME_TIMESTAMP, now)
        return accumulated + (now - lastResume).coerceAtLeast(0L)
    }

    private fun loadSnapshotFromStorage(): ActiveTimerSnapshot {
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        if (!isActive) return ActiveTimerSnapshot()

        val isPaused = prefs.getBoolean(KEY_IS_PAUSED, false)
        val taskId = prefs.getLong(KEY_TASK_ID, 0L)
        val taskTitle = prefs.getString(KEY_TASK_TITLE, "") ?: ""
        val taskSubject = prefs.getString(KEY_TASK_SUBJECT, "") ?: ""
        val scheduledDuration = prefs.getInt(KEY_SCHEDULED_DURATION, 45)
        val scheduledStart = prefs.getString(KEY_SCHEDULED_START, "") ?: ""
        val scheduledEnd = prefs.getString(KEY_SCHEDULED_END, "") ?: ""
        val startTimestamp = prefs.getLong(KEY_START_TIMESTAMP, 0L)
        val totalPaused = prefs.getLong(KEY_TOTAL_PAUSED_MILLIS, 0L)

        val activeMillis = calculateCurrentActiveMillis()
        val scheduledMillis = scheduledDuration * 60 * 1000L

        val remainingMillis = (scheduledMillis - activeMillis).coerceAtLeast(0L)
        val overtimeMillis = (activeMillis - scheduledMillis).coerceAtLeast(0L)
        val progress = if (scheduledMillis > 0) (activeMillis.toFloat() / scheduledMillis.toFloat()).coerceIn(0f, 1f) else 0f

        return ActiveTimerSnapshot(
            isActive = true,
            isPaused = isPaused,
            taskId = taskId,
            taskTitle = taskTitle,
            taskSubject = taskSubject,
            scheduledDurationMinutes = scheduledDuration,
            scheduledStartTime = scheduledStart,
            scheduledEndTime = scheduledEnd,
            actualStartTimestamp = startTimestamp,
            actualFocusedMillis = activeMillis,
            totalPausedMillis = totalPaused,
            remainingMillis = remainingMillis,
            overtimeMillis = overtimeMillis,
            progressFraction = progress,
            isPlannedDurationCompleted = activeMillis >= scheduledMillis
        )
    }

    private fun clearStorage() {
        tickerJob?.cancel()
        tickerJob = null
        prefs.edit().clear().commit()
        _timerState.value = ActiveTimerSnapshot()
    }

    companion object {
        private const val PREFS_NAME = "studybloom_active_timer_prefs"
        private const val KEY_IS_ACTIVE = "key_is_active"
        private const val KEY_IS_PAUSED = "key_is_paused"
        private const val KEY_TASK_ID = "key_task_id"
        private const val KEY_TASK_TITLE = "key_task_title"
        private const val KEY_TASK_SUBJECT = "key_task_subject"
        private const val KEY_SCHEDULED_DURATION = "key_scheduled_duration"
        private const val KEY_SCHEDULED_START = "key_scheduled_start"
        private const val KEY_SCHEDULED_END = "key_scheduled_end"
        private const val KEY_START_TIMESTAMP = "key_start_timestamp"
        private const val KEY_LAST_RESUME_TIMESTAMP = "key_last_resume_timestamp"
        private const val KEY_ACCUMULATED_ACTIVE_MILLIS = "key_accumulated_active_millis"
        private const val KEY_TOTAL_PAUSED_MILLIS = "key_total_paused_millis"
        private const val KEY_LAST_PAUSE_TIMESTAMP = "key_last_pause_timestamp"
        private const val KEY_ALERT_SHOWN = "key_alert_shown"

        @Volatile
        private var INSTANCE: TaskTimerManager? = null

        fun getInstance(context: Context): TaskTimerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TaskTimerManager(context).also { INSTANCE = it }
            }
        }

        fun formatMillis(millis: Long): String {
            val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            return if (h > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
            } else {
                String.format(Locale.US, "%02d:%02d", m, s)
            }
        }

        fun formatTimeOnly(timestamp: Long): String {
            if (timestamp <= 0L) return "--:--"
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}
