package com.example.data.repository

import com.example.data.ai.AiRepository
import com.example.data.ai.AssistantContext
import com.example.data.ai.DocumentAnalysisResult
import com.example.data.ai.ScheduleRequest
import com.example.data.local.StudyBloomDatabase
import com.example.data.local.entity.AvailabilitySlotEntity
import com.example.data.local.entity.LearningMaterialEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LocalDataCounts(
    val tasksCount: Int = 0,
    val sessionsCount: Int = 0,
    val materialsCount: Int = 0,
    val topicsCount: Int = 0
)

class StudyBloomRepository(
    private val database: StudyBloomDatabase,
    private val aiRepository: AiRepository
) {
    private val userProfileDao = database.userProfileDao()
    private val materialDao = database.learningMaterialDao()
    private val topicDao = database.topicDao()
    private val taskDao = database.studyTaskDao()
    private val sessionDao = database.studySessionDao()
    private val slotDao = database.availabilitySlotDao()
    private val pendingAiRequestDao = database.pendingAiRequestDao()

    // --- Offline Queued AI Requests ---
    val pendingAiRequests: Flow<List<com.example.data.local.entity.PendingAiRequestEntity>> =
        pendingAiRequestDao.getAllRequests()

    val queuedAiCount: Flow<Int> = pendingAiRequestDao.getQueuedCountFlow()

    suspend fun queueAiRequest(
        type: String,
        title: String,
        prompt: String,
        relatedEntityId: Long? = null,
        initialPreview: String = ""
    ): Long {
        return pendingAiRequestDao.insertRequest(
            com.example.data.local.entity.PendingAiRequestEntity(
                requestType = type,
                title = title,
                prompt = prompt,
                relatedEntityId = relatedEntityId,
                status = "QUEUED",
                responsePreview = initialPreview
            )
        )
    }

    suspend fun getQueuedRequests(): List<com.example.data.local.entity.PendingAiRequestEntity> =
        pendingAiRequestDao.getQueuedRequests()

    suspend fun updateAiRequest(request: com.example.data.local.entity.PendingAiRequestEntity) =
        pendingAiRequestDao.updateRequest(request)

    suspend fun deleteAiRequest(id: Long) =
        pendingAiRequestDao.deleteRequest(id)

    suspend fun clearCompletedAiRequests() =
        pendingAiRequestDao.clearCompleted()

    // --- Profile & Preferences ---
    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getUserProfile()

    suspend fun getProfileOnce(): UserProfileEntity {
        return userProfileDao.getUserProfileOnce() ?: UserProfileEntity()
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        userProfileDao.insertProfile(profile)
    }

    suspend fun updateThemeMode(themeMode: String) {
        userProfileDao.updateThemeMode(themeMode)
    }

    suspend fun completeOnboarding(profile: UserProfileEntity) {
        userProfileDao.insertProfile(profile.copy(hasCompletedOnboarding = true))
    }

    suspend fun checkAndUpdateStreak() {
        val profile = getProfileOnce()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (profile.lastActiveDate != today) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

            val newStreak = if (profile.lastActiveDate == yesterday) {
                profile.currentStreakDays + 1
            } else if (profile.lastActiveDate.isBlank()) {
                1
            } else {
                1
            }
            userProfileDao.updateStreak(newStreak, today)
        }
    }

    // --- Materials & Topics ---
    val allMaterials: Flow<List<LearningMaterialEntity>> = materialDao.getAllMaterials()

    fun getMaterialById(id: Long): Flow<LearningMaterialEntity?> = materialDao.getMaterialById(id)

    fun getTopicsForMaterial(materialId: Long): Flow<List<TopicEntity>> = topicDao.getTopicsForMaterial(materialId)

    suspend fun addMaterialAndAnalyze(
        title: String,
        fileType: String,
        uriString: String,
        fileSizeFormatted: String,
        contentSample: String
    ): Result<Long> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val initialId = materialDao.insertMaterial(
            LearningMaterialEntity(
                title = title,
                fileType = fileType,
                uriString = uriString,
                uploadDate = today,
                fileSizeFormatted = fileSizeFormatted,
                status = "PROCESSING"
            )
        )

        val analysisResult = aiRepository.analyzeDocument(title, fileType, contentSample)
        return if (analysisResult.isSuccess) {
            val data = analysisResult.getOrThrow()
            materialDao.updateMaterial(
                LearningMaterialEntity(
                    id = initialId,
                    title = title,
                    fileType = fileType,
                    uriString = uriString,
                    uploadDate = today,
                    fileSizeFormatted = fileSizeFormatted,
                    extractedText = contentSample.take(500),
                    summary = data.summary,
                    mainSubject = data.subject,
                    estimatedDifficulty = data.estimatedDifficulty,
                    estimatedTotalHours = data.estimatedTotalHours,
                    status = "READY"
                )
            )

            val topicEntities = data.topics.map {
                TopicEntity(
                    materialId = initialId,
                    title = it.title,
                    subtopicsRaw = it.subtopics.joinToString(", "),
                    difficulty = it.difficulty,
                    estimatedMinutes = it.estimatedMinutes,
                    isPrerequisite = it.isPrerequisite,
                    orderIndex = it.orderIndex
                )
            }
            topicDao.insertTopics(topicEntities)
            Result.success(initialId)
        } else {
            materialDao.updateMaterial(
                LearningMaterialEntity(
                    id = initialId,
                    title = title,
                    fileType = fileType,
                    uriString = uriString,
                    uploadDate = today,
                    fileSizeFormatted = fileSizeFormatted,
                    status = "ERROR"
                )
            )
            Result.failure(analysisResult.exceptionOrNull() ?: Exception("Analysis failed"))
        }
    }

    suspend fun deleteMaterial(id: Long) {
        topicDao.deleteTopicsForMaterial(id)
        materialDao.deleteMaterialById(id)
    }

    suspend fun updateTopic(topic: TopicEntity) {
        topicDao.updateTopic(topic)
    }

    suspend fun addTopic(topic: TopicEntity): Long {
        return topicDao.insertTopic(topic)
    }

    suspend fun deleteTopic(id: Long) {
        topicDao.deleteTopicById(id)
    }

    // --- Study Tasks ---
    fun getTasksForDate(date: String): Flow<List<StudyTaskEntity>> = taskDao.getTasksForDate(date)

    fun getTasksBetweenDates(startDate: String, endDate: String): Flow<List<StudyTaskEntity>> =
        taskDao.getTasksBetweenDates(startDate, endDate)

    val pendingTasks: Flow<List<StudyTaskEntity>> = taskDao.getAllPendingTasks()

    suspend fun insertTask(task: StudyTaskEntity): Long = taskDao.insertTask(task)

    suspend fun insertTasks(tasks: List<StudyTaskEntity>) = taskDao.insertTasks(tasks)

    suspend fun updateTask(task: StudyTaskEntity) = taskDao.updateTask(task)

    suspend fun setTaskCompleted(id: Long, completed: Boolean) {
        taskDao.setTaskCompleted(id, completed)
        if (completed) {
            val task = taskDao.getTaskByIdOnce(id)
            if (task != null) {
                sessionDao.insertSession(
                    StudySessionEntity(
                        taskId = task.id,
                        taskTitle = task.title,
                        subject = task.subject,
                        durationMinutes = task.durationMinutes,
                        date = task.date,
                        completedSuccessfully = true,
                        sessionType = "POMODORO"
                    )
                )
            }
        }
    }

    suspend fun setTaskSkipped(id: Long, skipped: Boolean) = taskDao.setTaskSkipped(id, skipped)

    suspend fun rescheduleTask(id: Long, newDate: String, newStart: String, newEnd: String) =
        taskDao.rescheduleTask(id, newDate, newStart, newEnd)

    suspend fun deleteTask(id: Long) = taskDao.deleteTaskById(id)

    suspend fun recordCompletedTaskSession(
        taskId: Long,
        plannedDurationMinutes: Int,
        actualDurationMinutes: Int,
        overtimeMinutes: Int,
        pausedMinutes: Int,
        startTimestamp: Long,
        endTimestamp: Long,
        markTaskAsCompleted: Boolean
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val task = taskDao.getTaskByIdOnce(taskId)

        if (task != null) {
            val updatedTask = task.copy(
                actualDurationMinutes = (task.actualDurationMinutes + actualDurationMinutes),
                overtimeMinutes = (task.overtimeMinutes + overtimeMinutes),
                isCompleted = if (markTaskAsCompleted) true else task.isCompleted,
                startedAtTimestamp = task.startedAtTimestamp ?: startTimestamp,
                completedAtTimestamp = if (markTaskAsCompleted) endTimestamp else task.completedAtTimestamp
            )
            taskDao.updateTask(updatedTask)

            sessionDao.insertSession(
                StudySessionEntity(
                    taskId = task.id,
                    taskTitle = task.title,
                    subject = task.subject,
                    durationMinutes = actualDurationMinutes,
                    plannedDurationMinutes = plannedDurationMinutes,
                    overtimeMinutes = overtimeMinutes,
                    pausedMinutes = pausedMinutes,
                    date = today,
                    timestamp = endTimestamp,
                    completedSuccessfully = true,
                    sessionType = "TASK_CLOCK"
                )
            )

            val currentProfile = getProfileOnce()
            userProfileDao.insertProfile(
                currentProfile.copy(
                    completedSessionsCount = currentProfile.completedSessionsCount + 1
                )
            )
        }
    }

    suspend fun getLocalDataSummary(): LocalDataCounts {
        return LocalDataCounts(
            tasksCount = taskDao.getTasksCount(),
            sessionsCount = sessionDao.getSessionsCount(),
            materialsCount = materialDao.getMaterialsCount(),
            topicsCount = topicDao.getTopicsCount()
        )
    }

    suspend fun markAccountReminderSeen() {
        val currentProfile = getProfileOnce()
        userProfileDao.insertProfile(currentProfile.copy(hasSeenAccountReminder = true))
    }

    // --- Smart Rescheduling ---
    suspend fun smartRescheduleMissedTasks(targetDate: String): Int {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val overdueTasks = taskDao.getTasksBetweenDatesOnce("2000-01-01", today)
            .filter { !it.isCompleted && !it.isSkipped && it.date < today }

        var count = 0
        var currentHour = 18
        for (task in overdueTasks) {
            val newStart = String.format(Locale.US, "%02d:00", currentHour)
            val newEnd = String.format(Locale.US, "%02d:45", currentHour)
            taskDao.rescheduleTask(task.id, targetDate, newStart, newEnd)
            currentHour = if (currentHour < 22) currentHour + 1 else 18
            count++
        }
        return count
    }

    // --- Sessions & Analytics ---
    val allSessions: Flow<List<StudySessionEntity>> = sessionDao.getAllSessions()

    fun getSessionsForDate(date: String): Flow<List<StudySessionEntity>> = sessionDao.getSessionsForDate(date)

    fun getSessionsBetweenDates(start: String, end: String): Flow<List<StudySessionEntity>> =
        sessionDao.getSessionsBetweenDates(start, end)

    fun getTotalMinutesForDate(date: String): Flow<Int?> = sessionDao.getTotalMinutesForDate(date)

    val totalMinutesAllTime: Flow<Int?> = sessionDao.getTotalMinutesAllTime()

    suspend fun recordFocusSession(
        taskId: Long?,
        taskTitle: String,
        subject: String,
        durationMinutes: Int
    ): Long {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return sessionDao.insertSession(
            StudySessionEntity(
                taskId = taskId,
                taskTitle = taskTitle,
                subject = subject,
                durationMinutes = durationMinutes,
                date = today,
                completedSuccessfully = true,
                sessionType = "POMODORO"
            )
        )
    }

    // --- Availability Slots ---
    val availabilitySlots: Flow<List<AvailabilitySlotEntity>> = slotDao.getAllSlots()

    suspend fun getAllSlotsOnce(): List<AvailabilitySlotEntity> = slotDao.getAllSlotsOnce()

    suspend fun updateSlot(slot: AvailabilitySlotEntity) = slotDao.updateSlot(slot)

    suspend fun insertSlot(slot: AvailabilitySlotEntity) = slotDao.insertSlot(slot)

    suspend fun deleteSlot(id: Long) = slotDao.deleteSlotById(id)

    // --- AI Capabilities ---
    suspend fun generateSchedule(request: ScheduleRequest): Result<List<StudyTaskEntity>> {
        return aiRepository.generateSchedule(request)
    }

    suspend fun askAssistant(prompt: String, context: AssistantContext): String {
        return aiRepository.askAssistant(prompt, context)
    }
}
