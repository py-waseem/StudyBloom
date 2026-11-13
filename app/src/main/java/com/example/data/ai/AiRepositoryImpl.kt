package com.example.data.ai

import com.example.data.local.entity.AvailabilitySlotEntity
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.data.local.entity.TopicEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

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

    private data class TimeInterval(val start: Int, val end: Int) {
        val duration: Int get() = (end - start).coerceAtLeast(0)
    }

    private data class GoalWorkItem(
        val goal: StudyGoalEntity,
        val requestedMinutes: Int,
        var remainingMinutes: Int,
        var scheduledMinutes: Int = 0,
        var totalPartsCreated: Int = 0
    )

    override suspend fun generateMultiGoalSchedule(
        request: MultiGoalScheduleRequest
    ): Result<MultiGoalScheduleResult> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startCal = Calendar.getInstance()
        val endCal = Calendar.getInstance()

        val parsedStart = try {
            dateFormat.parse(request.startDate)
        } catch (_: Exception) {
            null
        } ?: Date()
        startCal.time = parsedStart

        val effectiveStartDate = dateFormat.format(startCal.time)
        val parsedEnd = try {
            dateFormat.parse(request.targetEndDate)
        } catch (_: Exception) {
            null
        } ?: parsedStart
        endCal.time = if (parsedEnd.before(parsedStart)) parsedStart else parsedEnd
        val effectiveEndDate = dateFormat.format(endCal.time)
        val isMultiDayHorizon = effectiveEndDate > effectiveStartDate

        val sessionDuration = if (request.sessionDurationMinutes in 15..120) request.sessionDurationMinutes else 45
        val breakDuration = if (request.breakDurationMinutes in 5..30) request.breakDurationMinutes else 15
        val dailyCapMinutes = request.dailyGoalMinutes.coerceAtLeast(30)

        val activeGoals = request.goals.filter { !it.isCompleted }
        val nonSkippedExisting = request.existingTasks.filter { !it.isSkipped }

        val workItems = activeGoals
            .sortedWith(
                compareBy<StudyGoalEntity>(
                    { if (it.preferredDate.isBlank()) "9999-12-31" else it.preferredDate },
                    { if (it.deadlineDate.isBlank()) "9999-12-31" else it.deadlineDate },
                    { priorityRank(it.priority) },
                    { difficultyRank(it.difficulty) },
                    { it.createdAtTimestamp },
                    { it.id }
                )
            )
            .map { goal ->
                val alreadyPlannedActive = nonSkippedExisting
                    .filter { it.goalId == goal.id && !it.isCompleted }
                    .sumOf { it.durationMinutes }
                val alreadyCovered = (goal.actualMinutes + alreadyPlannedActive).coerceAtMost(goal.estimatedMinutes)
                val needed = (goal.estimatedMinutes - alreadyCovered).coerceAtLeast(0)
                GoalWorkItem(
                    goal = goal,
                    requestedMinutes = needed,
                    remainingMinutes = needed
                )
            }

        val generatedTasks = mutableListOf<StudyTaskEntity>()
        var totalAvailableCapacityMinutes = 0

        val dayCursor = startCal.clone() as Calendar
        var daySafetyCounter = 0
        val maxHorizonDays = 31

        while (daySafetyCounter < maxHorizonDays) {
            val dateStr = dateFormat.format(dayCursor.time)
            if (dateStr > effectiveEndDate) break

            val dayOfWeek = calendarDayOfWeekToIso(dayCursor.get(Calendar.DAY_OF_WEEK))
            val existingTasksOnDay = nonSkippedExisting.filter { it.date == dateStr }
            val existingMinutesOnDay = existingTasksOnDay.sumOf { it.durationMinutes }
            var remainingDailyBudget = (dailyCapMinutes - existingMinutesOnDay).coerceAtLeast(0)

            val occupiedIntervals = existingTasksOnDay.mapNotNull { task ->
                val s = timeStringToMinutes(task.startTime)
                val e = timeStringToMinutes(task.endTime)
                if (e > s) TimeInterval(s, e) else null
            }

            val configuredSlotsForDay = request.availabilitySlots.filter { it.dayOfWeek == dayOfWeek }
            val rawWindows = if (configuredSlotsForDay.isNotEmpty()) {
                configuredSlotsForDay.filter { it.isEnabled }.mapNotNull { slot ->
                    val s = timeStringToMinutes(slot.startTime)
                    val e = timeStringToMinutes(slot.endTime)
                    if (e > s) TimeInterval(s, e) else null
                }
            } else {
                listOf(
                    TimeInterval(7 * 60, 10 * 60),
                    TimeInterval(13 * 60, 16 * 60 + 30),
                    TimeInterval(18 * 60 + 30, 22 * 60 + 30)
                )
            }

            val freeSegments = subtractIntervals(mergeIntervals(rawWindows), occupiedIntervals)
            val usableWallClockMinutes = freeSegments.sumOf { it.duration }
            totalAvailableCapacityMinutes += minOf(remainingDailyBudget, usableWallClockMinutes)

            var lastScheduledGoalId: Long? = null
            var lastScheduledDifficulty: String? = null

            for (segment in freeSegments) {
                var cursor = segment.start
                while (remainingDailyBudget > 0 && cursor < segment.end) {
                    val eligibleItems = workItems.filter { item ->
                        item.remainingMinutes > 0 &&
                            (!isMultiDayHorizon || item.goal.preferredDate.isBlank() || item.goal.preferredDate <= dateStr)
                    }
                    if (eligibleItems.isEmpty()) break

                    val chosen = pickNextGoalWorkItem(
                        eligibleItems = eligibleItems,
                        lastGoalId = lastScheduledGoalId,
                        lastDifficulty = lastScheduledDifficulty
                    ) ?: break

                    val availableInSegment = segment.end - cursor
                    val minViableChunk = minOf(chosen.remainingMinutes, 15)
                    if (availableInSegment < minViableChunk) break
                    if (remainingDailyBudget < minViableChunk) break

                    val chunkDuration = minOf(
                        chosen.remainingMinutes,
                        sessionDuration,
                        remainingDailyBudget,
                        availableInSegment
                    )
                    if (chunkDuration <= 0) break

                    val taskStart = minutesToTimeString(cursor)
                    val taskEnd = minutesToTimeString(cursor + chunkDuration)

                    val partNumber = chosen.totalPartsCreated + 1
                    val willSplitIntoMultiple = partNumber > 1 || chosen.remainingMinutes > chunkDuration
                    val taskTitle = if (willSplitIntoMultiple) {
                        "${chosen.goal.title} (Part $partNumber)"
                    } else {
                        chosen.goal.title
                    }

                    val taskType = when (chosen.goal.category) {
                        "CODING_PRACTICE", "PROGRAMMING" -> "CODING"
                        "REVISION" -> "REVISION"
                        else -> "STUDY"
                    }

                    generatedTasks.add(
                        StudyTaskEntity(
                            goalId = chosen.goal.id,
                            title = taskTitle,
                            description = chosen.goal.description.ifBlank {
                                "Focused session for ${chosen.goal.subject}"
                            },
                            category = chosen.goal.category,
                            subject = chosen.goal.subject,
                            priority = chosen.goal.priority,
                            date = dateStr,
                            startTime = taskStart,
                            endTime = taskEnd,
                            durationMinutes = chunkDuration,
                            isCompleted = false,
                            taskType = taskType,
                            learningObjective = "Complete ${chunkDuration}m milestone toward '${chosen.goal.title}'",
                            estimatedDifficulty = chosen.goal.difficulty,
                            isAIGenerated = true
                        )
                    )

                    chosen.remainingMinutes -= chunkDuration
                    chosen.scheduledMinutes += chunkDuration
                    chosen.totalPartsCreated = partNumber
                    remainingDailyBudget -= chunkDuration
                    cursor += chunkDuration + breakDuration
                    lastScheduledGoalId = chosen.goal.id
                    lastScheduledDifficulty = chosen.goal.difficulty
                }

                if (remainingDailyBudget <= 0) break
            }

            dayCursor.add(Calendar.DAY_OF_YEAR, 1)
            daySafetyCounter++
        }

        val allocations = workItems.map { item ->
            val status = when {
                item.remainingMinutes <= 0 -> "SCHEDULED"
                item.scheduledMinutes > 0 -> "PARTIAL"
                else -> "UNSCHEDULED"
            }
            ScheduledGoalAllocation(
                goalId = item.goal.id,
                goalTitle = item.goal.title,
                requestedMinutes = item.requestedMinutes,
                scheduledMinutes = item.scheduledMinutes,
                remainingMinutes = item.remainingMinutes,
                status = status
            )
        }

        val unscheduledGoals = workItems
            .filter { it.scheduledMinutes == 0 && it.remainingMinutes > 0 }
            .map { it.goal }
        val partialGoals = workItems
            .filter { it.scheduledMinutes > 0 && it.remainingMinutes > 0 }
            .map { it.goal }
        val overflowMinutes = allocations.sumOf { it.remainingMinutes }
        val totalRequestedMinutes = allocations.sumOf { it.requestedMinutes }
        val scheduledGoalIds = allocations
            .filter { it.scheduledMinutes > 0 }
            .map { it.goalId }
            .toSet()

        val suggestedDates = if (overflowMinutes > 0) {
            val extraDaysNeeded = ceil(overflowMinutes.toDouble() / dailyCapMinutes.toDouble()).toInt().coerceIn(1, 14)
            val suggestionCal = endCal.clone() as Calendar
            List(extraDaysNeeded) {
                suggestionCal.add(Calendar.DAY_OF_YEAR, 1)
                dateFormat.format(suggestionCal.time)
            }
        } else {
            emptyList()
        }

        val warningMessage = if (overflowMinutes > 0) {
            val affectedNames = (partialGoals + unscheduledGoals).joinToString(", ") { it.title }
            val hoursStr = formatDurationMinutes(overflowMinutes)
            "Capacity overflow ($hoursStr unscheduled): $affectedNames could not fully fit within your daily target or available slots. Consider distributing across ${suggestedDates.size} additional day(s)."
        } else {
            null
        }

        return Result.success(
            MultiGoalScheduleResult(
                tasks = generatedTasks,
                allocations = allocations,
                scheduledGoalIds = scheduledGoalIds,
                unscheduledGoals = unscheduledGoals,
                partialGoals = partialGoals,
                overflowMinutes = overflowMinutes,
                totalRequestedMinutes = totalRequestedMinutes,
                totalAvailableCapacityMinutes = totalAvailableCapacityMinutes,
                warningMessage = warningMessage,
                suggestedMultiDayDates = suggestedDates
            )
        )
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

    private fun priorityRank(priority: String): Int = when (priority.uppercase()) {
        "HIGH" -> 0
        "MEDIUM" -> 1
        "LOW" -> 2
        else -> 1
    }

    private fun difficultyRank(difficulty: String): Int = when (difficulty.uppercase()) {
        "HARD" -> 0
        "MEDIUM" -> 1
        "EASY" -> 2
        else -> 1
    }

    private fun calendarDayOfWeekToIso(calDay: Int): Int = when (calDay) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }

    private fun pickNextGoalWorkItem(
        eligibleItems: List<GoalWorkItem>,
        lastGoalId: Long?,
        lastDifficulty: String?
    ): GoalWorkItem? {
        if (eligibleItems.isEmpty()) return null
        val sorted = eligibleItems.sortedWith(
            compareBy<GoalWorkItem>(
                { priorityRank(it.goal.priority) },
                { if (it.goal.deadlineDate.isBlank()) "9999-12-31" else it.goal.deadlineDate },
                { if (it.goal.preferredDate.isBlank()) "9999-12-31" else it.goal.preferredDate },
                { difficultyRank(it.goal.difficulty) },
                { it.goal.createdAtTimestamp },
                { it.goal.id }
            )
        )
        val top = sorted.first()
        // Balance difficulty & variety within the same priority tier without letting lower-priority
        // goals displace higher-priority goals when capacity is tight.
        if (lastGoalId == top.goal.id && lastDifficulty == "HARD") {
            val topPriority = priorityRank(top.goal.priority)
            val alternate = sorted.firstOrNull { candidate ->
                candidate.goal.id != lastGoalId &&
                    priorityRank(candidate.goal.priority) == topPriority &&
                    candidate.goal.difficulty != "HARD"
            } ?: sorted.firstOrNull { candidate ->
                candidate.goal.id != lastGoalId &&
                    priorityRank(candidate.goal.priority) == topPriority
            }
            if (alternate != null) return alternate
        }
        return top
    }

    private fun mergeIntervals(intervals: List<TimeInterval>): List<TimeInterval> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.start }
        val merged = mutableListOf<TimeInterval>()
        var current = sorted.first()
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.start <= current.end) {
                current = TimeInterval(current.start, maxOf(current.end, next.end))
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private fun subtractIntervals(
        available: List<TimeInterval>,
        occupied: List<TimeInterval>
    ): List<TimeInterval> {
        if (available.isEmpty()) return emptyList()
        val mergedOccupied = mergeIntervals(occupied)
        if (mergedOccupied.isEmpty()) return available

        val free = mutableListOf<TimeInterval>()
        for (window in available) {
            var currentStart = window.start
            for (busy in mergedOccupied) {
                if (busy.end <= currentStart) continue
                if (busy.start >= window.end) break
                if (busy.start > currentStart) {
                    free.add(TimeInterval(currentStart, minOf(busy.start, window.end)))
                }
                currentStart = maxOf(currentStart, busy.end)
                if (currentStart >= window.end) break
            }
            if (currentStart < window.end) {
                free.add(TimeInterval(currentStart, window.end))
            }
        }
        return free.filter { it.duration > 0 }
    }

    private fun formatDurationMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
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
