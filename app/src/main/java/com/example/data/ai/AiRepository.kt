package com.example.data.ai

import com.example.data.local.entity.AvailabilitySlotEntity
import com.example.data.local.entity.LearningMaterialEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.data.local.entity.TopicEntity

data class DocumentAnalysisResult(
    val subject: String,
    val summary: String,
    val estimatedDifficulty: String,
    val estimatedTotalHours: Double,
    val topics: List<ExtractedTopic>
)

data class ExtractedTopic(
    val title: String,
    val subtopics: List<String>,
    val difficulty: String, // "EASY", "MEDIUM", "HARD"
    val estimatedMinutes: Int,
    val isPrerequisite: Boolean,
    val orderIndex: Int
)

data class ScheduleRequest(
    val selectedSubject: String,
    val topics: List<TopicEntity>,
    val startDate: String, // YYYY-MM-DD
    val targetEndDate: String, // YYYY-MM-DD
    val sessionDurationMinutes: Int = 45,
    val breakDurationMinutes: Int = 15,
    val priority: String = "HIGH",
    val availabilitySlots: List<AvailabilitySlotEntity>
)

data class AssistantContext(
    val userName: String,
    val todayDate: String,
    val pendingTasks: List<StudyTaskEntity>,
    val completedTasksToday: List<StudyTaskEntity>,
    val materials: List<LearningMaterialEntity>,
    val availableHoursToday: Double
)

interface AiRepository {
    suspend fun analyzeDocument(
        fileName: String,
        fileType: String,
        contentSample: String
    ): Result<DocumentAnalysisResult>

    suspend fun generateSchedule(
        request: ScheduleRequest
    ): Result<List<StudyTaskEntity>>

    suspend fun askAssistant(
        prompt: String,
        context: AssistantContext
    ): String
}
