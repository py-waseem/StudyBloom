package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "material_topics",
    foreignKeys = [
        ForeignKey(
            entity = LearningMaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["materialId"])]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long,
    val title: String,
    val subtopicsRaw: String = "", // comma-separated or json string
    val difficulty: String = "MEDIUM", // "EASY", "MEDIUM", "HARD"
    val estimatedMinutes: Int = 60,
    val isPrerequisite: Boolean = false,
    val orderIndex: Int = 0,
    val isCompleted: Boolean = false
)
