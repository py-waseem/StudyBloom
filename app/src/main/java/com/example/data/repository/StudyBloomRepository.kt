package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.ai.AiRepository
import com.example.data.ai.AssistantContext
import com.example.data.ai.MultiGoalScheduleRequest
import com.example.data.ai.MultiGoalScheduleResult
import com.example.data.ai.ScheduleRequest
import com.example.data.local.StudyBloomDatabase
import com.example.data.local.entity.AvailabilitySlotEntity
import com.example.data.local.entity.GoalOwnership
import com.example.data.local.entity.LearningMaterialEntity
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LocalDataCounts(
    val tasksCount: Int = 0,
    val sessionsCount: Int = 0,
    val materialsCount: Int = 0,
    val topicsCount: Int = 0,
    val goalsCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val goalDao = database.studyGoalDao()

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

    val currentOwnerIdFlow: Flow<String> = userProfile
        .map { GoalOwnership.ownerIdForProfile(it) }
        .distinctUntilChanged()

    suspend fun getProfileOnce(): UserProfileEntity {
        return userProfileDao.getUserProfileOnce() ?: UserProfileEntity()
    }

    suspend fun getCurrentOwnerIdOnce(): String {
        return GoalOwnership.ownerIdForProfile(userProfileDao.getUserProfileOnce())
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
        if (profile.lastActiveDate.isNotBlank() && profile.lastActiveDate != today) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            if (profile.lastActiveDate != yesterday && profile.currentStreakDays > 0) {
                userProfileDao.updateStreak(0, profile.lastActiveDate)
            }
        }
    }

    suspend fun recordStudyActivityToday() {
        val profile = getProfileOnce()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (profile.lastActiveDate != today) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            val newStreak = if (profile.lastActiveDate == yesterday) {
                profile.currentStreakDays + 1
            } else {
                1
            }
            userProfileDao.updateStreak(newStreak, today)
        }
    }

    // --- Study Goals (Multiple Daily Study Goals with Owner Isolation) ---
    val allGoals: Flow<List<StudyGoalEntity>> = currentOwnerIdFlow.flatMapLatest { ownerId ->
        goalDao.getAllGoalsForOwner(ownerId)
    }

    val activeGoals: Flow<List<StudyGoalEntity>> = currentOwnerIdFlow.flatMapLatest { ownerId ->
        goalDao.getActiveGoalsForOwner(ownerId)
    }

    fun getGoalsForDate(date: String): Flow<List<StudyGoalEntity>> =
        currentOwnerIdFlow.flatMapLatest { ownerId ->
            goalDao.getGoalsForOwnerAndDate(ownerId, date)
        }

    suspend fun getAllGoalsForCurrentOwnerOnce(): List<StudyGoalEntity> {
        return goalDao.getAllGoalsForOwnerOnce(getCurrentOwnerIdOnce())
    }

    suspend fun getGoalsForDateOnce(date: String): List<StudyGoalEntity> {
        return goalDao.getGoalsForOwnerAndDateOnce(getCurrentOwnerIdOnce(), date)
    }

    suspend fun getGoalByIdForCurrentOwnerOnce(goalId: Long): StudyGoalEntity? {
        return goalDao.getGoalByIdAndOwnerOnce(goalId, getCurrentOwnerIdOnce())
    }

    suspend fun createGoal(
        title: String,
        description: String = "",
        subject: String = "General",
        category: String = "PROGRAMMING",
        estimatedMinutes: Int = 60,
        difficulty: String = "MEDIUM",
        priority: String = "MEDIUM",
        preferredDate: String,
        deadlineDate: String = ""
    ): Long {
        val ownerId = getCurrentOwnerIdOnce()
        val now = System.currentTimeMillis()
        return goalDao.insertGoal(
            StudyGoalEntity(
                ownerId = ownerId,
                title = title.trim(),
                description = description.trim(),
                subject = subject.trim().ifBlank { "General" },
                category = category,
                estimatedMinutes = estimatedMinutes.coerceAtLeast(15),
                actualMinutes = 0,
                difficulty = difficulty,
                priority = priority,
                preferredDate = preferredDate,
                deadlineDate = deadlineDate,
                isCompleted = false,
                isSynced = false,
                createdAtTimestamp = now,
                updatedAtTimestamp = now
            )
        )
    }

    suspend fun updateGoal(goal: StudyGoalEntity): Boolean {
        val ownerId = getCurrentOwnerIdOnce()
        val existing = goalDao.getGoalByIdAndOwnerOnce(goal.id, ownerId) ?: return false
        val updated = existing.copy(
            title = goal.title.trim(),
            description = goal.description.trim(),
            subject = goal.subject.trim().ifBlank { "General" },
            category = goal.category,
            estimatedMinutes = goal.estimatedMinutes.coerceAtLeast(15),
            difficulty = goal.difficulty,
            priority = goal.priority,
            preferredDate = goal.preferredDate,
            deadlineDate = goal.deadlineDate,
            isCompleted = goal.isCompleted,
            isSynced = false,
            updatedAtTimestamp = System.currentTimeMillis()
        )
        goalDao.updateGoal(updated)
        return true
    }

    suspend fun setGoalCompleted(goalId: Long, completed: Boolean) {
        val ownerId = getCurrentOwnerIdOnce()
        goalDao.setGoalCompleted(goalId, ownerId, completed, System.currentTimeMillis())
    }

    suspend fun rescheduleGoal(goalId: Long, newPreferredDate: String, moveUncompletedTasks: Boolean = true): Boolean {
        val ownerId = getCurrentOwnerIdOnce()
        return database.withTransaction {
            val existing = goalDao.getGoalByIdAndOwnerOnce(goalId, ownerId) ?: return@withTransaction false
            goalDao.updateGoal(
                existing.copy(
                    preferredDate = newPreferredDate,
                    isSynced = false,
                    updatedAtTimestamp = System.currentTimeMillis()
                )
            )
            if (moveUncompletedTasks) {
                taskDao.rescheduleUncompletedTasksForGoal(goalId, newPreferredDate)
            }
            true
        }
    }

    /**
     * Deletes ONLY the specified goal owned by the current user/guest.
     * - Timer history (study_sessions) is NEVER deleted.
     * - Completed tasks are NEVER deleted (only unlinked with goalId = NULL).
     * - Uncompleted scheduled tasks for this goal are safely unlinked (goalId = NULL) by default,
     *   or removed only if removeUncompletedTasks is explicitly true.
     * - Unrelated tasks and other goals remain untouched.
     */
    suspend fun deleteGoal(goalId: Long, removeUncompletedTasks: Boolean = false): Boolean {
        val ownerId = getCurrentOwnerIdOnce()
        return database.withTransaction {
            goalDao.getGoalByIdAndOwnerOnce(goalId, ownerId) ?: return@withTransaction false
            if (removeUncompletedTasks) {
                taskDao.deleteUncompletedTasksForGoal(goalId)
            }
            // Any remaining (completed or kept) tasks become standalone tasks with goalId = NULL
            taskDao.unlinkTasksFromGoal(goalId)
            goalDao.deleteGoalByIdAndOwner(goalId, ownerId) > 0
        }
    }

    suspend fun migrateGuestGoalsToAccount(email: String): Int {
        val targetOwnerId = "user:${email.trim().lowercase()}"
        val now = System.currentTimeMillis()
        return database.withTransaction {
            val count = goalDao.reassignGoalsOwner(GoalOwnership.GUEST_OWNER_ID, targetOwnerId, now)
            goalDao.markGoalsSyncedForOwner(targetOwnerId, now)
            count
        }
    }

    suspend fun syncGoalsForCurrentOwner(): Int {
        val ownerId = getCurrentOwnerIdOnce()
        val unsynced = goalDao.getUnsyncedGoalsForOwner(ownerId)
        if (unsynced.isNotEmpty()) {
            goalDao.markGoalsSyncedForOwner(ownerId, System.currentTimeMillis())
        }
        return unsynced.size
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
    val allTasks: Flow<List<StudyTaskEntity>> = taskDao.getAllTasksFlow()

    fun getTasksForDate(date: String): Flow<List<StudyTaskEntity>> = taskDao.getTasksForDate(date)

    suspend fun getTasksForDateOnce(date: String): List<StudyTaskEntity> = taskDao.getTasksForDateOnce(date)

    fun getTasksBetweenDates(startDate: String, endDate: String): Flow<List<StudyTaskEntity>> =
        taskDao.getTasksBetweenDates(startDate, endDate)

    suspend fun getTasksBetweenDatesOnce(startDate: String, endDate: String): List<StudyTaskEntity> =
        taskDao.getTasksBetweenDatesOnce(startDate, endDate)

    val pendingTasks: Flow<List<StudyTaskEntity>> = taskDao.getAllPendingTasks()

    suspend fun insertTask(task: StudyTaskEntity): Long = taskDao.insertTask(task)

    suspend fun insertTasks(tasks: List<StudyTaskEntity>) = taskDao.insertTasks(tasks)

    suspend fun updateTask(task: StudyTaskEntity) = taskDao.updateTask(task)

    suspend fun setTaskCompleted(id: Long, completed: Boolean) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val task = taskDao.getTaskByIdOnce(id) ?: return@withTransaction
            if (task.isCompleted == completed) return@withTransaction

            taskDao.setTaskCompleted(id, completed)
            if (completed) {
                val existingSessionsForTask = sessionDao.getSessionsCountForTask(task.id)
                val submissionKey = "TASK_COMPLETE:${task.id}"
                val alreadySubmitted = sessionDao.countBySubmissionKey(submissionKey) > 0

                if (existingSessionsForTask == 0 && task.actualDurationMinutes == 0 && !alreadySubmitted) {
                    val creditedMinutes = task.durationMinutes.coerceAtLeast(1)
                    taskDao.updateTask(
                        task.copy(
                            isCompleted = true,
                            actualDurationMinutes = creditedMinutes,
                            completedAtTimestamp = task.completedAtTimestamp ?: now
                        )
                    )
                    sessionDao.insertSession(
                        StudySessionEntity(
                            taskId = task.id,
                            goalId = task.goalId,
                            sessionSubmissionKey = submissionKey,
                            taskTitle = task.title,
                            subject = task.subject,
                            durationMinutes = creditedMinutes,
                            plannedDurationMinutes = task.durationMinutes,
                            date = task.date,
                            timestamp = now,
                            completedSuccessfully = true,
                            sessionType = "POMODORO"
                        )
                    )
                    if (task.goalId != null) {
                        goalDao.incrementGoalActualMinutes(task.goalId, creditedMinutes, now)
                    }
                }
                recordStudyActivityToday()
            }
        }
    }

    suspend fun setTaskSkipped(id: Long, skipped: Boolean) = taskDao.setTaskSkipped(id, skipped)

    suspend fun rescheduleTask(id: Long, newDate: String, newStart: String, newEnd: String) =
        taskDao.rescheduleTask(id, newDate, newStart, newEnd)

    suspend fun deleteTask(id: Long) = taskDao.deleteTaskById(id)

    suspend fun ensureActiveTasksForToday() {
        // Purge only confirmed untouched demo starter tasks while preserving all genuine user tasks
        taskDao.deleteDemoStarterTasks()
    }

    suspend fun recordCompletedTaskSession(
        taskId: Long,
        plannedDurationMinutes: Int,
        actualDurationMinutes: Int,
        overtimeMinutes: Int,
        pausedMinutes: Int,
        startTimestamp: Long,
        endTimestamp: Long,
        markTaskAsCompleted: Boolean,
        submissionKey: String? = null
    ): Boolean {
        if (taskId <= 0L || actualDurationMinutes <= 0) return false
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val effectiveKey = submissionKey ?: if (startTimestamp > 0L) "TASK_CLOCK:$taskId:$startTimestamp" else null

        return database.withTransaction {
            if (effectiveKey != null && sessionDao.countBySubmissionKey(effectiveKey) > 0) {
                return@withTransaction false
            }

            val task = taskDao.getTaskByIdOnce(taskId) ?: return@withTransaction false
            val updatedTask = task.copy(
                actualDurationMinutes = task.actualDurationMinutes + actualDurationMinutes,
                overtimeMinutes = task.overtimeMinutes + overtimeMinutes,
                isCompleted = if (markTaskAsCompleted) true else task.isCompleted,
                startedAtTimestamp = task.startedAtTimestamp ?: startTimestamp,
                completedAtTimestamp = if (markTaskAsCompleted) endTimestamp else task.completedAtTimestamp
            )
            taskDao.updateTask(updatedTask)

            sessionDao.insertSession(
                StudySessionEntity(
                    taskId = task.id,
                    goalId = task.goalId,
                    sessionSubmissionKey = effectiveKey,
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

            if (task.goalId != null) {
                goalDao.incrementGoalActualMinutes(task.goalId, actualDurationMinutes, endTimestamp)
            }

            val currentProfile = getProfileOnce()
            userProfileDao.insertProfile(
                currentProfile.copy(
                    completedSessionsCount = currentProfile.completedSessionsCount + 1
                )
            )
            recordStudyActivityToday()
            true
        }
    }

    suspend fun getLocalDataSummary(ownerIdOverride: String? = null): LocalDataCounts {
        val ownerId = ownerIdOverride ?: getCurrentOwnerIdOnce()
        return LocalDataCounts(
            tasksCount = taskDao.getTasksCount(),
            sessionsCount = sessionDao.getSessionsCount(),
            materialsCount = materialDao.getMaterialsCount(),
            topicsCount = topicDao.getTopicsCount(),
            goalsCount = goalDao.getGoalsCountForOwner(ownerId)
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
        durationMinutes: Int,
        markTaskCompleted: Boolean = taskId != null,
        submissionKey: String? = null
    ): Long {
        if (durationMinutes <= 0) return -1L
        val now = System.currentTimeMillis()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val effectiveKey = submissionKey ?: "POMODORO:${taskId ?: 0L}:$now"

        return database.withTransaction {
            if (sessionDao.countBySubmissionKey(effectiveKey) > 0) {
                return@withTransaction -1L
            }

            val task = if (taskId != null) taskDao.getTaskByIdOnce(taskId) else null
            val goalId = task?.goalId

            val sessionId = sessionDao.insertSession(
                StudySessionEntity(
                    taskId = taskId,
                    goalId = goalId,
                    sessionSubmissionKey = effectiveKey,
                    taskTitle = taskTitle,
                    subject = subject,
                    durationMinutes = durationMinutes,
                    plannedDurationMinutes = task?.durationMinutes ?: durationMinutes,
                    date = today,
                    timestamp = now,
                    completedSuccessfully = true,
                    sessionType = "POMODORO"
                )
            )

            if (task != null) {
                taskDao.updateTask(
                    task.copy(
                        actualDurationMinutes = task.actualDurationMinutes + durationMinutes,
                        isCompleted = if (markTaskCompleted) true else task.isCompleted,
                        completedAtTimestamp = if (markTaskCompleted) (task.completedAtTimestamp ?: now) else task.completedAtTimestamp
                    )
                )
            }

            if (goalId != null) {
                goalDao.incrementGoalActualMinutes(goalId, durationMinutes, now)
            }

            recordStudyActivityToday()
            sessionId
        }
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

    suspend fun generateMultiGoalSchedule(request: MultiGoalScheduleRequest): Result<MultiGoalScheduleResult> {
        return aiRepository.generateMultiGoalSchedule(request)
    }

    suspend fun askAssistant(prompt: String, context: AssistantContext): String {
        return aiRepository.askAssistant(prompt, context)
    }
}
