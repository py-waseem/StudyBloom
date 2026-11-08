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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelBlueLight
import com.example.ui.theme.SoftPinkLight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayTabItem(
    val dayName: String,
    val dayNumber: String,
    val fullDate: String,
    val isToday: Boolean
)

@Composable
fun MyPlanScreen(
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    dayTasks: List<StudyTaskEntity>,
    onToggleComplete: (StudyTaskEntity) -> Unit,
    onStartFocus: (StudyTaskEntity) -> Unit,
    onSkipTask: (StudyTaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onRescheduleTask: (StudyTaskEntity, String, String, String) -> Unit,
    onSmartRescheduleMissed: () -> Unit,
    onExportWeeklyPdf: () -> Unit,
    onOpenAddTask: () -> Unit,
    onOpenWizard: () -> Unit
) {
    val weekDays = remember {
        val list = mutableListOf<DayTabItem>()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val nameSdf = SimpleDateFormat("EEE", Locale.getDefault())
        val numSdf = SimpleDateFormat("d", Locale.getDefault())
        val fullSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 0 until 7) {
            val dStr = fullSdf.format(cal.time)
            list.add(
                DayTabItem(
                    dayName = nameSdf.format(cal.time),
                    dayNumber = numSdf.format(cal.time),
                    fullDate = dStr,
                    isToday = dStr == todayStr
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val totalDayMinutes = dayTasks.sumOf { it.durationMinutes }
    val completedCount = dayTasks.count { it.isCompleted }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Study Plan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "7-day master overview and gentle schedule management",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onExportWeeklyPdf,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("plan_export_pdf_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = LavenderPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "PDF", fontSize = 12.sp, color = LavenderPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 7-Day Horizontal Selector
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weekDays) { day ->
                    val isSelected = day.fullDate == selectedDate
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    isSelected -> LavenderPrimary
                                    day.isToday -> LavenderLight
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable { onSelectDate(day.fullDate) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("day_tab_${day.fullDate}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.dayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = day.dayNumber,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else if (day.isToday) LavenderPrimary else Color.Transparent)
                        )
                    }
                }
            }
        }

        // Daily Stat & Action Bar
        item {
            BloomCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Selected Date: $selectedDate",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${dayTasks.size} sessions • ${totalDayMinutes / 60}h ${totalDayMinutes % 60}m planned • $completedCount completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Smart Reschedule Missed Button
                    ElevatedButton(
                        onClick = onSmartRescheduleMissed,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = PastelBlueLight,
                            contentColor = PastelBlue
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Reschedule Missed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section Title
        item {
            SectionHeader(
                title = "Sessions for $selectedDate",
                subtitle = "${dayTasks.size} total"
            ) {
                OutlinedButton(
                    onClick = onOpenAddTask,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add Task", fontSize = 11.sp)
                }
            }
        }

        // Tasks List
        if (dayTasks.isEmpty()) {
            item {
                BloomCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🍃", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No sessions planned for this date", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Use the AI Schedule Wizard to generate a balanced multi-day plan or add custom tasks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onOpenWizard,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Generate Multi-Day Plan", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(dayTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggleComplete = { onToggleComplete(task) },
                    onStartFocus = { onStartFocus(task) },
                    onSkip = { onSkipTask(task) },
                    onDelete = { onDeleteTask(task.id) },
                    onReschedule = { newStart, newEnd ->
                        onRescheduleTask(task, task.date, newStart, newEnd)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
