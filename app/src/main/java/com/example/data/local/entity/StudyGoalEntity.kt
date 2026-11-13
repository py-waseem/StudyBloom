package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object GoalOwnership {
    const val GUEST_OWNER_ID = "guest"

    fun ownerIdForProfile(profile: UserProfileEntity?): String {
        if (profile == null || profile.isGuest || profile.accountEmail.isBlank()) {
            return GUEST_OWNER_ID
        }
        return "user:${profile.accountEmail.trim().lowercase()}"
    }
}

@Entity(
    tableName = "study_goals",
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["preferredDate"]),
        Index(value = ["ownerId", "preferredDate"])
    ]
)
data class StudyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String = GoalOwnership.GUEST_OWNER_ID,
    val title: String,
    val description: String = "",
    val subject: String = "General",
    val category: String = "PROGRAMMING", // "PROGRAMMING", "CODING_PRACTICE", "UNIVERSITY", "PROJECT", "REVISION", "READING", "PERSONAL"
    val estimatedMinutes: Int = 60,
    val actualMinutes: Int = 0,
    val difficulty: String = "MEDIUM", // "EASY", "MEDIUM", "HARD"
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val preferredDate: String, // YYYY-MM-DD
    val deadlineDate: String = "", // YYYY-MM-DD or ""
    val isCompleted: Boolean = false,
    val isSynced: Boolean = false,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val updatedAtTimestamp: Long = System.currentTimeMillis()
)
