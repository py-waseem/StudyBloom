package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String = "Focus Session",
    val subject: String = "General",
    val durationMinutes: Int, // Actual focused duration
    val plannedDurationMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val pausedMinutes: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String, // YYYY-MM-DD
    val completedSuccessfully: Boolean = true,
    val sessionType: String = "TASK_CLOCK" // "TASK_CLOCK", "POMODORO", "CUSTOM", "BREAK"
)
