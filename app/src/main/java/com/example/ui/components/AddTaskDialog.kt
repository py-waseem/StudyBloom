package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary

@Composable
fun AddTaskDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onSaveTask: (
        title: String,
        description: String,
        category: String,
        subject: String,
        priority: String,
        date: String,
        startTime: String,
        durationMinutes: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Programming") }
    var category by remember { mutableStateOf("PROGRAMMING") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var date by remember { mutableStateOf(initialDate) }
    var startTime by remember { mutableStateOf("09:00") }
    var durationMinutes by remember { mutableStateOf(45) }

    val categories = listOf("PROGRAMMING", "CODING_PRACTICE", "UNIVERSITY", "PROJECT", "REVISION", "READING")
    val durations = listOf(25, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add Study Task", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Django ORM Practice") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_task_title_input")
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Course") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(text = "Duration: $durationMinutes min", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    durations.forEach { d ->
                        val isSel = durationMinutes == d
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LavenderPrimary else LavenderLight)
                                .clickable { durationMinutes = d }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "${d}m", fontSize = 11.sp, color = if (isSel) Color.White else LavenderPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(text = "Priority", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                        val isSel = priority == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LavenderPrimary else LavenderLight)
                                .clickable { priority = p }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = p, fontSize = 11.sp, color = if (isSel) Color.White else LavenderPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSaveTask(title, description, category, subject, priority, date, startTime, durationMinutes)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                modifier = Modifier.testTag("add_task_confirm_button")
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
