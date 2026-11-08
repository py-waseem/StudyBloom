package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BloomBadge
import com.example.ui.components.BloomCard
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.LavenderDark
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelBlueLight
import com.example.ui.theme.SoftPink
import com.example.ui.theme.SoftPinkLight
import com.example.util.ActiveTimerSnapshot
import com.example.util.TaskTimerManager
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TaskClockScreen(
    timerSnapshot: ActiveTimerSnapshot,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onExtend: (minutes: Int) -> Unit,
    onFinishTask: () -> Unit,
    onStopSession: () -> Unit,
    onClose: () -> Unit
) {
    var showFinishConfirmDialog by remember { mutableStateOf(false) }
    var showExtendSheet by remember { mutableStateOf(false) }

    val formattedElapsed = TaskTimerManager.formatMillis(timerSnapshot.actualFocusedMillis)
    val formattedRemaining = TaskTimerManager.formatMillis(timerSnapshot.remainingMillis)
    val formattedOvertime = TaskTimerManager.formatMillis(timerSnapshot.overtimeMillis)
    val actualStartTimeStr = TaskTimerManager.formatTimeOnly(timerSnapshot.actualStartTimestamp)

    val progressAnimated by animateFloatAsState(
        targetValue = timerSnapshot.progressFraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress_clock"
    )

    // Gentle pulse animation when running
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("clock_close_button")
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Clock", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (timerSnapshot.isPaused) SoftPinkLight else MintGreenLight)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (timerSnapshot.isPaused) SoftPink else MintGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (timerSnapshot.isPaused) "Session Paused" else "Focus Flowing",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (timerSnapshot.isPaused) SoftPink else Color(0xFF027A48)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showExtendSheet = true }) {
                Icon(imageVector = Icons.Default.MoreTime, contentDescription = "Extend session", tint = LavenderPrimary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Task Name & Subject Header
        Text(
            text = timerSnapshot.taskTitle.ifBlank { "Active Study Task" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${timerSnapshot.taskSubject} • Scheduled ${timerSnapshot.scheduledDurationMinutes} min",
            style = MaterialTheme.typography.bodyMedium,
            color = LavenderPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Scheduled vs Actual Start Time Note
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Scheduled: ${timerSnapshot.scheduledStartTime} – ${timerSnapshot.scheduledEndTime}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Started at $actualStartTimeStr",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Aesthetic Analog & Progress Clock Canvas
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
                .testTag("aesthetic_clock_canvas_box")
        ) {
            val primaryColor = LavenderPrimary
            val accentTrack = LavenderLight
            val mintGlow = MintGreen
            val goldOvertime = SoftPink

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) - 18.dp.toPx()

                // Outer decorative track
                drawCircle(
                    color = accentTrack.copy(alpha = 0.5f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active Progress Arc
                val sweepAngle = 360f * progressAnimated
                val strokeBrush = Brush.sweepGradient(
                    colors = if (timerSnapshot.isPlannedDurationCompleted) {
                        listOf(primaryColor, goldOvertime, primaryColor)
                    } else {
                        listOf(primaryColor, PastelBlue, mintGlow, primaryColor)
                    },
                    center = center
                )

                drawArc(
                    brush = strokeBrush,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )

                // Hour Tick Marks (12 ticks around clock perimeter)
                for (hour in 0 until 12) {
                    val angleDeg = hour * 30f - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val innerR = radius - 18.dp.toPx()
                    val outerR = radius - 8.dp.toPx()

                    val startX = center.x + (innerR * cos(angleRad)).toFloat()
                    val startY = center.y + (innerR * sin(angleRad)).toFloat()
                    val endX = center.x + (outerR * cos(angleRad)).toFloat()
                    val endY = center.y + (outerR * sin(angleRad)).toFloat()

                    drawLine(
                        color = if (hour % 3 == 0) primaryColor.copy(alpha = 0.7f) else primaryColor.copy(alpha = 0.25f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (hour % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Smooth Analog Hand (pointing to elapsed fraction of scheduled time)
                val handAngle = (progressAnimated * 360f) - 90f
                val handRad = Math.toRadians(handAngle.toDouble())
                val handLength = radius - 24.dp.toPx()
                val handEnd = Offset(
                    center.x + (handLength * cos(handRad)).toFloat(),
                    center.y + (handLength * sin(handRad)).toFloat()
                )

                drawLine(
                    color = primaryColor,
                    start = center,
                    end = handEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center pivot cap
                drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = center)
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = center)
            }

            // Central Digital Overlay Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedElapsed,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 1.sp
                )

                if (timerSnapshot.isPlannedDurationCompleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftPinkLight)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+ $formattedOvertime overtime",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftPink
                        )
                    }
                } else {
                    Text(
                        text = "$formattedRemaining remaining",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progressAnimated * 100).toInt()}% completed",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LavenderPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gentle Planned Duration Complete Prompt Card
        AnimatedVisibility(visible = timerSnapshot.isPlannedDurationCompleted) {
            BloomCard(
                backgroundColor = MintGreenLight.copy(alpha = 0.8f),
                borderColor = MintGreen.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✨", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Planned Session Completed!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF027A48)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You reached your scheduled ${timerSnapshot.scheduledDurationMinutes} minutes. Would you like to continue in overtime or finish and log your progress?",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onFinishTask,
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Finish Task", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onExtend(15) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Continue (+15m)", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Live Controls Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stop Session (archives time, leaves pending)
            IconButton(
                onClick = onStopSession,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("clock_stop_session_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop session",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Pause / Resume Main Button
            Button(
                onClick = {
                    if (timerSnapshot.isPaused) onResume() else onPause()
                },
                modifier = Modifier
                    .size(76.dp)
                    .testTag("clock_play_pause_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerSnapshot.isPaused) LavenderPrimary else LavenderLight
                )
            ) {
                Icon(
                    imageVector = if (timerSnapshot.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (timerSnapshot.isPaused) "Resume" else "Pause",
                    tint = if (timerSnapshot.isPaused) Color.White else LavenderPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Finish Task (marks complete and logs session)
            IconButton(
                onClick = { showFinishConfirmDialog = true },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MintGreenLight)
                    .testTag("clock_finish_task_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Finish task",
                    tint = MintGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Extend Session Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Extend session:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { onExtend(5) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = "+5m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = { onExtend(15) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = "+15m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = { onExtend(30) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = "+30m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Finish Confirmation Dialog
    if (showFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmDialog = false },
            title = { Text(text = "Finish & Complete Task?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will record your actual focused time ($formattedElapsed) into your study analytics and mark '${timerSnapshot.taskTitle}' as completed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishConfirmDialog = false
                        onFinishTask()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                ) {
                    Text("Complete Task ✨")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmDialog = false }) {
                    Text("Continue Studying")
                }
            }
        )
    }

    // Extend Session Sheet
    if (showExtendSheet) {
        AlertDialog(
            onDismissRequest = { showExtendSheet = false },
            title = { Text(text = "Extend Study Time") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Add extra planned minutes to this session:", fontSize = 12.sp)
                    listOf(5, 10, 15, 30).forEach { mins ->
                        Button(
                            onClick = {
                                onExtend(mins)
                                showExtendSheet = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LavenderLight)
                        ) {
                            Text(text = "+$mins Minutes", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExtendSheet = false }) { Text("Cancel") }
            }
        )
    }
}
