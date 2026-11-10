package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StudyTaskEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.ui.components.BloomBadge
import com.example.ui.components.BloomCard
import com.example.ui.components.CategoryBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelBlueLight
import com.example.ui.theme.SoftPink
import com.example.ui.theme.SoftPinkLight
import java.text.SimpleDateFormat
import java.util.Date
import com.example.data.sync.SyncState
import com.example.ui.components.ConnectionStatusPill
import com.example.util.NetworkState
import java.util.Locale

@Composable
fun HomeScreen(
    userProfile: UserProfileEntity,
    todayTasks: List<StudyTaskEntity>,
    networkState: NetworkState = NetworkState(),
    syncState: SyncState = SyncState(),
    onOpenConnectionDetails: () -> Unit = {},
    onToggleTaskComplete: (StudyTaskEntity) -> Unit,
    onSkipTask: (StudyTaskEntity) -> Unit,
    onStartFocusForTask: (StudyTaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onRescheduleTask: (StudyTaskEntity, String, String, String) -> Unit,
    onOpenAddTask: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenFocusTimer: () -> Unit,
    onExportPdf: () -> Unit
) {
    val totalPlannedMinutes = todayTasks.sumOf { it.durationMinutes }
    val completedTasks = todayTasks.filter { it.isCompleted }
    val completedMinutes = completedTasks.sumOf { it.durationMinutes }
    val remainingMinutes = (totalPlannedMinutes - completedMinutes).coerceAtLeast(0)
    val progressPercent = if (totalPlannedMinutes > 0) (completedMinutes.toFloat() / totalPlannedMinutes.toFloat()) else 0f

    val niceDate = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        sdf.format(Date())
    }

    val motivationalMessage = when {
        todayTasks.isEmpty() -> "Your day is a blank canvas. Tap '+ Add Task' or 'AI Schedule' to start blooming!"
        progressPercent >= 1.0f -> "Outstanding achievement! You completed all planned sessions today. Take time to relax! 🌸"
        progressPercent >= 0.5f -> "More than halfway there! Maintain your gentle rhythm, one session at a time."
        completedTasks.isNotEmpty() -> "Wonderful start! Consistency beats intensity every single day."
        else -> "Plan gently. Focus when ready. Every small step compounds into mastery."
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Brand Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_studybloom_icon),
                        contentDescription = "StudyBloom App Icon",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_studybloom_wordmark),
                        contentDescription = "StudyBloom Logo",
                        modifier = Modifier.height(30.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    // Connection Status Pill (Online / Offline / Syncing)
                    ConnectionStatusPill(
                        networkState = networkState,
                        syncState = syncState,
                        onClick = onOpenConnectionDetails
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Consistency Streak Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SoftPinkLight)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔥", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userProfile.currentStreakDays}d streak",
                            color = SoftPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Greeting Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, ${userProfile.nickname} 🌸",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = niceDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Offline Notice Banner
                if (!networkState.isConnected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    BloomCard(
                        backgroundColor = LavenderLight.copy(alpha = 0.6f),
                        borderColor = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenConnectionDetails)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = LavenderPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline Mode Active • Local Room Storage",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Active timers & all tasks run 100% offline. Changes sync automatically when reconnected.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Daily Progress Summary Card
            item {
                BloomCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_progress_card"),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Today's Study Bloom",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${completedTasks.size}/${todayTasks.size} sessions completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column {
                                        Text(text = "Completed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "${completedMinutes / 60}h ${completedMinutes % 60}m", fontWeight = FontWeight.Bold, color = MintGreen)
                                    }
                                    Column {
                                        Text(text = "Remaining", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "${remainingMinutes / 60}h ${remainingMinutes % 60}m", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column {
                                        Text(text = "Daily Goal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "${userProfile.dailyGoalMinutes / 60}h", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            // Circular Progress
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(72.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { progressPercent },
                                    modifier = Modifier.size(72.dp),
                                    strokeWidth = 7.dp,
                                    color = LavenderPrimary,
                                    trackColor = LavenderLight
                                )
                                Text(
                                    text = "${(progressPercent * 100).toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Motivational message box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "💬 $motivationalMessage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Action Buttons Row
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ActionChip(
                            icon = Icons.Default.Timer,
                            label = "Focus Timer",
                            backgroundColor = LavenderLight,
                            contentColor = LavenderPrimary,
                            onClick = onOpenFocusTimer
                        )
                    }
                    item {
                        ActionChip(
                            icon = Icons.Default.AutoAwesome,
                            label = "AI Schedule",
                            backgroundColor = PastelBlueLight,
                            contentColor = PastelBlue,
                            onClick = onOpenWizard
                        )
                    }
                    item {
                        ActionChip(
                            icon = Icons.Default.Add,
                            label = "Add Task",
                            backgroundColor = MintGreenLight,
                            contentColor = MintGreen,
                            onClick = onOpenAddTask
                        )
                    }
                    item {
                        ActionChip(
                            icon = Icons.Default.Download,
                            label = "Export PDF",
                            backgroundColor = SoftPinkLight,
                            contentColor = SoftPink,
                            onClick = onExportPdf
                        )
                    }
                }
            }

            // In-app note about StudyBloom intelligent scheduler
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LavenderLight.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🌿", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "StudyBloom Intelligent Scheduler active • Non-conflicting session planning",
                            fontSize = 11.sp,
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Schedule Section Header
            item {
                SectionHeader(
                    title = "Today's Schedule",
                    subtitle = "${todayTasks.size} sessions"
                ) {
                    Text(
                        text = "Ask AI Assistant",
                        color = LavenderPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenAssistant() }
                            .padding(4.dp)
                    )
                }
            }

            // Task Items
            if (todayTasks.isEmpty()) {
                item {
                    EmptyDayPlaceholder(onOpenWizard = onOpenWizard, onOpenAddTask = onOpenAddTask)
                }
            } else {
                items(todayTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { onToggleTaskComplete(task) },
                        onStartFocus = { onStartFocusForTask(task) },
                        onSkip = { onSkipTask(task) },
                        onDelete = { onDeleteTask(task.id) },
                        onReschedule = { newStart, newEnd ->
                            onRescheduleTask(task, task.date, newStart, newEnd)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp)) // padding for bottom bar
            }
        }

        // Floating action button for quick task addition
        FloatingActionButton(
            onClick = onOpenAddTask,
            containerColor = LavenderPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
                .testTag("home_fab_add_task")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
        }
    }
}

@Composable
fun TaskCard(
    task: StudyTaskEntity,
    onToggleComplete: () -> Unit,
    onStartFocus: () -> Unit,
    onSkip: () -> Unit,
    onDelete: () -> Unit,
    onReschedule: (String, String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val cardBorderColor by animateColorAsState(
        targetValue = if (task.isCompleted) MintGreen else MaterialTheme.colorScheme.outline,
        label = "border_color"
    )

    BloomCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        backgroundColor = if (task.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface,
        borderColor = cardBorderColor
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle completion",
                        tint = if (task.isCompleted) MintGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Time Range Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${task.startTime} - ${task.endTime}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                CategoryBadge(category = task.category)

                Spacer(modifier = Modifier.weight(1f))

                PriorityBadge(priority = task.priority)

                // Dropdown menu
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Start Focus Session") },
                            onClick = {
                                menuExpanded = false
                                onStartFocus()
                            },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = LavenderPrimary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Skip this task") },
                            onClick = {
                                menuExpanded = false
                                onSkip()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reschedule +1 hr") },
                            onClick = {
                                menuExpanded = false
                                val startH = task.startTime.substringBefore(":").toIntOrNull() ?: 10
                                val endH = task.endTime.substringBefore(":").toIntOrNull() ?: 11
                                val newS = String.format(Locale.US, "%02d:%s", (startH + 1) % 24, task.startTime.substringAfter(":"))
                                val newE = String.format(Locale.US, "%02d:%s", (endH + 1) % 24, task.endTime.substringAfter(":"))
                                onReschedule(newS, newE)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete task", color = Color.Red) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Task Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )

            // Subject & Description
            if (task.subject.isNotBlank() || task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${task.subject} • ${task.description.ifBlank { "Learning session" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom bar in card: Duration & Quick Focus button
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${task.durationMinutes} min",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (task.taskType.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.taskType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = LavenderPrimary
                    )
                }

                if (task.actualDurationMinutes > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Studied: ${task.actualDurationMinutes}m" + if (task.overtimeMinutes > 0) " (+${task.overtimeMinutes}m OT)" else "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MintGreen
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!task.isCompleted) {
                    Button(
                        onClick = onStartFocus,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("task_start_button_${task.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Start Timer", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EmptyDayPlaceholder(onOpenWizard: () -> Unit, onOpenAddTask: () -> Unit) {
    BloomCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🌱", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No study sessions scheduled for today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create a balanced study plan using StudyBloom's AI Scheduler, or add your tasks manually.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onOpenWizard,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "AI Schedule", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onOpenAddTask,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "+ Custom Task", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
