package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_ai_requests")
data class PendingAiRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val requestType: String, // "DOCUMENT_ANALYSIS", "ASSISTANT_QUERY", "SCHEDULE_WIZARD"
    val title: String,
    val prompt: String,
    val relatedEntityId: Long? = null,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val status: String = "QUEUED", // "QUEUED", "PROCESSING", "COMPLETED", "FAILED"
    val responsePreview: String = ""
)
