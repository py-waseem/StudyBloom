package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "PROGRAMMING", // "PROGRAMMING", "CODING_PRACTICE", "UNIVERSITY", "PROJECT", "REVISION", "READING", "PERSONAL"
    val subject: String = "General",
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val date: String, // YYYY-MM-DD
    val startTime: String = "09:00", // HH:mm
    val endTime: String = "10:00", // HH:mm
    val durationMinutes: Int = 60,
    val actualDurationMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val notes: String = "",
    val materialId: Long? = null,
    val topicId: Long? = null,
    val taskType: String = "STUDY", // "STUDY", "CODING", "REVISION", "BREAK", "EXERCISE"
    val learningObjective: String = "",
    val estimatedDifficulty: String = "MEDIUM", // "EASY", "MEDIUM", "HARD"
    val isAIGenerated: Boolean = false,
    val startedAtTimestamp: Long? = null,
    val completedAtTimestamp: Long? = null
)
