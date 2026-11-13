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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.MultiGoalScheduleResult
import com.example.data.local.entity.LearningMaterialEntity
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.ui.components.BloomBadge
import com.example.ui.components.BloomCard
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenDark
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.PastelBlueDark
import com.example.ui.theme.PastelBlueLight
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.SoftPinkDark
import com.example.ui.theme.SoftPinkLight
import com.example.ui.viewmodel.ScheduleWizardData

@Composable
fun ScheduleWizardScreen(
    wizardData: ScheduleWizardData,
    materials: List<LearningMaterialEntity>,
    activeGoals: List<StudyGoalEntity> = emptyList(),
    onStepChange: (Int) -> Unit,
    onSetWizardMode: (String) -> Unit = {},
    onToggleGoalSelection: (Long) -> Unit = {},
    onSelectAllGoals: () -> Unit = {},
    onSetSubject: (String, Long?) -> Unit,
    onSetTarget: (String, String, Int) -> Unit,
    onSetSessionSettings: (Int, Int, String) -> Unit,
    onGenerate: () -> Unit,
    onRedistributeOverflow: () -> Unit = {},
    onSaveSchedule: () -> Unit,
    onClose: () -> Unit
) {
    var subjectInput by remember(wizardData.subject) { mutableStateOf(wizardData.subject) }
    var startDateInput by remember(wizardData.startDate) { mutableStateOf(wizardData.startDate) }
    var endDateInput by remember(wizardData.targetEndDate) { mutableStateOf(wizardData.targetEndDate) }
    var dailyMinutesInput by remember(wizardData.dailyMinutesTarget) { mutableStateOf(wizardData.dailyMinutesTarget) }
    var sessionDuration by remember(wizardData.sessionDuration) { mutableStateOf(wizardData.sessionDuration) }
    var breakDuration by remember(wizardData.breakDuration) { mutableStateOf(wizardData.breakDuration) }
    var priorityInput by remember(wizardData.priority) { mutableStateOf(wizardData.priority) }

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
                    text = "Step ${wizardData.step} of 5 • ${if (wizardData.wizardMode == "GOALS") "Multiple Study Goals" else "Subject / Material"}",
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
                    wizardMode = wizardData.wizardMode,
                    activeGoals = activeGoals,
                    selectedGoalIds = wizardData.selectedGoalIds,
                    subject = subjectInput,
                    materials = materials,
                    onModeChange = onSetWizardMode,
                    onToggleGoal = onToggleGoalSelection,
                    onSelectAllGoals = onSelectAllGoals,
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
                    isMultiGoalMode = wizardData.wizardMode == "GOALS",
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
                    multiGoalResult = wizardData.multiGoalResult,
                    onSave = onSaveSchedule,
                    onRegenerate = onGenerate,
                    onRedistributeOverflow = onRedistributeOverflow
                )
            }
        }

        if (wizardData.errorMessage != null && wizardData.step != 5) {
            Text(
                text = wizardData.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
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
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("wizard_next_button")
                ) {
                    Text("Next Step")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (wizardData.step == 4) {
                Button(
                    onClick = {
                        onSetTarget(startDateInput, endDateInput, dailyMinutesInput)
                        onSetSessionSettings(sessionDuration, breakDuration, priorityInput)
                        onGenerate()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("wizard_generate_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate AI Schedule", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onGenerate,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("wizard_regenerate_button")
                ) {
                    Text("Regenerate")
                }
            }
        }
    }
}

@Composable
fun Step1WhatToStudy(
    wizardMode: String,
    activeGoals: List<StudyGoalEntity>,
    selectedGoalIds: Set<Long>,
    subject: String,
    materials: List<LearningMaterialEntity>,
    onModeChange: (String) -> Unit,
    onToggleGoal: (Long) -> Unit,
    onSelectAllGoals: () -> Unit,
    onSubjectChange: (String) -> Unit,
    onSelectMaterial: (LearningMaterialEntity) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag("wizard_step1_list")
    ) {
        item {
            Text(
                text = "Question 1: What do you want to schedule?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Schedule multiple daily study goals together or generate sessions from a subject/material.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isGoals = wizardMode == "GOALS"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGoals) LavenderPrimary else LavenderLight)
                        .clickable { onModeChange("GOALS") }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("wizard_mode_goals"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Study Goals (${activeGoals.size})",
                        color = if (isGoals) Color.White else LavenderPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isGoals) LavenderPrimary else LavenderLight)
                        .clickable { onModeChange("MATERIAL") }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("wizard_mode_material"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Subject / Material",
                        color = if (!isGoals) Color.White else LavenderPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (wizardMode == "GOALS") {
            val selectedGoals = activeGoals.filter { it.id in selectedGoalIds }
            val totalRemainingMins = selectedGoals.sumOf { (it.estimatedMinutes - it.actualMinutes).coerceAtLeast(0) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Active Goals",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${selectedGoals.size} selected • ${formatWizardDuration(totalRemainingMins)} remaining to schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (activeGoals.isNotEmpty()) {
                        TextButton(
                            onClick = onSelectAllGoals,
                            modifier = Modifier.testTag("wizard_select_all_goals")
                        ) {
                            Text("Select All", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (activeGoals.isEmpty()) {
                item {
                    BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "No pending study goals yet",
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add multiple daily study goals on Home or My Plan (e.g., Python learning 2h, Coding practice 1h, DBMS prep 1h), or switch to Subject / Material mode above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(activeGoals, key = { it.id }) { goal ->
                    val isSelected = goal.id in selectedGoalIds
                    val remMins = (goal.estimatedMinutes - goal.actualMinutes).coerceAtLeast(0)
                    BloomCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleGoal(goal.id) },
                        backgroundColor = if (isSelected) LavenderLight.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleGoal(goal.id) },
                                colors = CheckboxDefaults.colors(checkedColor = LavenderPrimary),
                                modifier = Modifier.testTag("wizard_goal_checkbox_${goal.id}")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.title,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${goal.subject} • ${formatWizardDuration(remMins)} left • ${goal.difficulty}" +
                                        if (goal.deadlineDate.isNotBlank()) " • Due ${goal.deadlineDate}" else " • Target ${goal.preferredDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            PriorityBadge(priority = goal.priority)
                        }
                    }
                }
            }
        } else {
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
}

@Composable
fun Step2Availability(
    dailyMinutes: Int,
    onDailyMinutesChange: (Int) -> Unit
) {
    val firstRowPresets = listOf(60, 90, 120, 180)
    val secondRowPresets = listOf(240, 300, 360)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Question 2: Daily study target?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Multiple daily goals are prioritized within this daily target. Any overflow will be clearly flagged so you can spread it across days.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Daily Study Target: ${formatWizardDuration(dailyMinutes)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    firstRowPresets.forEach { mins ->
                        DailyTargetPresetBox(
                            mins = mins,
                            isSelected = dailyMinutes == mins,
                            modifier = Modifier.weight(1f),
                            onClick = { onDailyMinutesChange(mins) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    secondRowPresets.forEach { mins ->
                        DailyTargetPresetBox(
                            mins = mins,
                            isSelected = dailyMinutes == mins,
                            modifier = Modifier.weight(1f),
                            onClick = { onDailyMinutesChange(mins) }
                        )
                    }
                }
            }
        }

        BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Conflict-Free Slot Allocation", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Honors your enabled Availability Slots and avoids overlapping existing tasks.\n" +
                        "• Automatically inserts restorative breaks between consecutive sessions and balances hard topics with coding practice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailyTargetPresetBox(
    mins: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) LavenderPrimary else LavenderLight)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag("wizard_daily_target_$mins"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatWizardDuration(mins),
            color = if (isSelected) Color.White else LavenderPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun Step3Targets(
    startDate: String,
    endDate: String,
    priority: String,
    isMultiGoalMode: Boolean,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Question 3: Target schedule window?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Set Start = Target Date for single-day multi-goal planning, or widen Target Completion Date to distribute across multiple days.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = onStartChange,
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_start_date_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = onEndChange,
                    label = { Text("Target End Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_end_date_input")
                )
            }
        }

        if (!isMultiGoalMode) {
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
                                    .heightIn(min = 48.dp)
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
                                .heightIn(min = 48.dp)
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
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) MintGreen else MintGreenLight)
                                .clickable { onBreakChange(b) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${b}m",
                                color = if (isSel) Color.White else MintGreenDark,
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
    multiGoalResult: MultiGoalScheduleResult?,
    onSave: () -> Unit,
    onRegenerate: () -> Unit,
    onRedistributeOverflow: () -> Unit
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
                Text(
                    text = "Balancing priority, deadlines, difficulty, and breaks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("wizard_preview_list")
        ) {
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
                        enabled = tasks.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("wizard_save_schedule_button")
                    ) {
                        Text("Save to Plan", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (multiGoalResult != null) {
                if (multiGoalResult.overflowMinutes > 0 || multiGoalResult.warningMessage != null) {
                    item {
                        BloomCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wizard_overflow_warning"),
                            backgroundColor = SoftPinkLight
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Capacity overflow",
                                        tint = SoftPinkDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Overflow & Unscheduled Goals Detected",
                                        fontWeight = FontWeight.Bold,
                                        color = SoftPinkDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = multiGoalResult.warningMessage
                                        ?: "${formatWizardDuration(multiGoalResult.overflowMinutes)} could not fit cleanly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (multiGoalResult.suggestedMultiDayDates.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = onRedistributeOverflow,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("wizard_redistribute_days_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Distribute Across ${multiGoalResult.suggestedMultiDayDates.size} Extra Day(s)",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Goal Allocation Breakdown",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            multiGoalResult.allocations.forEach { alloc ->
                                val (bg, fg, label) = when (alloc.status) {
                                    "SCHEDULED" -> Triple(MintGreenLight, MintGreenDark, "Scheduled")
                                    "PARTIAL" -> Triple(PastelBlueLight, PastelBlueDark, "Partial (${formatWizardDuration(alloc.remainingMinutes)} left)")
                                    else -> Triple(SoftPinkLight, PriorityHigh, "Unscheduled")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = alloc.goalTitle,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${formatWizardDuration(alloc.scheduledMinutes)} / ${formatWizardDuration(alloc.requestedMinutes)} scheduled",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    BloomBadge(text = label, backgroundColor = bg, textColor = fg)
                                }
                            }
                        }
                    }
                }
            }

            if (tasks.isEmpty()) {
                item {
                    BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "No sessions fit in the selected window.", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try distributing across extra days, increasing your daily target, or adjusting your availability slots.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = onRegenerate) {
                                Text("Try Again")
                            }
                        }
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
                            Text(
                                text = "${task.date} • ${task.startTime} - ${task.endTime}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LavenderPrimary
                            )
                            Text(text = task.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = "${task.subject} • ${task.taskType} • ${task.durationMinutes}m",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PriorityBadge(priority = task.priority)
                    }
                }
            }
        }
    }
}

private fun formatWizardDuration(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val h = safe / 60
    val m = safe % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
