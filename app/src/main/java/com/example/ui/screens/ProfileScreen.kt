package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.local.entity.UserProfileEntity
import com.example.ui.components.BloomCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelBlueLight
import com.example.ui.theme.SoftPinkLight

@Composable
fun ProfileScreen(
    userProfile: UserProfileEntity,
    onSaveProfile: (UserProfileEntity) -> Unit,
    onTestNotification: () -> Unit
) {
    var nickname by remember(userProfile) { mutableStateOf(userProfile.nickname) }
    var purpose by remember(userProfile) { mutableStateOf(userProfile.purpose) }
    var dailyGoalHours by remember(userProfile) { mutableStateOf(userProfile.dailyGoalMinutes / 60) }
    var timeZone by remember(userProfile) { mutableStateOf(userProfile.timeZone) }
    var themeMode by remember(userProfile) { mutableStateOf(userProfile.themeMode) }
    var reminderEnabled by remember(userProfile) { mutableStateOf(userProfile.reminderEnabled) }
    var reminderBefore by remember(userProfile) { mutableStateOf(userProfile.reminderBeforeMinutes) }
    var showTimezonePicker by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onTestNotification()
        }
    }

    val timezoneList = listOf(
        "Asia/Kathmandu",
        "Asia/Kolkata",
        "UTC",
        "America/New_York",
        "America/Los_Angeles",
        "Europe/London",
        "Asia/Tokyo"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(LavenderLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌸", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nickname,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = purpose,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        val updated = userProfile.copy(
                            nickname = nickname,
                            purpose = purpose,
                            dailyGoalMinutes = dailyGoalHours * 60,
                            timeZone = timeZone,
                            themeMode = themeMode,
                            reminderEnabled = reminderEnabled,
                            reminderBeforeMinutes = reminderBefore
                        )
                        onSaveProfile(updated)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                    modifier = Modifier.testTag("profile_save_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Personal Details
        item {
            SectionHeader(title = "Profile & Study Focus")
        }

        item {
            BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Your Preferred Nickname") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_nickname_input")
                    )

                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("Primary Learning Goal / Purpose") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Daily Study Target: $dailyGoalHours hours", fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 3, 4, 5).forEach { h ->
                                val isSel = dailyGoalHours == h
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) LavenderPrimary else LavenderLight)
                                        .clickable { dailyGoalHours = h }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${h}h",
                                        color = if (isSel) Color.White else LavenderPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Theme & Appearance
        item {
            SectionHeader(title = "Appearance & Theme")
        }

        item {
            BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Theme Mode", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Gentle Light", "DARK" to "Cozy Dark").forEach { (mode, label) ->
                            val isSel = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) LavenderPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        themeMode = mode
                                        onSaveProfile(userProfile.copy(themeMode = mode))
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Time Zone
        item {
            SectionHeader(title = "Time Zone & Clock")
        }

        item {
            BloomCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                onClick = { showTimezonePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = LavenderPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Application Time Zone", fontWeight = FontWeight.SemiBold)
                        Text(text = timeZone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = "Change", color = LavenderPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Notifications & Reminders
        item {
            SectionHeader(title = "Reminders & Notifications")
        }

        item {
            BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = LavenderPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Session Reminders", fontWeight = FontWeight.SemiBold)
                            Text(text = "Alerts before scheduled study and coding blocks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = LavenderPrimary, checkedTrackColor = LavenderLight)
                        )
                    }

                    if (reminderEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Reminder Interval: $reminderBefore min before", fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(5, 10, 15, 30).forEach { m ->
                                    val isSel = reminderBefore == m
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) LavenderPrimary else LavenderLight)
                                            .clickable { reminderBefore = m }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${m}m",
                                            fontSize = 11.sp,
                                            color = if (isSel) Color.White else LavenderPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onTestNotification()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_test_notification_button")
                    ) {
                        Text(text = "Send Test Reminder Notification 🌿", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Privacy & Storage Architecture
        item {
            BloomCard(
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                borderColor = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MintGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Local-First Privacy Architecture", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = "All schedules, documents, and focus stats are kept on your device in encrypted local Room storage. No personal notes are sold or harvested.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section: About StudyBloom Brand
        item {
            BloomCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_studybloom_wordmark),
                        contentDescription = "StudyBloom Elegant Wordmark",
                        modifier = Modifier.height(38.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Plan gently. Learn consistently. Grow every day.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0 • Elegant Wordmark Edition",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LavenderPrimary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showTimezonePicker) {
        AlertDialog(
            onDismissRequest = { showTimezonePicker = false },
            title = { Text("Select Application Time Zone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timezoneList.forEach { tz ->
                        Card(
                            onClick = {
                                timeZone = tz
                                showTimezonePicker = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = if (timeZone == tz) LavenderLight else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tz,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = if (timeZone == tz) FontWeight.Bold else FontWeight.Normal,
                                color = if (timeZone == tz) LavenderPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTimezonePicker = false }) { Text("Cancel") }
            }
        )
    }
}
