package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "availability_slots")
data class AvailabilitySlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
    val startTime: String, // HH:mm
    val endTime: String, // HH:mm
    val isEnabled: Boolean = true
)
