package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.ui.components.AccountCreationReminderDialog
import com.example.ui.components.ActiveTimerPill
import com.example.ui.components.AddEditGoalDialog
import com.example.ui.components.AddTaskDialog
import com.example.ui.components.AiAssistantSheet
import com.example.ui.components.AuthDialog
import com.example.ui.components.ConnectionDetailsSheet
import com.example.ui.components.DeleteGoalConfirmationDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.RescheduleGoalDialog
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MaterialsScreen
import com.example.ui.screens.MyPlanScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.ScheduleWizardScreen
import com.example.ui.screens.TaskClockScreen
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.StudyBloomTheme
import com.example.ui.viewmodel.StudyBloomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyBloomApp(
    viewModel: StudyBloomViewModel = viewModel(),
    openActiveClockTrigger: Long = 0L,
    openAuthOnStart: Boolean = false,
    initialIsRegister: Boolean = true
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val todayTasks by viewModel.todayTasks.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val todayGoals by viewModel.todayGoals.collectAsState()
    val activeGoals by viewModel.activeGoals.collectAsState()
    val selectedPlanDate by viewModel.selectedPlanDate.collectAsState()
    val planDateTasks by viewModel.planDateTasks.collectAsState()
    val planDateGoals by viewModel.planDateGoals.collectAsState()
    val allMaterials by viewModel.allMaterials.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val totalMinutesAllTime by viewModel.totalMinutesAllTime.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    // Live Task Clock Timer State
    val liveTimerSnapshot by viewModel.liveTimerSnapshot.collectAsState()

    // Focus Timer state (quick Pomodoro)
    val focusIsRunning by viewModel.focusIsRunning.collectAsState()
    val focusRemaining by viewModel.focusRemainingSeconds.collectAsState()
    val focusTotal by viewModel.focusTotalSeconds.collectAsState()
    val focusCurrentTask by viewModel.focusCurrentTask.collectAsState()
    val focusMode by viewModel.focusMode.collectAsState()

    // Wizard state
    val wizardData by viewModel.wizardState.collectAsState()

    // Assistant state
    val assistantMessages by viewModel.assistantMessages.collectAsState()
    val isAssistantThinking by viewModel.isAssistantThinking.collectAsState()

    // Network & Cloud Sync State
    val networkState by viewModel.networkState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val pendingAiRequests by viewModel.pendingAiRequests.collectAsState()

    // Authentication & Account Migration State
    val migrationState by viewModel.migrationState.collectAsState()
    val showAccountReminderDialog by viewModel.showAccountReminderDialog.collectAsState()

    // Dialog & Screen Visibility
    var showTaskClockScreen by remember { mutableStateOf(false) }
    var showFocusTimer by remember { mutableStateOf(false) }
    var showWizard by remember { mutableStateOf(false) }
    var showAssistantSheet by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<StudyGoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<StudyGoalEntity?>(null) }
    var goalToReschedule by remember { mutableStateOf<StudyGoalEntity?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showConnectionDetailsSheet by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(openAuthOnStart) }
    var authIsRegister by remember { mutableStateOf(initialIsRegister) }

    LaunchedEffect(openActiveClockTrigger) {
        if (openActiveClockTrigger > 0L && liveTimerSnapshot.isActive) {
            showTaskClockScreen = true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val themeMode = userProfile?.themeMode ?: "SYSTEM"

    StudyBloomTheme(themeMode = themeMode) {
        // First-Time Onboarding (Redesigned Login / Welcome Screen)
        if (userProfile != null && !userProfile!!.hasCompletedOnboarding) {
            LoginScreen(
                onContinueAsGuest = {
                    val guestProfile = userProfile!!.copy(
                        isGuest = true,
                        hasCompletedOnboarding = true
                    )
                    viewModel.completeOnboarding(guestProfile)
                },
                onCreateAccount = {
                    authIsRegister = true
                    showAuthDialog = true
                },
                onSignIn = {
                    authIsRegister = false
                    showAuthDialog = true
                }
            )

            if (showAuthDialog) {
                AuthDialog(
                    initialIsRegister = authIsRegister,
                    migrationState = migrationState,
                    isOnline = networkState.isConnected,
                    onDismiss = { showAuthDialog = false },
                    onCreateAccount = { name, email, pass ->
                        viewModel.createAccount(name, email, pass)
                    },
                    onSignIn = { email, pass ->
                        viewModel.signIn(email, pass)
                    },
                    onResetState = { viewModel.resetAuthMigrationState() }
                )
            }
            return@StudyBloomTheme
        }

        // Full-screen Aesthetic Task Clock
        if (showTaskClockScreen && liveTimerSnapshot.isActive) {
            BackHandler { showTaskClockScreen = false }
            TaskClockScreen(
                timerSnapshot = liveTimerSnapshot,
                onPause = { viewModel.pauseTaskClock() },
                onResume = { viewModel.resumeTaskClock() },
                onExtend = { viewModel.extendTaskClock(it) },
                onFinishTask = {
                    viewModel.finishTaskClock()
                    showTaskClockScreen = false
                },
                onStopSession = {
                    viewModel.stopTaskClock()
                    showTaskClockScreen = false
                },
                onClose = { showTaskClockScreen = false }
            )
            return@StudyBloomTheme
        }

        // Focus Timer Overlay
        if (showFocusTimer) {
            BackHandler { showFocusTimer = false }
            FocusTimerScreen(
                isRunning = focusIsRunning,
                remainingSeconds = focusRemaining,
                totalSeconds = focusTotal,
                currentTask = focusCurrentTask,
                todayTasks = todayTasks,
                focusMode = focusMode,
                onStart = { viewModel.startTimer() },
                onPause = { viewModel.pauseTimer() },
                onReset = { viewModel.stopTimer() },
                onCompleteSession = { viewModel.completeFocusSession() },
                onSelectTask = { viewModel.selectTaskForFocus(it) },
                onSetPreset = { mins, mode -> viewModel.setTimerPreset(mins, mode) },
                onClose = { showFocusTimer = false }
            )
            return@StudyBloomTheme
        }

        if (showWizard) {
            BackHandler { showWizard = false }
            ScheduleWizardScreen(
                wizardData = wizardData,
                materials = allMaterials,
                activeGoals = activeGoals,
                onStepChange = { viewModel.updateWizardStep(it) },
                onSetWizardMode = { viewModel.setWizardMode(it) },
                onToggleGoalSelection = { viewModel.toggleWizardGoalSelection(it) },
                onSelectAllGoals = { viewModel.selectAllWizardGoals(activeGoals) },
                onSetSubject = { subj, matId -> viewModel.setWizardSubject(subj, matId) },
                onSetTarget = { start, end, daily -> viewModel.setWizardTarget(start, end, daily) },
                onSetSessionSettings = { sess, brk, prio -> viewModel.setWizardSessionSettings(sess, brk, prio) },
                onGenerate = { viewModel.generateSchedulePreview() },
                onRedistributeOverflow = { viewModel.redistributeOverflowAcrossDays() },
                onSaveSchedule = {
                    viewModel.saveGeneratedSchedule()
                    showWizard = false
                },
                onClose = { showWizard = false }
            )
            return@StudyBloomTheme
        }

        // Main App Scaffold
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                Column {
                    // Persistent Active Task Indicator Pill above Navigation Bar
                    if (liveTimerSnapshot.isActive) {
                        ActiveTimerPill(
                            snapshot = liveTimerSnapshot,
                            onOpenClock = { showTaskClockScreen = true },
                            onPause = { viewModel.pauseTaskClock() },
                            onResume = { viewModel.resumeTaskClock() }
                        )
                    }

                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav_bar"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == "home",
                            onClick = { viewModel.setTab("home") },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 11.sp, fontWeight = if (currentTab == "home") FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LavenderPrimary,
                                selectedTextColor = LavenderPrimary,
                                indicatorColor = LavenderLight
                            ),
                            modifier = Modifier.testTag("nav_item_home")
                        )

                        NavigationBarItem(
                            selected = currentTab == "plan",
                            onClick = { viewModel.setTab("plan") },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "My Plan") },
                            label = { Text("My Plan", fontSize = 11.sp, fontWeight = if (currentTab == "plan") FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LavenderPrimary,
                                selectedTextColor = LavenderPrimary,
                                indicatorColor = LavenderLight
                            ),
                            modifier = Modifier.testTag("nav_item_plan")
                        )

                        NavigationBarItem(
                            selected = currentTab == "materials",
                            onClick = { viewModel.setTab("materials") },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Materials") },
                            label = { Text("Materials", fontSize = 11.sp, fontWeight = if (currentTab == "materials") FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LavenderPrimary,
                                selectedTextColor = LavenderPrimary,
                                indicatorColor = LavenderLight
                            ),
                            modifier = Modifier.testTag("nav_item_materials")
                        )

                        NavigationBarItem(
                            selected = currentTab == "progress",
                            onClick = { viewModel.setTab("progress") },
                            icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Progress") },
                            label = { Text("Progress", fontSize = 11.sp, fontWeight = if (currentTab == "progress") FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LavenderPrimary,
                                selectedTextColor = LavenderPrimary,
                                indicatorColor = LavenderLight
                            ),
                            modifier = Modifier.testTag("nav_item_progress")
                        )

                        NavigationBarItem(
                            selected = currentTab == "profile",
                            onClick = { viewModel.setTab("profile") },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontSize = 11.sp, fontWeight = if (currentTab == "profile") FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LavenderPrimary,
                                selectedTextColor = LavenderPrimary,
                                indicatorColor = LavenderLight
                            ),
                            modifier = Modifier.testTag("nav_item_profile")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (currentTab != "home") {
                    BackHandler {
                        viewModel.setTab("home")
                    }
                }

                when (currentTab) {
                    "home" -> HomeScreen(
                        userProfile = userProfile ?: UserProfileEntity(),
                        todayTasks = todayTasks,
                        todayGoals = todayGoals,
                        allTasks = allTasks,
                        networkState = networkState,
                        syncState = syncState,
                        onOpenConnectionDetails = { showConnectionDetailsSheet = true },
                        onToggleTaskComplete = { viewModel.toggleTaskComplete(it) },
                        onSkipTask = { viewModel.skipTask(it) },
                        onStartFocusForTask = { task ->
                            viewModel.startTaskClock(task)
                            showTaskClockScreen = true
                        },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onRescheduleTask = { task, date, start, end ->
                            viewModel.rescheduleTask(task, date, start, end)
                        },
                        onOpenAddTask = { showAddTaskDialog = true },
                        onOpenWizard = {
                            viewModel.openMultiGoalWizard(viewModel.todayDate)
                            showWizard = true
                        },
                        onOpenAssistant = { showAssistantSheet = true },
                        onOpenFocusTimer = { showFocusTimer = true },
                        onExportPdf = { showExportDialog = true },
                        onOpenAccountReminder = { viewModel.openAccountReminder() },
                        onOpenAddGoal = {
                            goalToEdit = null
                            showGoalDialog = true
                        },
                        onEditGoal = { goal ->
                            goalToEdit = goal
                            showGoalDialog = true
                        },
                        onToggleGoalComplete = { viewModel.toggleGoalComplete(it) },
                        onRescheduleGoal = { goalToReschedule = it },
                        onDeleteGoal = { goalToDelete = it },
                        onScheduleGoalsWithAi = {
                            viewModel.openMultiGoalWizard(viewModel.todayDate)
                            showWizard = true
                        }
                    )

                    "plan" -> MyPlanScreen(
                        selectedDate = selectedPlanDate,
                        onSelectDate = { viewModel.setSelectedPlanDate(it) },
                        dayTasks = planDateTasks,
                        dayGoals = planDateGoals,
                        allTasks = allTasks,
                        onToggleComplete = { viewModel.toggleTaskComplete(it) },
                        onStartFocus = { task ->
                            viewModel.startTaskClock(task)
                            showTaskClockScreen = true
                        },
                        onSkipTask = { viewModel.skipTask(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onRescheduleTask = { task, date, start, end ->
                            viewModel.rescheduleTask(task, date, start, end)
                        },
                        onSmartRescheduleMissed = { viewModel.smartRescheduleMissedTasks() },
                        onExportWeeklyPdf = { showExportDialog = true },
                        onOpenAddTask = { showAddTaskDialog = true },
                        onOpenWizard = {
                            viewModel.openMultiGoalWizard(selectedPlanDate)
                            showWizard = true
                        },
                        onOpenAddGoal = {
                            goalToEdit = null
                            showGoalDialog = true
                        },
                        onEditGoal = { goal ->
                            goalToEdit = goal
                            showGoalDialog = true
                        },
                        onToggleGoalComplete = { viewModel.toggleGoalComplete(it) },
                        onRescheduleGoal = { goalToReschedule = it },
                        onDeleteGoal = { goalToDelete = it },
                        onScheduleGoalsWithAi = {
                            viewModel.openMultiGoalWizard(selectedPlanDate)
                            showWizard = true
                        }
                    )

                    "materials" -> MaterialsScreen(
                        materials = allMaterials,
                        onUploadMaterial = { title, type, uri, size, sample ->
                            viewModel.uploadLearningMaterial(title, type, uri, size, sample)
                        },
                        onDeleteMaterial = { viewModel.deleteMaterial(it) },
                        onAddTopicToMaterial = { matId, title, diff, mins ->
                            viewModel.addCustomTopicToMaterial(matId, title, diff, mins)
                        },
                        onDeleteTopic = { viewModel.deleteTopic(it) },
                        onGenerateScheduleFromMaterial = { mat ->
                            viewModel.openMaterialWizard(mat)
                            showWizard = true
                        }
                    )

                    "progress" -> ProgressScreen(
                        userProfile = userProfile ?: UserProfileEntity(),
                        sessions = allSessions,
                        totalMinutesAllTime = totalMinutesAllTime ?: 0
                    )

                    "profile" -> ProfileScreen(
                        userProfile = userProfile ?: UserProfileEntity(),
                        networkState = networkState,
                        syncState = syncState,
                        onSyncNow = { viewModel.syncNow() },
                        onOpenConnectionDetails = { showConnectionDetailsSheet = true },
                        onOpenAuth = { isRegister ->
                            authIsRegister = isRegister
                            showAuthDialog = true
                        },
                        onSignOut = { viewModel.signOut() },
                        onSaveProfile = { viewModel.saveProfile(it) },
                        onTestNotification = { viewModel.testReminderNotification() }
                    )
                }
            }
        }

        // Contextual AI Assistant Sheet
        if (showAssistantSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAssistantSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                AiAssistantSheet(
                    messages = assistantMessages,
                    isThinking = isAssistantThinking,
                    isOnline = networkState.isConnected,
                    onSendMessage = { viewModel.sendAssistantMessage(it) },
                    onClose = { showAssistantSheet = false }
                )
            }
        }

        // Connection Details & Offline Guarantees Sheet
        if (showConnectionDetailsSheet) {
            ConnectionDetailsSheet(
                networkState = networkState,
                syncState = syncState,
                pendingRequests = pendingAiRequests,
                onSyncNow = { viewModel.syncNow() },
                onRetryRequest = { viewModel.retryPendingRequest(it) },
                onDeleteRequest = { viewModel.deletePendingRequest(it) },
                onDismiss = { showConnectionDetailsSheet = false }
            )
        }

        // Add Custom Task Dialog
        if (showAddTaskDialog) {
            AddTaskDialog(
                initialDate = selectedPlanDate,
                onDismiss = { showAddTaskDialog = false },
                onSaveTask = { title, desc, cat, subj, prio, date, start, dur ->
                    viewModel.addNewTask(title, desc, cat, subj, prio, date, start, dur)
                }
            )
        }

        // Add / Edit Daily Study Goal Dialog
        if (showGoalDialog) {
            val defaultDate = if (currentTab == "plan") selectedPlanDate else viewModel.todayDate
            AddEditGoalDialog(
                initialDate = defaultDate,
                goalToEdit = goalToEdit,
                onDismiss = {
                    showGoalDialog = false
                    goalToEdit = null
                },
                onSaveGoal = { title, desc, subj, cat, estMins, diff, prio, prefDate, deadline ->
                    val editing = goalToEdit
                    if (editing == null) {
                        viewModel.addGoal(title, desc, subj, cat, estMins, diff, prio, prefDate, deadline)
                    } else {
                        viewModel.updateGoal(
                            editing.copy(
                                title = title,
                                description = desc,
                                subject = subj,
                                category = cat,
                                estimatedMinutes = estMins,
                                difficulty = diff,
                                priority = prio,
                                preferredDate = prefDate,
                                deadlineDate = deadline
                            )
                        )
                    }
                }
            )
        }

        // Delete Goal Confirmation Dialog
        goalToDelete?.let { targetGoal ->
            val uncompletedLinked = allTasks.count { it.goalId == targetGoal.id && !it.isCompleted }
            DeleteGoalConfirmationDialog(
                goal = targetGoal,
                linkedUncompletedCount = uncompletedLinked,
                onDismiss = { goalToDelete = null },
                onConfirmDelete = { removeUncompleted ->
                    viewModel.deleteGoal(targetGoal.id, removeUncompleted)
                }
            )
        }

        // Reschedule Goal Dialog
        goalToReschedule?.let { targetGoal ->
            RescheduleGoalDialog(
                goal = targetGoal,
                onDismiss = { goalToReschedule = null },
                onConfirmReschedule = { newDate, moveTasks ->
                    viewModel.rescheduleGoal(targetGoal.id, newDate, moveTasks)
                }
            )
        }

        // Export Dialog
        if (showExportDialog) {
            ExportDialog(
                onDismiss = { showExportDialog = false },
                onExportTodayPdf = { viewModel.exportTodaySchedulePdf() },
                onExportWeeklyPdf = { viewModel.exportWeeklySchedulePdf() },
                onExportCsv = { viewModel.exportCsvString() }
            )
        }

        // Smart Account Creation Reminder Dialog (Never shown during active task timer)
        if (showAccountReminderDialog && !liveTimerSnapshot.isActive) {
            AccountCreationReminderDialog(
                onDismiss = { viewModel.dismissAccountReminder() },
                onCreateAccount = {
                    viewModel.dismissAccountReminder()
                    authIsRegister = true
                    showAuthDialog = true
                }
            )
        }

        // Authentication & Migration Dialog
        if (showAuthDialog) {
            AuthDialog(
                initialIsRegister = authIsRegister,
                migrationState = migrationState,
                isOnline = networkState.isConnected,
                onDismiss = { showAuthDialog = false },
                onCreateAccount = { name, email, pass ->
                    viewModel.createAccount(name, email, pass)
                },
                onSignIn = { email, pass ->
                    viewModel.signIn(email, pass)
                },
                onResetState = { viewModel.resetAuthMigrationState() }
            )
        }
    }
}
