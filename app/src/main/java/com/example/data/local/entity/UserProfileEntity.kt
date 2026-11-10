package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Learner",
    val nickname: String = "Learner",
    val purpose: String = "Programming and software development",
    val wakeTime: String = "07:00",
    val sleepTime: String = "23:00",
    val preferredStudyHours: String = "Evenings", // "Mornings", "Afternoons", "Evenings", "Flexible"
    val preferredSessionDurationMinutes: Int = 45,
    val preferredBreakDurationMinutes: Int = 15,
    val dailyGoalMinutes: Int = 180, // 3 hours daily default
    val reminderEnabled: Boolean = true,
    val reminderBeforeMinutes: Int = 10,
    val dailyPlanningReminderEnabled: Boolean = true,
    val dailyPlanningReminderTime: String = "08:00",
    val eveningSummaryEnabled: Boolean = true,
    val eveningSummaryTime: String = "21:30",
    val timeZone: String = "Asia/Kathmandu",
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val hasCompletedOnboarding: Boolean = false,
    val currentStreakDays: Int = 1,
    val lastActiveDate: String = "",
    // Guest & Account Authentication
    val isGuest: Boolean = true,
    val accountEmail: String = "",
    val accountName: String = "",
    val isEmailVerified: Boolean = false,
    val isCloudBackupEnabled: Boolean = false,
    val lastCloudSyncTimestamp: Long = 0L,
    val hasSeenAccountReminder: Boolean = false,
    val completedSessionsCount: Int = 0
)
