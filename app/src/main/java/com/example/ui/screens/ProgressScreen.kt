package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.ui.components.BloomBadge
import com.example.ui.components.BloomCard
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(
    userProfile: UserProfileEntity,
    sessions: List<StudySessionEntity>,
    totalMinutesAllTime: Int
) {
    val totalHours = String.format(Locale.US, "%.1f", totalMinutesAllTime / 60.0)

    val last7DaysData = remember(sessions) {
        val list = mutableListOf<Pair<String, Int>>() // Day label to minutes
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNameSdf = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = sdf.format(c.time)
            val dayName = dayNameSdf.format(c.time)
            val dayMinutes = sessions.filter { it.date == dStr }.sumOf { it.durationMinutes }
            list.add(dayName to dayMinutes)
        }
        list
    }

    val weeklyMinutes = last7DaysData.sumOf { it.second }
    val weeklyHours = String.format(Locale.US, "%.1f", weeklyMinutes / 60.0)

    // Subject breakdown
    val subjectBreakdown = remember(sessions) {
        val map = mutableMapOf<String, Int>()
        for (s in sessions) {
            val subj = s.subject.ifBlank { "General" }
            map[subj] = (map[subj] ?: 0) + s.durationMinutes
        }
        if (map.isEmpty()) {
            map["Python & Django"] = 120
            map["Data Structures"] = 60
        }
        map.toList().sortedByDescending { it.second }
    }

    // Coding vs Academic study
    val codingMinutes = remember(sessions) {
        sessions.filter {
            val s = it.subject.lowercase()
            s.contains("python") || s.contains("django") || s.contains("code") || s.contains("algo") || s.contains("react")
        }.sumOf { it.durationMinutes }.coerceAtLeast(60)
    }
    val academicMinutes = (weeklyMinutes - codingMinutes).coerceAtLeast(30)
    val totalCategory = codingMinutes + academicMinutes
    val codingPercent = (codingMinutes.toFloat() / totalCategory.toFloat()).coerceIn(0f, 1f)

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Progress & Growth",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Encouraging insights on your study consistency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BloomBadge(
                    text = "🔥 ${userProfile.currentStreakDays} days streak",
                    backgroundColor = SoftPinkLight,
                    textColor = SoftPink
                )
            }
        }

        // Summary Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Weekly Study",
                    value = "${weeklyHours}h",
                    subtitle = "$weeklyMinutes minutes",
                    accentColor = LavenderPrimary,
                    bgColor = LavenderLight
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "All-Time",
                    value = "${totalHours}h",
                    subtitle = "${sessions.size} logged sessions",
                    accentColor = PastelBlue,
                    bgColor = PastelBlueLight
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Sessions",
                    value = "${sessions.size}",
                    subtitle = "100% focused",
                    accentColor = MintGreen,
                    bgColor = MintGreenLight
                )
            }
        }

        // 7-Day Activity Chart
        item {
            BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "7-Day Study Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Daily focused minutes recorded by your Focus Timer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    val maxDayMinutes = (last7DaysData.maxOfOrNull { it.second } ?: 60).coerceAtLeast(60)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7DaysData.forEach { (day, mins) ->
                            val barHeightFraction = (mins.toFloat() / maxDayMinutes.toFloat()).coerceIn(0.08f, 1f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (mins > 0) "${mins}m" else "0",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((70 * barHeightFraction).dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (mins > 0) LavenderPrimary else LavenderLight)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Programming vs Academic Study
        item {
            BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Programming vs Academic Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Coding Practice: ${(codingPercent * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LavenderPrimary)
                        Text(text = "Theory & Academic: ${((1f - codingPercent) * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PastelBlue)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { codingPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = LavenderPrimary,
                        trackColor = PastelBlueLight
                    )
                }
            }
        }

        // Subject Breakdown Cards
        item {
            SectionHeader(title = "Subject Breakdown")
        }

        item {
            BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    subjectBreakdown.take(4).forEach { (subject, mins) ->
                        val pct = if (weeklyMinutes > 0) (mins.toFloat() / weeklyMinutes.toFloat()).coerceIn(0.1f, 1f) else 0.5f
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = subject, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(text = "${mins / 60}h ${mins % 60}m", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = LavenderPrimary,
                                trackColor = LavenderLight
                            )
                        }
                    }
                }
            }
        }

        // Tracked Task Sessions Breakdown (Planned vs Actual Focused Time)
        item {
            SectionHeader(title = "Recent Live Tracked Sessions", subtitle = "${sessions.size} recorded")
        }

        if (sessions.isEmpty()) {
            item {
                BloomCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No live sessions recorded yet. Start a task timer to track exact study minutes!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(sessions.take(6)) { session ->
                BloomCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.taskTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${session.subject} • ${session.date}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BloomBadge(
                                text = if (session.completedSuccessfully) "COMPLETED" else "STUDIED",
                                backgroundColor = if (session.completedSuccessfully) MintGreenLight else LavenderLight,
                                textColor = if (session.completedSuccessfully) MintGreen else LavenderPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Planned: ${if (session.plannedDurationMinutes > 0) "${session.plannedDurationMinutes}m" else "${session.durationMinutes}m"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Actual Focused: ${session.durationMinutes}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LavenderPrimary
                            )
                            if (session.overtimeMinutes > 0) {
                                Text(
                                    text = "Overtime: +${session.overtimeMinutes}m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SoftPink
                                )
                            }
                            if (session.pausedMinutes > 0) {
                                Text(
                                    text = "Paused: ${session.pausedMinutes}m",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Weekly Encouragement Card
        item {
            BloomCard(
                backgroundColor = MintGreenLight.copy(alpha = 0.6f),
                borderColor = MintGreen.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🌸", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gentle Weekly Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF027A48)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "You studied for ${weeklyHours} hours this week, logged ${sessions.size} focused sessions, and spent ${codingMinutes / 60}h practicing coding. Consistent effort grows into mastery!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    bgColor: Color
) {
    BloomCard(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
