package com.example

import com.example.data.ai.AiRepositoryImpl
import com.example.data.ai.ScheduleRequest
import com.example.data.local.entity.AvailabilitySlotEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.data.local.entity.TopicEntity
import com.example.util.PdfExporter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun ai_document_analysis_extracts_topics_accurately() = runBlocking {
        val aiRepo = AiRepositoryImpl()
        val result = aiRepo.analyzeDocument(
            fileName = "Django_Web_Framework_Textbook.pdf",
            fileType = "PDF",
            contentSample = "Chapter 1: Intro\nChapter 2: Models\nChapter 3: QuerySets\nChapter 4: Views"
        )
        assertTrue(result.isSuccess)
        val analysis = result.getOrThrow()
        assertEquals("Django Development", analysis.subject)
        assertTrue(analysis.topics.isNotEmpty())
    }

    @Test
    fun ai_schedule_generation_respects_slot_boundaries_and_avoids_overlap() = runBlocking {
        val aiRepo = AiRepositoryImpl()
        val topics = listOf(
            TopicEntity(materialId = 1, title = "Models & QuerySets", difficulty = "HARD", estimatedMinutes = 45, isPrerequisite = true, orderIndex = 1),
            TopicEntity(materialId = 1, title = "Class-Based Views", difficulty = "MEDIUM", estimatedMinutes = 45, isPrerequisite = false, orderIndex = 2)
        )
        val slots = listOf(
            AvailabilitySlotEntity(dayOfWeek = 1, startTime = "07:00", endTime = "09:00", isEnabled = true)
        )
        val request = ScheduleRequest(
            selectedSubject = "Django Development",
            topics = topics,
            startDate = "2026-11-09",
            targetEndDate = "2026-11-15",
            sessionDurationMinutes = 45,
            breakDurationMinutes = 15,
            availabilitySlots = slots
        )
        val scheduleResult = aiRepo.generateSchedule(request)
        assertTrue(scheduleResult.isSuccess)
        val tasks = scheduleResult.getOrThrow()
        assertTrue(tasks.isNotEmpty())
        assertEquals("07:00", tasks[0].startTime)
        assertEquals("07:45", tasks[0].endTime)
    }

    @Test
    fun csv_export_formats_schedule_columns_correctly() {
        val tasks = listOf(
            StudyTaskEntity(
                title = "Python Functions",
                subject = "Python Core",
                category = "PROGRAMMING",
                priority = "HIGH",
                date = "2026-11-08",
                startTime = "07:00",
                endTime = "07:45",
                durationMinutes = 45,
                isCompleted = true,
                learningObjective = "Master closures"
            )
        )
        val csv = PdfExporter.generateCsvExport(tasks)
        assertTrue(csv.contains("Date,StartTime,EndTime,Title,Subject"))
        assertTrue(csv.contains("Python Functions"))
        assertTrue(csv.contains("Completed"))
    }
}
