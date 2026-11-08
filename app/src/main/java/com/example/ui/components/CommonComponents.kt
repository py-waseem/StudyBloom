package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CategoryPersonal
import com.example.ui.theme.CategoryProgramming
import com.example.ui.theme.CategoryProject
import com.example.ui.theme.CategoryReading
import com.example.ui.theme.CategoryRevision
import com.example.ui.theme.CategoryUniversity
import com.example.ui.theme.LavenderLight
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelBlueLight
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.SoftPink
import com.example.ui.theme.SoftPinkLight

@Composable
fun BloomCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    }
}

@Composable
fun BloomBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = LavenderLight,
    textColor: Color = LavenderPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (bgColor, txtColor) = when (priority.uppercase()) {
        "HIGH" -> Color(0xFFFEE4E2) to PriorityHigh
        "MEDIUM" -> Color(0xFFFEF0C7) to PriorityMedium
        else -> MintGreenLight to PriorityLow
    }
    BloomBadge(
        text = priority.uppercase(),
        backgroundColor = bgColor,
        textColor = txtColor
    )
}

@Composable
fun CategoryBadge(category: String) {
    val (label, bgColor, txtColor) = when (category.uppercase()) {
        "PROGRAMMING" -> Triple("Coding", LavenderLight, LavenderPrimary)
        "CODING_PRACTICE" -> Triple("Practice", Color(0xFFEDE9FE), Color(0xFF6941C6))
        "UNIVERSITY" -> Triple("Academic", PastelBlueLight, PastelBlue)
        "PROJECT" -> Triple("Project", Color(0xFFFFFAEB), CategoryProject)
        "REVISION" -> Triple("Revision", MintGreenLight, Color(0xFF027A48))
        "READING" -> Triple("Reading", SoftPinkLight, SoftPink)
        else -> Triple("Personal", Color(0xFFF2F4F7), CategoryPersonal)
    }
    BloomBadge(
        text = label,
        backgroundColor = bgColor,
        textColor = txtColor
    )
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(LavenderPrimary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "($subtitle)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        action?.invoke()
    }
}
