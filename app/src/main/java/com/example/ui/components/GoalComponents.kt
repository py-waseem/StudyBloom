package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelBlueLight
import com.example.ui.theme.SoftPink
import com.example.ui.theme.SoftPinkLight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun formatDurationCompact(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val h = safe / 60
    val m = safe % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyGoalCard(
    goal: StudyGoalEntity,
    linkedTasks: List<StudyTaskEntity>,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onReschedule: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val activeScheduledMinutes = linkedTasks
        .filter { !it.isSkipped }
        .sumOf { it.durationMinutes }
    val statusLabel: String
    val statusBg: Color
    val statusTextColor: Color

    when {
        goal.isCompleted -> {
            statusLabel = "Completed"
            statusBg = MintGreenLight
            statusTextColor = MintGreen
        }
        activeScheduledMinutes >= goal.estimatedMinutes -> {
            statusLabel = "Scheduled (${formatDurationCompact(activeScheduledMinutes)})"
            statusBg = MintGreenLight
            statusTextColor = MintGreen
        }
        activeScheduledMinutes > 0 -> {
            statusLabel = "Partial (${formatDurationCompact(activeScheduledMinutes)}/${formatDurationCompact(goal.estimatedMinutes)})"
            statusBg = PastelBlueLight
            statusTextColor = PastelBlue
        }
        else -> {
            statusLabel = "Unscheduled"
            statusBg = SoftPinkLight
            statusTextColor = SoftPink
        }
    }

    val progressFraction = if (goal.estimatedMinutes > 0) {
        (goal.actualMinutes.toFloat() / goal.estimatedMinutes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val borderColor by animateColorAsState(
        targetValue = if (goal.isCompleted) MintGreen else MaterialTheme.colorScheme.outline,
        label = "goal_border_color"
    )

    BloomCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}"),
        backgroundColor = if (goal.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface,
        borderColor = borderColor
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("goal_toggle_${goal.id}")
                ) {
                    Icon(
                        imageVector = if (goal.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (goal.isCompleted) "Mark goal incomplete" else "Mark goal complete",
                        tint = if (goal.isCompleted) MintGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (goal.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = "${goal.subject} • Target ${formatDurationCompact(goal.estimatedMinutes)}" +
                            if (goal.deadlineDate.isNotBlank()) " • Due ${goal.deadlineDate}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("goal_menu_${goal.id}")
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Goal actions")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Goal") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = LavenderPrimary) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reschedule Date") },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = PastelBlue) },
                            onClick = {
                                menuExpanded = false
                                onReschedule()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (goal.isCompleted) "Mark Active" else "Mark Complete") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintGreen) },
                            onClick = {
                                menuExpanded = false
                                onToggleComplete()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Goal", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            if (goal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BloomBadge(text = statusLabel, backgroundColor = statusBg, textColor = statusTextColor)
                PriorityBadge(priority = goal.priority)
                CategoryBadge(category = goal.category)
                BloomBadge(
                    text = goal.difficulty,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Studied: ${formatDurationCompact(goal.actualMinutes)} / ${formatDurationCompact(goal.estimatedMinutes)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (goal.actualMinutes > 0) MintGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LavenderPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (goal.isCompleted) MintGreen else LavenderPrimary,
                trackColor = LavenderLight
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditGoalDialog(
    initialDate: String,
    goalToEdit: StudyGoalEntity? = null,
    onDismiss: () -> Unit,
    onSaveGoal: (
        title: String,
        description: String,
        subject: String,
        category: String,
        estimatedMinutes: Int,
        difficulty: String,
        priority: String,
        preferredDate: String,
        deadlineDate: String
    ) -> Unit
) {
    var title by remember(goalToEdit) { mutableStateOf(goalToEdit?.title ?: "") }
    var description by remember(goalToEdit) { mutableStateOf(goalToEdit?.description ?: "") }
    var subject by remember(goalToEdit) { mutableStateOf(goalToEdit?.subject ?: "") }
    var category by remember(goalToEdit) { mutableStateOf(goalToEdit?.category ?: "PROGRAMMING") }
    var estimatedMinutes by remember(goalToEdit) { mutableStateOf(goalToEdit?.estimatedMinutes ?: 60) }
    var difficulty by remember(goalToEdit) { mutableStateOf(goalToEdit?.difficulty ?: "MEDIUM") }
    var priority by remember(goalToEdit) { mutableStateOf(goalToEdit?.priority ?: "HIGH") }
    var preferredDate by remember(goalToEdit, initialDate) { mutableStateOf(goalToEdit?.preferredDate ?: initialDate) }
    var deadlineDate by remember(goalToEdit) { mutableStateOf(goalToEdit?.deadlineDate ?: "") }

    val durationPresets = listOf(30, 60, 90, 120, 180)
    val categories = listOf(
        "PROGRAMMING" to "Programming",
        "CODING_PRACTICE" to "Coding Practice",
        "UNIVERSITY" to "University",
        "REVISION" to "Revision",
        "PROJECT" to "Project",
        "READING" to "Reading"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (goalToEdit == null) "Add Daily Study Goal" else "Edit Study Goal",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title *") },
                    placeholder = { Text("e.g. Python learning, DBMS preparation") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_title_input")
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Topic") },
                    placeholder = { Text("e.g. Python, Algorithms, Math") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_subject_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Goal Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Estimated Duration: ${formatDurationCompact(estimatedMinutes)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    durationPresets.forEach { mins ->
                        val isSel = estimatedMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LavenderPrimary else LavenderLight)
                                .clickable { estimatedMinutes = mins }
                                .testTag("goal_duration_${mins}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatDurationCompact(mins),
                                fontSize = 11.sp,
                                color = if (isSel) Color.White else LavenderPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(text = "Priority", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                        val isSel = priority == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LavenderPrimary else LavenderLight)
                                .clickable { priority = p }
                                .testTag("goal_priority_$p"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                fontSize = 11.sp,
                                color = if (isSel) Color.White else LavenderPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(text = "Difficulty", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("EASY", "MEDIUM", "HARD").forEach { diff ->
                        val isSel = difficulty == diff
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MintGreen else MintGreenLight)
                                .clickable { difficulty = diff },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = diff,
                                fontSize = 11.sp,
                                color = if (isSel) Color.White else MintGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(text = "Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (key, label) ->
                        val isSel = category == key
                        Box(
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LavenderPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { category = key }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = preferredDate,
                        onValueChange = { preferredDate = it },
                        label = { Text("Study Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = deadlineDate,
                        onValueChange = { deadlineDate = it },
                        label = { Text("Deadline (Opt)") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSaveGoal(
                            title.trim(),
                            description.trim(),
                            subject.trim().ifBlank { title.trim() },
                            category,
                            estimatedMinutes,
                            difficulty,
                            priority,
                            preferredDate.trim().ifBlank { initialDate },
                            deadlineDate.trim()
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("goal_confirm_button")
            ) {
                Text(if (goalToEdit == null) "Save Goal" else "Update Goal")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteGoalConfirmationDialog(
    goal: StudyGoalEntity,
    linkedUncompletedCount: Int,
    onDismiss: () -> Unit,
    onConfirmDelete: (removeUncompletedTasks: Boolean) -> Unit
) {
    var removeUncompleted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Study Goal?", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Deleting '${goal.title}' will never remove your study timer history or completed sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (linkedUncompletedCount > 0) {
                    Text(
                        text = "This goal has $linkedUncompletedCount upcoming scheduled session(s). By default, they will remain on your schedule as standalone tasks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { removeUncompleted = !removeUncompleted }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = removeUncompleted,
                            onCheckedChange = { removeUncompleted = it },
                            colors = CheckboxDefaults.colors(checkedColor = LavenderPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Also delete $linkedUncompletedCount uncompleted scheduled session(s)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDelete(removeUncompleted)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("delete_goal_confirm_button")
            ) {
                Text("Delete Goal", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RescheduleGoalDialog(
    goal: StudyGoalEntity,
    onDismiss: () -> Unit,
    onConfirmReschedule: (newDate: String, moveUncompletedTasks: Boolean) -> Unit
) {
    var newDate by remember(goal) { mutableStateOf(goal.preferredDate) }
    var moveTasks by remember { mutableStateOf(true) }

    val quickDates = remember(goal.preferredDate) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val baseDate = try {
            sdf.parse(goal.preferredDate)
        } catch (_: Exception) {
            null
        }
        listOf(1 to "+1 Day", 2 to "+2 Days", 7 to "+1 Week").map { (days, label) ->
            val cal = Calendar.getInstance()
            if (baseDate != null) cal.time = baseDate
            cal.add(Calendar.DAY_OF_YEAR, days)
            sdf.format(cal.time) to label
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Move '${goal.title}' to another study day:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newDate,
                    onValueChange = { newDate = it },
                    label = { Text("New Study Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickDates.forEach { (dateVal, label) ->
                        OutlinedButton(
                            onClick = { newDate = dateVal },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = label, fontSize = 11.sp, color = LavenderPrimary)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { moveTasks = !moveTasks },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = moveTasks,
                        onCheckedChange = { moveTasks = it },
                        colors = CheckboxDefaults.colors(checkedColor = LavenderPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Also move uncompleted sessions for this goal",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newDate.isNotBlank()) {
                        onConfirmReschedule(newDate.trim(), moveTasks)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Save Date")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
