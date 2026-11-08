package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_materials")
data class LearningMaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fileType: String, // "PDF", "DOCX", "TXT", "IMAGE", "NOTES"
    val uriString: String = "",
    val uploadDate: String, // YYYY-MM-DD
    val fileSizeFormatted: String, // e.g. "2.4 MB"
    val extractedText: String = "",
    val summary: String = "",
    val mainSubject: String = "",
    val estimatedDifficulty: String = "MEDIUM", // "BEGINNER", "MEDIUM", "ADVANCED"
    val estimatedTotalHours: Double = 10.0,
    val status: String = "READY" // "READY", "PROCESSING", "ERROR"
)
