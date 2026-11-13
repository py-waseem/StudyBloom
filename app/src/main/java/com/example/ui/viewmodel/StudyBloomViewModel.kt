package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiRepositoryImpl
import com.example.data.ai.AssistantContext
import com.example.data.ai.MultiGoalScheduleRequest
import com.example.data.ai.MultiGoalScheduleResult
import com.example.data.ai.ScheduleRequest
import com.example.data.local.StudyBloomDatabase
import com.example.data.local.entity.AvailabilitySlotEntity
import com.example.data.local.entity.LearningMaterialEntity
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.PendingAiRequestEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.StudyBloomRepository
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncResult
import com.example.data.sync.SyncState
import com.example.util.NetworkConnectivityMonitor
import com.example.util.NetworkState
import com.example.util.NotificationHelper
import com.example.util.PdfExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AssistantMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ScheduleWizardData(
    val step: Int = 1,
    val wizardMode: String = "GOALS", // "GOALS" or "MATERIAL"
    val selectedGoalIds: Set<Long> = emptySet(),
    val subject: String = "",
    val selectedMaterialId: Long? = null,
    val selectedTopics: List<TopicEntity> = emptyList(),
    val startDate: String = "",
    val targetEndDate: String = "",
    val dailyMinutesTarget: Int = 180,
    val sessionDuration: Int = 45,
    val breakDuration: Int = 15,
    val priority: String = "HIGH",
    val generatedPreview: List<StudyTaskEntity> = emptyList(),
    val multiGoalResult: MultiGoalScheduleResult? = null,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

class StudyBloomViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyBloomRepository

    // Online/Offline Network & Cloud Sync
    val networkMonitor = NetworkConnectivityMonitor(application)
    val networkState: StateFlow<NetworkState> = networkMonitor.networkState

    private val syncManager: SyncManager
    val syncState: StateFlow<SyncState>

    val pendingAiRequests: StateFlow<List<PendingAiRequestEntity>>

    // Guest Mode & Account Migration
    val authManager: com.example.data.auth.AuthManager
    val migrationState: StateFlow<com.example.data.auth.MigrationState>

    private val _showAccountReminderDialog = MutableStateFlow(false)
    val showAccountReminderDialog: StateFlow<Boolean> = _showAccountReminderDialog.asStateFlow()

    init {
        val database = StudyBloomDatabase.getInstance(application)
        val aiRepo = AiRepositoryImpl()
        repository = StudyBloomRepository(database, aiRepo)
        authManager = com.example.data.auth.AuthManager(application, repository)
        migrationState = authManager.migrationState

        syncManager = SyncManager(application, repository, aiRepo, networkMonitor)
        syncState = syncManager.syncState
        pendingAiRequests = repository.pendingAiRequests
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            repository.checkAndUpdateStreak()
        }
    }

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDate: String = dateFormat.format(Date())

    private val _selectedPlanDate = MutableStateFlow(todayDate)
    val selectedPlanDate: StateFlow<String> = _selectedPlanDate.asStateFlow()

    // Tasks for selected date in Plan screen
    private val _planDateTasks = MutableStateFlow<List<StudyTaskEntity>>(emptyList())
    val planDateTasks: StateFlow<List<StudyTaskEntity>> = _planDateTasks.asStateFlow()

    // Tasks for today
    val todayTasks: StateFlow<List<StudyTaskEntity>> = repository.getTasksForDate(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All tasks (used to derive goal scheduled/unscheduled state & planned minutes)
    val allTasks: StateFlow<List<StudyTaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Study Goals (owner-isolated for current guest or registered user)
    val allGoals: StateFlow<List<StudyGoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGoals: StateFlow<List<StudyGoalEntity>> = repository.activeGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val planDateGoals: StateFlow<List<StudyGoalEntity>> = _selectedPlanDate
        .flatMapLatest { date -> repository.getGoalsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayGoals: StateFlow<List<StudyGoalEntity>> = repository.getGoalsForDate(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All materials
    val allMaterials: StateFlow<List<LearningMaterialEntity>> = repository.allMaterials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All availability slots
    val availabilitySlots: StateFlow<List<AvailabilitySlotEntity>> = repository.availabilitySlots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All study sessions for progress
    val allSessions: StateFlow<List<StudySessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMinutesAllTime: StateFlow<Int?> = repository.totalMinutesAllTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Current navigation tab: "home", "plan", "materials", "progress", "profile"
    private val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Live Task Clock Timer (persistent timestamp-based)
    private val taskTimerManager = com.example.util.TaskTimerManager.getInstance(application)
    val liveTimerSnapshot: StateFlow<com.example.util.ActiveTimerSnapshot> = taskTimerManager.timerState

    // Active Focus Timer (quick Pomodoro)
    private val _focusIsRunning = MutableStateFlow(false)
    val focusIsRunning: StateFlow<Boolean> = _focusIsRunning.asStateFlow()

    private val _focusRemainingSeconds = MutableStateFlow(25 * 60)
    val focusRemainingSeconds: StateFlow<Int> = _focusRemainingSeconds.asStateFlow()

    private val _focusTotalSeconds = MutableStateFlow(25 * 60)
    val focusTotalSeconds: StateFlow<Int> = _focusTotalSeconds.asStateFlow()

    private val _focusCurrentTask = MutableStateFlow<StudyTaskEntity?>(null)
    val focusCurrentTask: StateFlow<StudyTaskEntity?> = _focusCurrentTask.asStateFlow()

    private val _focusMode = MutableStateFlow("POMODORO") // "POMODORO", "SHORT_BREAK", "LONG_BREAK", "CUSTOM"
    val focusMode: StateFlow<String> = _focusMode.asStateFlow()

    private var timerJob: Job? = null

    // AI Study Assistant
    private val _assistantMessages = MutableStateFlow<List<AssistantMessage>>(
        listOf(
            AssistantMessage(
                isUser = false,
                text = "Hello! I am your StudyBloom Companion 🌸. Ask me what to study next, how to organize your hours, or how to break down difficult chapters!"
            )
        )
    )
    val assistantMessages: StateFlow<List<AssistantMessage>> = _assistantMessages.asStateFlow()

    private val _isAssistantThinking = MutableStateFlow(false)
    val isAssistantThinking: StateFlow<Boolean> = _isAssistantThinking.asStateFlow()

    // Schedule Creation Wizard
    private val _wizardState = MutableStateFlow(
        ScheduleWizardData(
            startDate = todayDate,
            targetEndDate = getFutureDate(7)
        )
    )
    val wizardState: StateFlow<ScheduleWizardData> = _wizardState.asStateFlow()

    // Status notification / snackbar
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureActiveTasksForToday()
            repository.checkAndUpdateStreak()
        }

        // Observe plan date tasks
        viewModelScope.launch {
            _selectedPlanDate.collect { date ->
                repository.getTasksForDate(date).collect { tasks ->
                    _planDateTasks.value = tasks
                }
            }
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setSelectedPlanDate(date: String) {
        _selectedPlanDate.value = date
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // --- Study Goals Actions ---
    fun addGoal(
        title: String,
        description: String,
        subject: String,
        category: String,
        estimatedMinutes: Int,
        difficulty: String,
        priority: String,
        preferredDate: String,
        deadlineDate: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createGoal(
                title = title,
                description = description,
                subject = subject,
                category = category,
                estimatedMinutes = estimatedMinutes,
                difficulty = difficulty,
                priority = priority,
                preferredDate = preferredDate.ifBlank { todayDate },
                deadlineDate = deadlineDate
            )
            _userMessage.value = "Goal '$title' added! 🌸"
        }
    }

    fun updateGoal(goal: StudyGoalEntity) {
        viewModelScope.launch {
            if (repository.updateGoal(goal)) {
                _userMessage.value = "Goal '${goal.title}' updated!"
            }
        }
    }

    fun toggleGoalComplete(goal: StudyGoalEntity) {
        viewModelScope.launch {
            val newCompleted = !goal.isCompleted
            repository.setGoalCompleted(goal.id, newCompleted)
            if (newCompleted) {
                _userMessage.value = "Goal '${goal.title}' completed! 🌟"
            }
        }
    }

    fun rescheduleGoal(goalId: Long, newPreferredDate: String, moveUncompletedTasks: Boolean = true) {
        viewModelScope.launch {
            if (repository.rescheduleGoal(goalId, newPreferredDate, moveUncompletedTasks)) {
                _userMessage.value = "Goal rescheduled to $newPreferredDate."
            }
        }
    }

    fun deleteGoal(goalId: Long, removeUncompletedTasks: Boolean = false) {
        viewModelScope.launch {
            if (repository.deleteGoal(goalId, removeUncompletedTasks)) {
                _userMessage.value = if (removeUncompletedTasks) {
                    "Goal and its uncompleted sessions removed. Timer history preserved."
                } else {
                    "Goal removed. Scheduled sessions & timer history preserved."
                }
            }
        }
    }

    // --- Task Actions ---
    fun toggleTaskComplete(task: StudyTaskEntity) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            repository.setTaskCompleted(task.id, newCompleted)
            if (newCompleted) {
                _userMessage.value = "Great job completing '${task.title}'! 🌸"
            }
        }
    }

    fun skipTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.setTaskSkipped(task.id, true)
            _userMessage.value = "Task marked skipped. You can reschedule anytime."
        }
    }

    fun rescheduleTask(task: StudyTaskEntity, newDate: String, newStart: String, newEnd: String) {
        viewModelScope.launch {
            repository.rescheduleTask(task.id, newDate, newStart, newEnd)
            _userMessage.value = "Rescheduled to $newDate at $newStart"
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            _userMessage.value = "Task deleted"
        }
    }

    fun addNewTask(
        title: String,
        description: String,
        category: String,
        subject: String,
        priority: String,
        date: String,
        startTime: String,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            val endMinutes = timeStringToMinutes(startTime) + durationMinutes
            val endTime = minutesToTimeString(endMinutes)
            val newTask = StudyTaskEntity(
                title = title,
                description = description,
                category = category,
                subject = subject,
                priority = priority,
                date = date,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                learningObjective = "Complete task objective"
            )
            repository.insertTask(newTask)
            _userMessage.value = "Task added to $date schedule!"
        }
    }

    fun smartRescheduleMissedTasks() {
        viewModelScope.launch {
            val count = repository.smartRescheduleMissedTasks(todayDate)
            if (count > 0) {
                _userMessage.value = "Reorganized $count missed tasks into today's evening slots! 🌿"
            } else {
                _userMessage.value = "No missed tasks found to reschedule."
            }
        }
    }

    // --- Document & Material Management ---
    fun uploadLearningMaterial(
        title: String,
        fileType: String,
        uriString: String,
        fileSizeFormatted: String,
        contentSample: String
    ) {
        viewModelScope.launch {
            val isOnline = networkState.value.isConnected
            _userMessage.value = if (isOnline) "Analyzing document with StudyBloom AI..." else "Analyzing document locally in Room storage..."
            val result = repository.addMaterialAndAnalyze(
                title = title,
                fileType = fileType,
                uriString = uriString,
                fileSizeFormatted = fileSizeFormatted,
                contentSample = contentSample
            )
            if (result.isSuccess) {
                if (!isOnline) {
                    val materialId = result.getOrNull()
                    repository.queueAiRequest(
                        type = "DOCUMENT_ANALYSIS",
                        title = "Document: $title",
                        prompt = contentSample.take(300),
                        relatedEntityId = materialId,
                        initialPreview = "Structured learning topics mapped locally."
                    )
                    _userMessage.value = "Analyzed '$title' locally! Saved in Room & queued for cloud sync. ☁️"
                } else {
                    _userMessage.value = "Material '$title' analyzed and added with learning modules! ✨"
                }
            } else {
                _userMessage.value = "Document saved locally in Room database."
            }
        }
    }

    fun deleteMaterial(id: Long) {
        viewModelScope.launch {
            repository.deleteMaterial(id)
            _userMessage.value = "Material and associated topics removed."
        }
    }

    fun addCustomTopicToMaterial(materialId: Long, title: String, difficulty: String, minutes: Int) {
        viewModelScope.launch {
            repository.addTopic(
                TopicEntity(
                    materialId = materialId,
                    title = title,
                    subtopicsRaw = "Custom subtopic",
                    difficulty = difficulty,
                    estimatedMinutes = minutes,
                    orderIndex = 99
                )
            )
            _userMessage.value = "Custom topic added!"
        }
    }

    fun deleteTopic(id: Long) {
        viewModelScope.launch {
            repository.deleteTopic(id)
        }
    }

    // --- Focus Timer ---
    fun selectTaskForFocus(task: StudyTaskEntity?) {
        _focusCurrentTask.value = task
        if (task != null) {
            val seconds = task.durationMinutes * 60
            _focusTotalSeconds.value = seconds
            _focusRemainingSeconds.value = seconds
        }
    }

    fun setTimerPreset(minutes: Int, modeName: String) {
        stopTimer()
        _focusMode.value = modeName
        val totalSecs = minutes * 60
        _focusTotalSeconds.value = totalSecs
        _focusRemainingSeconds.value = totalSecs
    }

    fun startTimer() {
        if (_focusIsRunning.value) return
        _focusIsRunning.value = true
        timerJob = viewModelScope.launch {
            while (_focusRemainingSeconds.value > 0 && _focusIsRunning.value) {
                delay(1000)
                _focusRemainingSeconds.value -= 1
            }
            if (_focusRemainingSeconds.value <= 0) {
                _focusIsRunning.value = false
                completeFocusSession()
            }
        }
    }

    fun pauseTimer() {
        _focusIsRunning.value = false
        timerJob?.cancel()
    }

    fun stopTimer() {
        _focusIsRunning.value = false
        timerJob?.cancel()
        _focusRemainingSeconds.value = _focusTotalSeconds.value
    }

    fun completeFocusSession() {
        val minutesSpent = (_focusTotalSeconds.value - _focusRemainingSeconds.value) / 60
        val actualMinutes = if (minutesSpent > 0) minutesSpent else _focusTotalSeconds.value / 60
        val task = _focusCurrentTask.value
        val sessionTimestamp = System.currentTimeMillis()
        val submissionKey = "POMODORO:${task?.id ?: 0L}:$sessionTimestamp"

        viewModelScope.launch {
            // Single transactional write updates session, task, and linked goal without double-counting
            repository.recordFocusSession(
                taskId = task?.id,
                taskTitle = task?.title ?: "Focus Session",
                subject = task?.subject ?: "General Study",
                durationMinutes = actualMinutes,
                markTaskCompleted = task != null,
                submissionKey = submissionKey
            )
            NotificationHelper.showFocusCompleteNotification(
                getApplication(),
                task?.title ?: "Focus Session",
                actualMinutes
            )
            _userMessage.value = "Focused study session logged ($actualMinutes min)! Great dedication! 🌟"
            stopTimer()
        }
    }

    // --- Live Aesthetic Task Clock ---
    fun startTaskClock(task: StudyTaskEntity) {
        taskTimerManager.startTask(task)
        _userMessage.value = "Live timer started for '${task.title}' ⏱️"
    }

    fun pauseTaskClock() {
        taskTimerManager.pause()
    }

    fun resumeTaskClock() {
        taskTimerManager.resume()
    }

    fun extendTaskClock(additionalMinutes: Int) {
        taskTimerManager.extendSession(additionalMinutes)
        _userMessage.value = "Session extended by $additionalMinutes minutes 🌿"
    }

    fun finishTaskClock() {
        val result = taskTimerManager.finishSession()
        if (!result.isValid) return
        viewModelScope.launch {
            val recorded = repository.recordCompletedTaskSession(
                taskId = result.taskId,
                plannedDurationMinutes = result.plannedDurationMinutes,
                actualDurationMinutes = result.actualDurationMinutes,
                overtimeMinutes = result.overtimeMinutes,
                pausedMinutes = result.pausedMinutes,
                startTimestamp = result.startTimestamp,
                endTimestamp = result.endTimestamp,
                markTaskAsCompleted = true,
                submissionKey = result.submissionKey
            )
            if (recorded) {
                _userMessage.value = "Task '${result.taskTitle}' finished! Logged ${result.actualDurationMinutes}m focused study. 🌸"

                // Smart Account Reminder: Gently offer account creation to guest after completing study session
                val profile = repository.getProfileOnce()
                if (profile.isGuest && !profile.hasSeenAccountReminder) {
                    delay(1000)
                    _showAccountReminderDialog.value = true
                }
            }
        }
    }

    fun stopTaskClock() {
        val result = taskTimerManager.stopWithoutCompleting()
        if (!result.isValid) return
        viewModelScope.launch {
            val recorded = repository.recordCompletedTaskSession(
                taskId = result.taskId,
                plannedDurationMinutes = result.plannedDurationMinutes,
                actualDurationMinutes = result.actualDurationMinutes,
                overtimeMinutes = result.overtimeMinutes,
                pausedMinutes = result.pausedMinutes,
                startTimestamp = result.startTimestamp,
                endTimestamp = result.endTimestamp,
                markTaskAsCompleted = false,
                submissionKey = result.submissionKey
            )
            if (recorded) {
                _userMessage.value = "Session stopped. Logged ${result.actualDurationMinutes}m focused time."
            }
        }
    }

    // --- Schedule Wizard ---
    fun openMultiGoalWizard(targetDate: String = _selectedPlanDate.value) {
        viewModelScope.launch {
            val profile = repository.getProfileOnce()
            val allActiveGoals = repository.getAllGoalsForCurrentOwnerOnce().filter { !it.isCompleted }
            val matchingGoals = allActiveGoals.filter {
                it.preferredDate == targetDate || it.preferredDate <= targetDate
            }.ifEmpty { allActiveGoals }

            _wizardState.value = ScheduleWizardData(
                step = 1,
                wizardMode = if (allActiveGoals.isNotEmpty()) "GOALS" else "MATERIAL",
                selectedGoalIds = matchingGoals.map { it.id }.toSet(),
                startDate = targetDate.ifBlank { todayDate },
                targetEndDate = targetDate.ifBlank { todayDate },
                dailyMinutesTarget = profile.dailyGoalMinutes,
                sessionDuration = profile.preferredSessionDurationMinutes,
                breakDuration = profile.preferredBreakDurationMinutes
            )
        }
    }

    fun openMaterialWizard(material: LearningMaterialEntity? = null) {
        viewModelScope.launch {
            val profile = repository.getProfileOnce()
            val topics = if (material != null) {
                StudyBloomDatabase.getInstance(getApplication()).topicDao().getTopicsForMaterialOnce(material.id)
            } else {
                emptyList()
            }
            _wizardState.value = ScheduleWizardData(
                step = 1,
                wizardMode = "MATERIAL",
                subject = material?.title ?: "",
                selectedMaterialId = material?.id,
                selectedTopics = topics,
                startDate = todayDate,
                targetEndDate = getFutureDate(7),
                dailyMinutesTarget = profile.dailyGoalMinutes,
                sessionDuration = profile.preferredSessionDurationMinutes,
                breakDuration = profile.preferredBreakDurationMinutes
            )
        }
    }

    fun setWizardMode(mode: String) {
        viewModelScope.launch {
            val current = _wizardState.value
            val selectedIds = if (mode == "GOALS" && current.selectedGoalIds.isEmpty()) {
                repository.getAllGoalsForCurrentOwnerOnce()
                    .filter { !it.isCompleted }
                    .map { it.id }
                    .toSet()
            } else {
                current.selectedGoalIds
            }
            _wizardState.value = current.copy(
                wizardMode = mode,
                selectedGoalIds = selectedIds,
                generatedPreview = emptyList(),
                multiGoalResult = null,
                errorMessage = null
            )
        }
    }

    fun toggleWizardGoalSelection(goalId: Long) {
        val current = _wizardState.value
        val updated = current.selectedGoalIds.toMutableSet()
        if (!updated.add(goalId)) {
            updated.remove(goalId)
        }
        _wizardState.value = current.copy(selectedGoalIds = updated, errorMessage = null)
    }

    fun selectAllWizardGoals(goals: List<StudyGoalEntity>) {
        val activeIds = goals.filter { !it.isCompleted }.map { it.id }.toSet()
        _wizardState.value = _wizardState.value.copy(selectedGoalIds = activeIds, errorMessage = null)
    }

    fun updateWizardStep(step: Int) {
        _wizardState.value = _wizardState.value.copy(step = step)
    }

    fun setWizardSubject(subject: String, materialId: Long? = null) {
        viewModelScope.launch {
            val topics = if (materialId != null) {
                StudyBloomDatabase.getInstance(getApplication()).topicDao().getTopicsForMaterialOnce(materialId)
            } else {
                emptyList()
            }
            _wizardState.value = _wizardState.value.copy(
                subject = subject,
                selectedMaterialId = materialId,
                selectedTopics = topics
            )
        }
    }

    fun setWizardTarget(startDate: String, targetEndDate: String, dailyMinutes: Int) {
        _wizardState.value = _wizardState.value.copy(
            startDate = startDate,
            targetEndDate = targetEndDate,
            dailyMinutesTarget = dailyMinutes
        )
    }

    fun setWizardSessionSettings(sessionDuration: Int, breakDuration: Int, priority: String) {
        _wizardState.value = _wizardState.value.copy(
            sessionDuration = sessionDuration,
            breakDuration = breakDuration,
            priority = priority
        )
    }

    fun generateSchedulePreview() {
        viewModelScope.launch {
            _wizardState.value = _wizardState.value.copy(isGenerating = true, errorMessage = null)
            val slots = repository.getAllSlotsOnce()
            val state = _wizardState.value

            if (state.wizardMode == "GOALS") {
                val allActive = repository.getAllGoalsForCurrentOwnerOnce().filter { !it.isCompleted }
                val selectedGoals = allActive.filter { it.id in state.selectedGoalIds }
                if (selectedGoals.isNotEmpty()) {
                    val effectiveStart = state.startDate.ifBlank { todayDate }
                    val effectiveEnd = state.targetEndDate.ifBlank { effectiveStart }
                        .let { if (it < effectiveStart) effectiveStart else it }
                    val existingTasks = repository.getTasksBetweenDatesOnce(effectiveStart, effectiveEnd)

                    val multiResult = repository.generateMultiGoalSchedule(
                        MultiGoalScheduleRequest(
                            goals = selectedGoals,
                            existingTasks = existingTasks,
                            startDate = effectiveStart,
                            targetEndDate = effectiveEnd,
                            dailyGoalMinutes = state.dailyMinutesTarget,
                            sessionDurationMinutes = state.sessionDuration,
                            breakDurationMinutes = state.breakDuration,
                            availabilitySlots = slots
                        )
                    )
                    if (multiResult.isSuccess) {
                        val res = multiResult.getOrThrow()
                        _wizardState.value = _wizardState.value.copy(
                            isGenerating = false,
                            generatedPreview = res.tasks,
                            multiGoalResult = res,
                            step = 5,
                            errorMessage = res.warningMessage
                        )
                    } else {
                        _wizardState.value = _wizardState.value.copy(
                            isGenerating = false,
                            errorMessage = multiResult.exceptionOrNull()?.message ?: "Failed to generate goal schedule"
                        )
                    }
                    return@launch
                } else if (state.subject.isBlank()) {
                    _wizardState.value = _wizardState.value.copy(
                        isGenerating = false,
                        errorMessage = "Select at least one study goal or switch to Material / Subject mode."
                    )
                    return@launch
                }
            }

            val topics = if (state.selectedTopics.isNotEmpty()) {
                state.selectedTopics
            } else {
                // Generate structured study modules when scheduling a custom subject
                listOf(
                    TopicEntity(materialId = 0, title = "1. Foundations of ${state.subject.ifBlank { "Study Topic" }}", subtopicsRaw = "Key concepts, glossary", difficulty = "EASY", estimatedMinutes = 45, isPrerequisite = true, orderIndex = 1),
                    TopicEntity(materialId = 0, title = "2. Core Methodologies in ${state.subject.ifBlank { "Study Topic" }}", subtopicsRaw = "Practical workflows", difficulty = "MEDIUM", estimatedMinutes = 60, isPrerequisite = true, orderIndex = 2),
                    TopicEntity(materialId = 0, title = "3. Hands-on Practice & Application", subtopicsRaw = "Exercises & implementation", difficulty = "HARD", estimatedMinutes = 90, isPrerequisite = false, orderIndex = 3),
                    TopicEntity(materialId = 0, title = "4. Review, Edge Cases & Synthesis", subtopicsRaw = "Spaced repetition", difficulty = "MEDIUM", estimatedMinutes = 45, isPrerequisite = false, orderIndex = 4)
                )
            }

            val request = ScheduleRequest(
                selectedSubject = state.subject,
                topics = topics,
                startDate = state.startDate.ifBlank { todayDate },
                targetEndDate = state.targetEndDate.ifBlank { getFutureDate(7) },
                sessionDurationMinutes = state.sessionDuration,
                breakDurationMinutes = state.breakDuration,
                priority = state.priority,
                availabilitySlots = slots
            )

            val result = repository.generateSchedule(request)
            if (result.isSuccess) {
                _wizardState.value = _wizardState.value.copy(
                    isGenerating = false,
                    generatedPreview = result.getOrThrow(),
                    multiGoalResult = null,
                    step = 5 // Preview step
                )
            } else {
                _wizardState.value = _wizardState.value.copy(
                    isGenerating = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to generate schedule"
                )
            }
        }
    }

    fun redistributeOverflowAcrossDays() {
        val current = _wizardState.value
        val suggestedLast = current.multiGoalResult?.suggestedMultiDayDates?.lastOrNull()
        val expandedEnd = suggestedLast ?: addDaysToDate(current.targetEndDate.ifBlank { todayDate }, 3)
        _wizardState.value = current.copy(targetEndDate = expandedEnd)
        generateSchedulePreview()
    }

    fun saveGeneratedSchedule() {
        val preview = _wizardState.value.generatedPreview
        if (preview.isEmpty()) return

        viewModelScope.launch {
            repository.insertTasks(preview)
            _userMessage.value = "Saved ${preview.size} scheduled sessions to your planner! 🎉"
            _wizardState.value = ScheduleWizardData(
                startDate = todayDate,
                targetEndDate = getFutureDate(7)
            )
            _currentTab.value = "plan"
        }
    }

    // --- AI Assistant ---
    fun sendAssistantMessage(prompt: String) {
        if (prompt.isBlank()) return
        val current = _assistantMessages.value.toMutableList()
        current.add(AssistantMessage(isUser = true, text = prompt))
        _assistantMessages.value = current
        _isAssistantThinking.value = true

        viewModelScope.launch {
            val isOnline = networkState.value.isConnected
            val profile = repository.getProfileOnce()
            val pending = repository.pendingTasks.stateIn(this).value
            val todayTasksList = todayTasks.value
            val materialsList = allMaterials.value

            val context = AssistantContext(
                userName = profile.nickname.ifBlank { "Learner" },
                todayDate = todayDate,
                pendingTasks = pending,
                completedTasksToday = todayTasksList.filter { it.isCompleted },
                materials = materialsList,
                availableHoursToday = profile.dailyGoalMinutes / 60.0
            )

            val reply = repository.askAssistant(prompt, context)
            if (!isOnline) {
                repository.queueAiRequest(
                    type = "ASSISTANT_QUERY",
                    title = "AI Query: ${prompt.take(30)}",
                    prompt = prompt,
                    initialPreview = reply
                )
            }

            _isAssistantThinking.value = false
            val updated = _assistantMessages.value.toMutableList()
            val replyText = if (!isOnline) {
                "$reply\n\n🌿 (Generated offline with StudyBloom Local Engine • Queued for cloud sync)"
            } else {
                reply
            }
            updated.add(AssistantMessage(isUser = false, text = replyText))
            _assistantMessages.value = updated
        }
    }

    // --- Cloud Sync & Offline Management ---
    fun syncNow() {
        viewModelScope.launch {
            val res = syncManager.performSync(isManual = true)
            when (res) {
                is SyncResult.Success -> {
                    _userMessage.value = if (res.itemsSynced > 0)
                        "Synced ${res.itemsSynced} pending AI requests & local study data with cloud ☁️"
                    else
                        "StudyBloom is up to date • Synced with cloud"
                }
                is SyncResult.Offline -> {
                    _userMessage.value = "Working offline • All tasks and active timers are safe in local Room storage"
                }
                is SyncResult.Error -> {
                    _userMessage.value = "Sync paused. Local Room database remains safe and active."
                }
            }
        }
    }

    fun retryPendingRequest(id: Long) {
        viewModelScope.launch {
            syncNow()
        }
    }

    fun deletePendingRequest(id: Long) {
        viewModelScope.launch {
            repository.deleteAiRequest(id)
            _userMessage.value = "Queued request removed."
        }
    }

    // --- Profile & Settings ---
    fun saveProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.updateProfile(profile)
            _userMessage.value = "Profile preferences updated!"
        }
    }

    fun completeOnboarding(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.completeOnboarding(profile)
        }
    }

    fun testReminderNotification() {
        val nextTask = todayTasks.value.firstOrNull { !it.isCompleted && !it.isSkipped }
        NotificationHelper.showTaskReminder(
            getApplication(),
            nextTask?.title ?: "Scheduled Study Session",
            nextTask?.subject?.ifBlank { "StudyBloom" } ?: "StudyBloom",
            nextTask?.startTime ?: "Now"
        )
        _userMessage.value = "Test notification sent! Check your notification shade. 🌿"
    }

    // --- PDF Export ---
    fun exportTodaySchedulePdf(): File {
        val profile = userProfile.value ?: UserProfileEntity()
        val tasks = todayTasks.value
        return PdfExporter.generateDailySchedulePdf(
            context = getApplication(),
            userName = profile.nickname,
            date = todayDate,
            dailyGoalMinutes = profile.dailyGoalMinutes,
            tasks = tasks
        )
    }

    fun exportWeeklySchedulePdf(): File {
        val profile = userProfile.value ?: UserProfileEntity()
        val tasks = planDateTasks.value.ifEmpty { todayTasks.value }
        return PdfExporter.generateWeeklySchedulePdf(
            context = getApplication(),
            userName = profile.nickname,
            startDate = todayDate,
            tasks = tasks
        )
    }

    fun exportCsvString(): String {
        return PdfExporter.generateCsvExport(todayTasks.value)
    }

    private fun getFutureDate(daysAhead: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        return dateFormat.format(cal.time)
    }

    private fun addDaysToDate(baseDate: String, daysAhead: Int): String {
        val cal = Calendar.getInstance()
        try {
            val parsed = dateFormat.parse(baseDate)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {
            cal.time = Date()
        }
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        return dateFormat.format(cal.time)
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

    // --- Authentication & Account Creation Actions ---

    fun openAccountReminder() {
        _showAccountReminderDialog.value = true
    }

    fun dismissAccountReminder() {
        _showAccountReminderDialog.value = false
        viewModelScope.launch {
            repository.markAccountReminderSeen()
        }
    }

    fun createAccount(name: String, email: String, pass: String) {
        viewModelScope.launch {
            val result = authManager.createAccountAndMigrate(
                name = name,
                email = email,
                password = pass,
                isNetworkConnected = networkState.value.isConnected
            )
            if (result.isSuccess) {
                _userMessage.value = "Account created & cloud backup enabled! 🌸"
                _showAccountReminderDialog.value = false
            }
        }
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            val result = authManager.signIn(
                email = email,
                password = pass,
                isNetworkConnected = networkState.value.isConnected
            )
            if (result.isSuccess) {
                _userMessage.value = "Signed in! Cloud backup synced. 🌸"
                _showAccountReminderDialog.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _userMessage.value = "Switched to Guest mode. Data is stored on this device."
        }
    }

    fun resetAuthMigrationState() {
        authManager.resetState()
    }
}
