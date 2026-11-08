package com.example.data.ai

import com.example.data.local.entity.AvailabilitySlotEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.data.local.entity.TopicEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AiRepositoryImpl : AiRepository {

    override suspend fun analyzeDocument(
        fileName: String,
        fileType: String,
        contentSample: String
    ): Result<DocumentAnalysisResult> {
        // Simulate thoughtful processing with a brief delay
        delay(1200)

        val cleanName = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
        val lower = cleanName.lowercase()

        val detectedSubject = when {
            lower.contains("django") -> "Django Development"
            lower.contains("python") -> "Python Core & Scripting"
            lower.contains("react") || lower.contains("javascript") || lower.contains("frontend") -> "Frontend Web Development"
            lower.contains("algo") || lower.contains("dsa") || lower.contains("data struct") -> "Data Structures & Algorithms"
            lower.contains("sql") || lower.contains("database") -> "Database Systems & SQL"
            lower.contains("os") || lower.contains("system") -> "Operating Systems & Concurrency"
            lower.contains("network") -> "Computer Networks & Protocols"
            lower.contains("machine learning") || lower.contains("ml") || lower.contains("ai") -> "Machine Learning Foundations"
            lower.contains("exam") || lower.contains("prep") -> "Examination Syllabus & Review"
            else -> cleanName.capitalizeWords()
        }

        val topics = if (contentSample.isNotBlank() && contentSample.lines().size > 3) {
            parseTopicsFromContent(contentSample, detectedSubject)
        } else {
            generateDefaultTopicsForSubject(detectedSubject)
        }

        val totalMinutes = topics.sumOf { it.estimatedMinutes }
        val totalHours = String.format(Locale.US, "%.1f", totalMinutes / 60.0).toDoubleOrNull() ?: 8.0

        val difficulty = when {
            topics.any { it.difficulty == "HARD" } -> "HARD"
            topics.count { it.difficulty == "MEDIUM" } >= topics.size / 2 -> "MEDIUM"
            else -> "BEGINNER"
        }

        val summary = "StudyBloom AI analyzed '$fileName'. Identified $detectedSubject with ${topics.size} structured learning modules and prerequisite order mapped for steady mastery."

        return Result.success(
            DocumentAnalysisResult(
                subject = detectedSubject,
                summary = summary,
                estimatedDifficulty = difficulty,
                estimatedTotalHours = totalHours,
                topics = topics
            )
        )
    }

    override suspend fun generateSchedule(
        request: ScheduleRequest
    ): Result<List<StudyTaskEntity>> {
        delay(1000)

        val generatedTasks = mutableListOf<StudyTaskEntity>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        try {
            val startDateParsed = dateFormat.parse(request.startDate)
            if (startDateParsed != null) cal.time = startDateParsed
        } catch (_: Exception) {
            cal.time = Date()
        }

        val sessionDuration = if (request.sessionDurationMinutes in 20..120) request.sessionDurationMinutes else 45
        val breakDuration = if (request.breakDurationMinutes in 5..30) request.breakDurationMinutes else 15

        // Order topics: prerequisites first, then orderIndex
        val sortedTopics = request.topics.sortedWith(
            compareByDescending<TopicEntity> { it.isPrerequisite }
                .thenBy { it.orderIndex }
        )

        var topicQueue = sortedTopics.toMutableList()
        var dayCounter = 0
        val maxDays = 14 // Schedule up to 2 weeks ahead

        while (topicQueue.isNotEmpty() && dayCounter < maxDays) {
            val dateStr = dateFormat.format(cal.time)
            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            // Find matching availability slots for this day
            val slotsForDay = request.availabilitySlots.filter { it.dayOfWeek == dayOfWeek && it.isEnabled }
            val activeSlots = if (slotsForDay.isNotEmpty()) {
                slotsForDay
            } else {
                listOf(
                    AvailabilitySlotEntity(dayOfWeek = dayOfWeek, startTime = "07:00", endTime = "08:30"),
                    AvailabilitySlotEntity(dayOfWeek = dayOfWeek, startTime = "19:00", endTime = "21:30")
                )
            }

            for (slot in activeSlots) {
                if (topicQueue.isEmpty()) break

                var currentMinute = timeStringToMinutes(slot.startTime)
                val slotEndMinute = timeStringToMinutes(slot.endTime)

                while (currentMinute + sessionDuration <= slotEndMinute && topicQueue.isNotEmpty()) {
                    val currentTopic = topicQueue.first()

                    val taskStart = minutesToTimeString(currentMinute)
                    val taskEnd = minutesToTimeString(currentMinute + sessionDuration)

                    val isCoding = request.selectedSubject.lowercase().contains("python") ||
                            request.selectedSubject.lowercase().contains("django") ||
                            request.selectedSubject.lowercase().contains("react") ||
                            request.selectedSubject.lowercase().contains("code") ||
                            request.selectedSubject.lowercase().contains("web")

                    val taskType = when {
                        currentTopic.difficulty == "HARD" && isCoding -> "CODING"
                        currentTopic.isPrerequisite -> "STUDY"
                        dayCounter % 2 == 1 && generatedTasks.isNotEmpty() -> "REVISION"
                        else -> "STUDY"
                    }

                    val category = when (taskType) {
                        "CODING" -> "CODING_PRACTICE"
                        "REVISION" -> "REVISION"
                        else -> if (isCoding) "PROGRAMMING" else "UNIVERSITY"
                    }

                    generatedTasks.add(
                        StudyTaskEntity(
                            title = currentTopic.title,
                            description = "Focused deep study on ${currentTopic.subtopicsRaw.ifBlank { currentTopic.title }}",
                            category = category,
                            subject = request.selectedSubject,
                            priority = request.priority,
                            date = dateStr,
                            startTime = taskStart,
                            endTime = taskEnd,
                            durationMinutes = sessionDuration,
                            isCompleted = false,
                            materialId = currentTopic.materialId,
                            topicId = currentTopic.id,
                            taskType = taskType,
                            learningObjective = "Master core concepts of ${currentTopic.title} and apply practical exercises.",
                            estimatedDifficulty = currentTopic.difficulty,
                            isAIGenerated = true
                        )
                    )

                    currentMinute += sessionDuration + breakDuration
                    topicQueue.removeAt(0)
                }
            }

            cal.add(Calendar.DAY_OF_YEAR, 1)
            dayCounter++
        }

        return Result.success(generatedTasks)
    }

    override suspend fun askAssistant(
        prompt: String,
        context: AssistantContext
    ): String {
        delay(600)
        val lower = prompt.lowercase()

        return when {
            lower.contains("next") || lower.contains("what should i study") -> {
                val nextTask = context.pendingTasks.firstOrNull()
                if (nextTask != null) {
                    "Hi ${context.userName}! Based on your current plan, your next priority is **${nextTask.title}** (${nextTask.subject}), scheduled at ${nextTask.startTime}. Focus on: ${nextTask.description.ifBlank { "Core concepts and practice" }}. Taking 45 minutes of quiet focus now will keep you right on track!"
                } else {
                    "You've completed all scheduled tasks for today, ${context.userName}! Wonderful work. You can do a gentle 20-minute revision of yesterday's material, or start a preview session of tomorrow's topics."
                }
            }
            lower.contains("finish") || lower.contains("friday") || lower.contains("deadline") -> {
                val pendingCount = context.pendingTasks.size
                val estimatedHours = pendingCount * 0.75
                "Looking at your pace, you have $pendingCount upcoming sessions (~${String.format(Locale.US, "%.1f", estimatedHours)} hours). If you stick to your daily goal of ${String.format(Locale.US, "%.1f", context.availableHoursToday)} hours, you will comfortably finish ahead of your target date without cramming!"
            }
            lower.contains("reorganize") || lower.contains("missed") -> {
                "Don't worry about missed sessions at all! Gentle consistency is what builds mastery. I can redistribute your pending tasks into your evening available slots without overloading your schedule. Head to 'Create My Schedule' or click 'Reschedule Missed' on your Daily Planner."
            }
            lower.contains("revise") || lower.contains("revision") -> {
                val completed = context.completedTasksToday
                if (completed.isNotEmpty()) {
                    "Today you worked on: ${completed.joinToString { it.title }}. For tonight's active recall, spend 15 minutes explaining these concepts out loud or writing 3 key flashcards."
                } else {
                    "For optimal retention (Spaced Repetition), spend 20 minutes revisiting the fundamentals of your primary subject: active recall on syntax, data flow, and error handling."
                }
            }
            lower.contains("two hours") || lower.contains("2 hours") || lower.contains("prioritize") -> {
                "With 2 hours available today, here is your optimal gentle bloom routine:\n" +
                        "1. **45 min Focus:** High-priority theory/core concept.\n" +
                        "2. **15 min Refresh Break:** Step away, drink water.\n" +
                        "3. **45 min Active Practice:** Hands-on exercises or coding.\n" +
                        "4. **15 min Active Recall:** Write down what you learned from memory."
            }
            lower.contains("divide") || lower.contains("smaller") || lower.contains("chapter") -> {
                "To make any large chapter effortless, divide it into 3 micro-milestones:\n" +
                        "• **Stage 1 (Skim & Terminology):** 25 mins reading headers and definitions.\n" +
                        "• **Stage 2 (Deep Dive & Implementation):** 45 mins working through examples.\n" +
                        "• **Stage 3 (Summary & Edge Cases):** 30 mins solving 2-3 practice problems."
            }
            else -> {
                "I'm here to help you study gently and grow consistently, ${context.userName}. You have ${context.pendingTasks.size} pending tasks and ${context.completedTasksToday.size} completed sessions today. Ask me to prioritize your hours, summarize chapters, or reschedule when your day changes!"
            }
        }
    }

    private fun parseTopicsFromContent(content: String, subject: String): List<ExtractedTopic> {
        val lines = content.lines().filter { it.isNotBlank() }
        val topics = mutableListOf<ExtractedTopic>()
        var index = 1

        for (line in lines.take(10)) {
            val clean = line.replace(Regex("^[-*•0-9.]+\\s*"), "").trim()
            if (clean.length in 4..60) {
                topics.add(
                    ExtractedTopic(
                        title = clean,
                        subtopics = listOf("Core concepts", "Key terminology", "Practical exercise"),
                        difficulty = if (index % 3 == 0) "HARD" else if (index % 2 == 0) "MEDIUM" else "EASY",
                        estimatedMinutes = if (index % 3 == 0) 75 else 50,
                        isPrerequisite = index <= 2,
                        orderIndex = index++
                    )
                )
            }
        }

        if (topics.isEmpty()) {
            return generateDefaultTopicsForSubject(subject)
        }
        return topics
    }

    private fun generateDefaultTopicsForSubject(subject: String): List<ExtractedTopic> {
        return when {
            subject.contains("Django") -> listOf(
                ExtractedTopic("1. Django MVT & Project Setup", listOf("settings.py", "URL dispatchers", "virtual environments"), "EASY", 45, true, 1),
                ExtractedTopic("2. Models, Field Types & Migrations", listOf("Schema design", "Makemigrations", "SQL generation"), "MEDIUM", 60, true, 2),
                ExtractedTopic("3. QuerySets & Database Operations", listOf("Filtering", "Foreign keys", "Optimization"), "HARD", 75, true, 3),
                ExtractedTopic("4. Views & Dynamic Templates", listOf("Function views", "Class views", "Context passing"), "MEDIUM", 60, false, 4),
                ExtractedTopic("5. User Authentication & Permissions", listOf("LoginView", "Password hashing", "Decorators"), "MEDIUM", 60, false, 5),
                ExtractedTopic("6. Django REST Framework & APIs", listOf("Serializers", "Routers", "Status codes"), "HARD", 90, false, 6)
            )
            subject.contains("Python") -> listOf(
                ExtractedTopic("1. Control Flow & Data Structures", listOf("Lists, dicts, sets", "List comprehensions"), "EASY", 45, true, 1),
                ExtractedTopic("2. Functions, Args, & Closures", listOf("Keyword arguments", "Lambdas", "Decorators"), "MEDIUM", 60, true, 2),
                ExtractedTopic("3. Object-Oriented Python", listOf("Classes", "Inheritance", "Dunder methods"), "MEDIUM", 60, false, 3),
                ExtractedTopic("4. Error Handling & File I/O", listOf("Try/except/finally", "Context managers"), "EASY", 45, false, 4),
                ExtractedTopic("5. Generators & Itertools", listOf("Yield", "Memory efficiency", "Iterators"), "HARD", 75, false, 5)
            )
            subject.contains("Data Structure") || subject.contains("Algorithms") -> listOf(
                ExtractedTopic("1. Big-O Complexity & Array Basics", listOf("Time/Space bounds", "Two pointers"), "EASY", 45, true, 1),
                ExtractedTopic("2. Linked Lists & Stack Queues", listOf("Singly vs Doubly", "Monotonic stacks"), "MEDIUM", 60, true, 2),
                ExtractedTopic("3. Hash Tables & Frequency Maps", listOf("Collision handling", "Sliding window"), "MEDIUM", 60, false, 3),
                ExtractedTopic("4. Binary Trees & Graph Traversals", listOf("DFS, BFS", "Recursion", "Topological sort"), "HARD", 90, false, 4),
                ExtractedTopic("5. Dynamic Programming Essentials", listOf("Memoization", "Tabulation", "Knapsack"), "HARD", 90, false, 5)
            )
            else -> listOf(
                ExtractedTopic("1. Fundamentals & Core Definitions", listOf("Background overview", "Key terms"), "EASY", 45, true, 1),
                ExtractedTopic("2. Principles & Methodologies", listOf("Core theories", "Structured workflows"), "MEDIUM", 60, true, 2),
                ExtractedTopic("3. Detailed Implementation & Case Studies", listOf("Worked examples", "Common pitfalls"), "HARD", 75, false, 3),
                ExtractedTopic("4. Practice Problems & Application", listOf("Hands-on exercises", "Edge cases"), "MEDIUM", 60, false, 4),
                ExtractedTopic("5. Comprehensive Review & Self-Test", listOf("Active recall", "Summary checklist"), "EASY", 45, false, 5)
            )
        }
    }

    private fun timeStringToMinutes(time: String): Int {
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun minutesToTimeString(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
