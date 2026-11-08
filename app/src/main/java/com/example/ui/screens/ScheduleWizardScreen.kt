package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.LearningMaterialEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.ui.components.BloomBadge
import com.example.ui.components.BloomCard
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelBlueLight
import com.example.ui.theme.SoftPinkLight
import com.example.ui.viewmodel.ScheduleWizardData

@Composable
fun ScheduleWizardScreen(
    wizardData: ScheduleWizardData,
    materials: List<LearningMaterialEntity>,
    onStepChange: (Int) -> Unit,
    onSetSubject: (String, Long?) -> Unit,
    onSetTarget: (String, String, Int) -> Unit,
    onSetSessionSettings: (Int, Int, String) -> Unit,
    onGenerate: () -> Unit,
    onSaveSchedule: () -> Unit,
    onClose: () -> Unit
) {
    var subjectInput by remember { mutableStateOf(wizardData.subject) }
    var startDateInput by remember { mutableStateOf(wizardData.startDate) }
    var endDateInput by remember { mutableStateOf(wizardData.targetEndDate) }
    var dailyMinutesInput by remember { mutableStateOf(wizardData.dailyMinutesTarget) }
    var sessionDuration by remember { mutableStateOf(wizardData.sessionDuration) }
    var breakDuration by remember { mutableStateOf(wizardData.breakDuration) }
    var priorityInput by remember { mutableStateOf(wizardData.priority) }

    val stepProgress = (wizardData.step) / 5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Schedule Creator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Step ${wizardData.step} of 5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LinearProgressIndicator(
            progress = { stepProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = LavenderPrimary,
            trackColor = LavenderLight
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Step Content
        Box(modifier = Modifier.weight(1f)) {
            when (wizardData.step) {
                1 -> Step1WhatToStudy(
                    subject = subjectInput,
                    materials = materials,
                    onSubjectChange = {
                        subjectInput = it
                        onSetSubject(it, null)
                    },
                    onSelectMaterial = { mat ->
                        subjectInput = mat.title
                        onSetSubject(mat.title, mat.id)
                    }
                )
                2 -> Step2Availability(
                    dailyMinutes = dailyMinutesInput,
                    onDailyMinutesChange = { dailyMinutesInput = it }
                )
                3 -> Step3Targets(
                    startDate = startDateInput,
                    endDate = endDateInput,
                    priority = priorityInput,
                    onStartChange = { startDateInput = it },
                    onEndChange = { endDateInput = it },
                    onPriorityChange = { priorityInput = it }
                )
                4 -> Step4SessionPreferences(
                    sessionDuration = sessionDuration,
                    breakDuration = breakDuration,
                    onSessionChange = { sessionDuration = it },
                    onBreakChange = { breakDuration = it }
                )
                5 -> Step5PreviewAndSave(
                    isGenerating = wizardData.isGenerating,
                    tasks = wizardData.generatedPreview,
                    onSave = onSaveSchedule,
                    onRegenerate = onGenerate
                )
            }
        }

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (wizardData.step > 1) {
                OutlinedButton(
                    onClick = { onStepChange(wizardData.step - 1) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (wizardData.step < 4) {
                Button(
                    onClick = {
                        onSetSubject(subjectInput, wizardData.selectedMaterialId)
                        onSetTarget(startDateInput, endDateInput, dailyMinutesInput)
                        onSetSessionSettings(sessionDuration, breakDuration, priorityInput)
                        onStepChange(wizardData.step + 1)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                    modifier = Modifier.testTag("wizard_next_button")
                ) {
                    Text("Next Step")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            } else if (wizardData.step == 4) {
                Button(
                    onClick = {
                        onSetSessionSettings(sessionDuration, breakDuration, priorityInput)
                        onGenerate()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                    modifier = Modifier.testTag("wizard_generate_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate AI Schedule", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun Step1WhatToStudy(
    subject: String,
    materials: List<LearningMaterialEntity>,
    onSubjectChange: (String) -> Unit,
    onSelectMaterial: (LearningMaterialEntity) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(
                text = "Question 1: What do you want to study?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pick an uploaded document syllabus or enter a subject / programming project manually.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Subject or Project Title", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = onSubjectChange,
                        placeholder = { Text("e.g. Django Development, Operating Systems, React Native") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_subject_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        if (materials.isNotEmpty()) {
            item {
                Text(text = "Or choose from your learning materials:", fontWeight = FontWeight.Bold)
            }

            items(materials) { mat ->
                BloomCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMaterial(mat) },
                    backgroundColor = if (subject == mat.title) LavenderLight else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📖", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = mat.title, fontWeight = FontWeight.SemiBold)
                            Text(text = "${mat.fileType} • ${mat.mainSubject}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (subject == mat.title) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = LavenderPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Step2Availability(
    dailyMinutes: Int,
    onDailyMinutesChange: (Int) -> Unit
) {
    val presets = listOf(60, 90, 120, 180, 240)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Question 2: How much time can you study per day?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "The AI will never schedule more than you can comfortably manage without fatigue.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Daily Study Target: ${dailyMinutes / 60}h ${dailyMinutes % 60}m", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { mins ->
                        val isSel = dailyMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) LavenderPrimary else LavenderLight)
                                .clickable { onDailyMinutesChange(mins) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins / 60}h" + if (mins % 60 != 0) " 30m" else "",
                                color = if (isSel) Color.White else LavenderPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Active Availability Slots", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Weekdays (Mon-Fri): 06:30-08:00 AM (Morning Focus) & 07:00-10:00 PM (Evening Deep Work)\n" +
                            "• Weekends (Sat-Sun): 09:00-12:00 PM & 03:00-06:00 PM\n" +
                            "The scheduler will intelligently fit tasks into these non-overlapping slots with built-in breaks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun Step3Targets(
    startDate: String,
    endDate: String,
    priority: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Question 3: What is your target timeline?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = onStartChange,
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = onEndChange,
                    label = { Text("Target Completion Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Goal Priority", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                        val isSel = priority == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LavenderPrimary else LavenderLight)
                                .clickable { onPriorityChange(p) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = p, color = if (isSel) Color.White else LavenderPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Step4SessionPreferences(
    sessionDuration: Int,
    breakDuration: Int,
    onSessionChange: (Int) -> Unit,
    onBreakChange: (Int) -> Unit
) {
    val sessionOptions = listOf(25, 45, 60, 90)
    val breakOptions = listOf(5, 10, 15, 20)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Question 4: Session & Break Durations",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Study Session Chunk: $sessionDuration minutes", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sessionOptions.forEach { dur ->
                        val isSel = sessionDuration == dur
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) LavenderPrimary else LavenderLight)
                                .clickable { onSessionChange(dur) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${dur}m",
                                color = if (isSel) Color.White else LavenderPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Break Duration: $breakDuration minutes", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    breakOptions.forEach { b ->
                        val isSel = breakDuration == b
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) MintGreen else MintGreenLight)
                                .clickable { onBreakChange(b) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${b}m",
                                color = if (isSel) Color.White else MintGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Step5PreviewAndSave(
    isGenerating: Boolean,
    tasks: List<StudyTaskEntity>,
    onSave: () -> Unit,
    onRegenerate: () -> Unit
) {
    if (isGenerating) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = LavenderPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "StudyBloom AI is orchestrating your schedule...", fontWeight = FontWeight.Medium)
                Text(text = "Balancing difficulty, prerequisites, and buffer breaks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI-Generated Schedule Preview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tasks.size} structured sessions ready to save to your planner",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onSave,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                        modifier = Modifier.testTag("wizard_save_schedule_button")
                    ) {
                        Text("Save to Plan", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(tasks) { task ->
                BloomCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${task.date} • ${task.startTime} - ${task.endTime}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LavenderPrimary)
                            Text(text = task.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "${task.subject} • ${task.taskType} • ${task.durationMinutes}m", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PriorityBadge(priority = task.priority)
                    }
                }
            }
        }
    }
}
