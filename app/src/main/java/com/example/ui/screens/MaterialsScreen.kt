package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StudyBloomDatabase
import com.example.data.local.entity.LearningMaterialEntity
import com.example.data.local.entity.TopicEntity
import com.example.ui.components.BloomBadge
import com.example.ui.components.BloomCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelBlueLight
import com.example.ui.theme.SoftPinkLight

@Composable
fun MaterialsScreen(
    materials: List<LearningMaterialEntity>,
    onUploadMaterial: (title: String, fileType: String, uri: String, fileSize: String, contentSample: String) -> Unit,
    onDeleteMaterial: (Long) -> Unit,
    onAddTopicToMaterial: (materialId: Long, title: String, difficulty: String, minutes: Int) -> Unit,
    onDeleteTopic: (Long) -> Unit,
    onGenerateScheduleFromMaterial: (LearningMaterialEntity) -> Unit
) {
    val context = LocalContext.current
    var selectedMaterialForDetail by remember { mutableStateOf<LearningMaterialEntity?>(null) }
    var showQuickSampleDialog by remember { mutableStateOf(false) }

    // Native Storage Access Framework file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Uploaded_Document.pdf"
            val fileType = when {
                fileName.endsWith(".pdf", ignoreCase = true) -> "PDF"
                fileName.endsWith(".docx", ignoreCase = true) -> "DOCX"
                fileName.endsWith(".txt", ignoreCase = true) -> "TXT"
                else -> "DOCUMENT"
            }

            // Extract sample text safely if possible
            val contentSample = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            } catch (_: Exception) {
                ""
            }

            onUploadMaterial(fileName, fileType, uri.toString(), "1.8 MB", contentSample)
        }
    }

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
                        text = "Learning Materials",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Upload textbooks, syllabi & notes for AI chapter extraction",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Upload button
                Button(
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "text/plain"
                            )
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                    modifier = Modifier.testTag("materials_upload_button")
                ) {
                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Sample Material Loader Card
        item {
            BloomCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = LavenderLight.copy(alpha = 0.5f),
                borderColor = LavenderPrimary.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = LavenderPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Material Analyzer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LavenderPrimary
                        )
                        Text(
                            text = "Extract chapters, topics, difficulty & prerequisites automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { showQuickSampleDialog = true },
                        modifier = Modifier.testTag("materials_try_sample_button")
                    ) {
                        Text(text = "+ Sample", fontWeight = FontWeight.Bold, color = LavenderPrimary)
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "My Documents",
                subtitle = "${materials.size} materials"
            )
        }

        if (materials.isEmpty()) {
            item {
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
                        Text(text = "📚", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No study materials uploaded yet", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Upload a PDF, DOCX, or TXT file, or choose from popular learning samples to generate an intelligent schedule.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showQuickSampleDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary)
                        ) {
                            Text(text = "Add Learning Sample")
                        }
                    }
                }
            }
        } else {
            items(materials, key = { it.id }) { material ->
                MaterialCard(
                    material = material,
                    onClickDetail = { selectedMaterialForDetail = material },
                    onDelete = { onDeleteMaterial(material.id) },
                    onGenerateSchedule = { onGenerateScheduleFromMaterial(material) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Material Detail & Topics Dialog
    if (selectedMaterialForDetail != null) {
        val mat = selectedMaterialForDetail!!
        val db = StudyBloomDatabase.getInstance(context)
        val topics by db.topicDao().getTopicsForMaterial(mat.id).collectAsState(initial = emptyList())

        MaterialTopicsDialog(
            material = mat,
            topics = topics,
            onDismiss = { selectedMaterialForDetail = null },
            onAddTopic = { title, diff, mins ->
                onAddTopicToMaterial(mat.id, title, diff, mins)
            },
            onDeleteTopic = { topicId ->
                onDeleteTopic(topicId)
            },
            onGenerateSchedule = {
                selectedMaterialForDetail = null
                onGenerateScheduleFromMaterial(mat)
            }
        )
    }

    // Quick Sample Materials Dialog
    if (showQuickSampleDialog) {
        AlertDialog(
            onDismissRequest = { showQuickSampleDialog = false },
            title = { Text(text = "Choose a Learning Material Sample", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select a textbook or curriculum sample to test AI chapter analysis and automatic schedule generation:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    SampleOptionCard(
                        title = "Python & Data Structures Mastery",
                        type = "PDF",
                        size = "3.8 MB",
                        onClick = {
                            showQuickSampleDialog = false
                            onUploadMaterial("Python & Data Structures Mastery.pdf", "PDF", "", "3.8 MB", "Arrays, Linked Lists, Stacks, Queues, Binary Search Trees, Sorting Algorithms, Big-O Complexity")
                        }
                    )

                    SampleOptionCard(
                        title = "Computer Architecture & Operating Systems",
                        type = "PDF",
                        size = "5.1 MB",
                        onClick = {
                            showQuickSampleDialog = false
                            onUploadMaterial("Computer Architecture & Operating Systems.pdf", "PDF", "", "5.1 MB", "Processes, Threads, CPU Scheduling, Virtual Memory, Paging, Deadlocks, File Systems")
                        }
                    )

                    SampleOptionCard(
                        title = "Full-Stack React & Node.js Web Dev",
                        type = "PDF",
                        size = "2.9 MB",
                        onClick = {
                            showQuickSampleDialog = false
                            onUploadMaterial("Full-Stack React & Node.js Web Dev.pdf", "PDF", "", "2.9 MB", "Components, Props, State, Hooks, Context API, Express.js REST APIs, MongoDB Mongoose")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickSampleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MaterialCard(
    material: LearningMaterialEntity,
    onClickDetail: () -> Unit,
    onDelete: () -> Unit,
    onGenerateSchedule: () -> Unit
) {
    BloomCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("material_card_${material.id}"),
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (material.fileType) {
                                "PDF" -> Color(0xFFFEE4E2)
                                "DOCX" -> PastelBlueLight
                                else -> LavenderLight
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = when (material.fileType) {
                            "PDF" -> Color(0xFFD92D20)
                            "DOCX" -> PastelBlue
                            else -> LavenderPrimary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = material.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${material.fileType} • ${material.fileSizeFormatted} • Uploaded ${material.uploadDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }

            if (material.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = material.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BloomBadge(
                    text = "${material.estimatedTotalHours} hrs total",
                    backgroundColor = PastelBlueLight,
                    textColor = PastelBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                BloomBadge(
                    text = material.estimatedDifficulty,
                    backgroundColor = MintGreenLight,
                    textColor = MintGreen
                )

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = onClickDetail,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(text = "View Topics", fontSize = 11.sp, color = LavenderPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onGenerateSchedule,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MaterialTopicsDialog(
    material: LearningMaterialEntity,
    topics: List<TopicEntity>,
    onDismiss: () -> Unit,
    onAddTopic: (String, String, Int) -> Unit,
    onDeleteTopic: (Long) -> Unit,
    onGenerateSchedule: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newTopicTitle by remember { mutableStateOf("") }
    var newTopicDifficulty by remember { mutableStateOf("MEDIUM") }
    var newTopicMinutes by remember { mutableStateOf(60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = material.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "AI Extracted Chapters & Topics (${topics.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(topics, key = { it.id }) { topic ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = topic.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = "${topic.subtopicsRaw} • ${topic.estimatedMinutes} min",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        PriorityBadge(priority = topic.difficulty)

                        IconButton(onClick = { onDeleteTopic(topic.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Custom Topic", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGenerateSchedule,
                colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary)
            ) {
                Text("Create Schedule From Topics")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "Add Custom Topic") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTopicTitle,
                        onValueChange = { newTopicTitle = it },
                        label = { Text("Topic Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EASY", "MEDIUM", "HARD").forEach { diff ->
                            val isSel = newTopicDifficulty == diff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) LavenderLight else Color.LightGray.copy(alpha = 0.3f))
                                    .clickable { newTopicDifficulty = diff }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = diff, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTopicTitle.isNotBlank()) {
                            onAddTopic(newTopicTitle, newTopicDifficulty, newTopicMinutes)
                            newTopicTitle = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SampleOptionCard(
    title: String,
    type: String,
    size: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = LavenderPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(text = "$type • $size", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = LavenderPrimary)
        }
    }
}
