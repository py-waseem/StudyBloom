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
import com.example.data.local.entity.UserProfileEntity
import com.example.ui.components.ActiveTimerPill
import com.example.ui.components.AddTaskDialog
import com.example.ui.components.AiAssistantSheet
import com.example.ui.components.ExportDialog
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.HomeScreen
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
    openActiveClockTrigger: Long = 0L
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val todayTasks by viewModel.todayTasks.collectAsState()
    val selectedPlanDate by viewModel.selectedPlanDate.collectAsState()
    val planDateTasks by viewModel.planDateTasks.collectAsState()
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

    // Dialog & Screen Visibility
    var showTaskClockScreen by remember { mutableStateOf(false) }
    var showFocusTimer by remember { mutableStateOf(false) }
    var showWizard by remember { mutableStateOf(false) }
    var showAssistantSheet by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

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
        // First-Time Onboarding
        if (userProfile != null && !userProfile!!.hasCompletedOnboarding) {
            OnboardingScreen(
                initialProfile = userProfile!!,
                onComplete = { completedProfile ->
                    viewModel.completeOnboarding(completedProfile)
                }
            )
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
                onStepChange = { viewModel.updateWizardStep(it) },
                onSetSubject = { subj, matId -> viewModel.setWizardSubject(subj, matId) },
                onSetTarget = { start, end, daily -> viewModel.setWizardTarget(start, end, daily) },
                onSetSessionSettings = { sess, brk, prio -> viewModel.setWizardSessionSettings(sess, brk, prio) },
                onGenerate = { viewModel.generateSchedulePreview() },
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
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar"),
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
                when (currentTab) {
                    "home" -> HomeScreen(
                        userProfile = userProfile ?: UserProfileEntity(),
                        todayTasks = todayTasks,
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
                        onOpenWizard = { showWizard = true },
                        onOpenAssistant = { showAssistantSheet = true },
                        onOpenFocusTimer = { showFocusTimer = true },
                        onExportPdf = { showExportDialog = true }
                    )

                    "plan" -> MyPlanScreen(
                        selectedDate = selectedPlanDate,
                        onSelectDate = { viewModel.setSelectedPlanDate(it) },
                        dayTasks = planDateTasks,
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
                        onOpenWizard = { showWizard = true }
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
                            viewModel.setWizardSubject(mat.title, mat.id)
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
                    onSendMessage = { viewModel.sendAssistantMessage(it) },
                    onClose = { showAssistantSheet = false }
                )
            }
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

        // Export Dialog
        if (showExportDialog) {
            ExportDialog(
                onDismiss = { showExportDialog = false },
                onExportTodayPdf = { viewModel.exportTodaySchedulePdf() },
                onExportWeeklyPdf = { viewModel.exportWeeklySchedulePdf() },
                onExportCsv = { viewModel.exportCsvString() }
            )
        }
    }
}
