package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StudyTaskEntity
import com.example.ui.components.BloomCard
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelBlueLight
import com.example.ui.theme.SoftPinkLight
import java.util.Locale

@Composable
fun FocusTimerScreen(
    isRunning: Boolean,
    remainingSeconds: Int,
    totalSeconds: Int,
    currentTask: StudyTaskEntity?,
    todayTasks: List<StudyTaskEntity>,
    focusMode: String,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onCompleteSession: () -> Unit,
    onSelectTask: (StudyTaskEntity?) -> Unit,
    onSetPreset: (minutes: Int, modeName: String) -> Unit,
    onClose: () -> Unit
) {
    var showTaskPicker by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    val progress = if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds.toFloat()) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timer_progress")

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "StudyBloom Focus",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetButton(
                title = "25m Pomodoro",
                isSelected = focusMode == "POMODORO" && totalSeconds == 25 * 60,
                modifier = Modifier.weight(1f),
                onClick = { onSetPreset(25, "POMODORO") }
            )
            PresetButton(
                title = "50m Deep Work",
                isSelected = focusMode == "DEEP_WORK" || totalSeconds == 50 * 60,
                modifier = Modifier.weight(1f),
                onClick = { onSetPreset(50, "DEEP_WORK") }
            )
            PresetButton(
                title = "10m Break",
                isSelected = focusMode == "SHORT_BREAK" || totalSeconds == 10 * 60,
                modifier = Modifier.weight(1f),
                onClick = { onSetPreset(10, "SHORT_BREAK") }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Current Task Card
        BloomCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTaskPicker = true },
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LavenderLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎯", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTask?.title ?: "No specific task linked",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (currentTask != null) "${currentTask.subject} • Tap to switch" else "Tap to link a scheduled task",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Circular Timer Display
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(240.dp),
                strokeWidth = 14.dp,
                color = LavenderLight
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(240.dp),
                strokeWidth = 14.dp,
                color = if (focusMode.contains("BREAK")) MintGreen else LavenderPrimary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isRunning) "Deep in focus 🌸" else "Ready to bloom",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Timer Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(imageVector = Icons.Default.Replay, contentDescription = "Reset timer", tint = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Play / Pause Button
            Button(
                onClick = {
                    if (isRunning) onPause() else onStart()
                },
                modifier = Modifier
                    .size(72.dp)
                    .testTag("focus_timer_toggle_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Stop & Log Button
            IconButton(
                onClick = {
                    showCompletionDialog = true
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("focus_timer_complete_button")
            ) {
                Icon(imageVector = Icons.Default.Stop, contentDescription = "End session", tint = Color.Red.copy(alpha = 0.8f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Task Picker Dialog
    if (showTaskPicker) {
        AlertDialog(
            onDismissRequest = { showTaskPicker = false },
            title = { Text("Select Task to Focus On") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onSelectTask(null)
                            showTaskPicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unlink (General Free Focus)")
                    }
                    todayTasks.forEach { task ->
                        BloomCard(
                            onClick = {
                                onSelectTask(task)
                                showTaskPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = task.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "${task.subject} • ${task.durationMinutes}m", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTaskPicker = false }) { Text("Close") }
            }
        )
    }

    // Completion Dialog
    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { showCompletionDialog = false },
            title = { Text("Complete Focus Session?") },
            text = {
                Text(
                    text = "Would you like to finish and record this study time into your StudyBloom progress analytics?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompletionDialog = false
                        onCompleteSession()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary)
                ) {
                    Text("Yes, Log Session ✨")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompletionDialog = false }) {
                    Text("Resume")
                }
            }
        )
    }
}

@Composable
fun PresetButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) LavenderPrimary else LavenderLight)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else LavenderPrimary
        )
    }
}
