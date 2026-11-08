package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.SoftPink
import com.example.util.PdfExporter
import java.io.File

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExportTodayPdf: () -> File,
    onExportWeeklyPdf: () -> File,
    onExportCsv: () -> String
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = LavenderPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Download & Share Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Generate printable schedules with checkbox tracking, duration breakdowns, and study goals:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: Today's Schedule PDF
                ExportOptionCard(
                    title = "Today's Schedule (PDF)",
                    description = "Printable A4 page with checkbox list & daily goals",
                    icon = Icons.Default.Description,
                    iconTint = LavenderPrimary,
                    modifier = Modifier.testTag("export_today_pdf_button"),
                    onClick = {
                        val file = onExportTodayPdf()
                        PdfExporter.sharePdfFile(context, file)
                        onDismiss()
                    }
                )

                // Option 2: 7-Day Weekly Schedule PDF
                ExportOptionCard(
                    title = "7-Day Weekly Master Plan (PDF)",
                    description = "Seven-day overview with time slots & hours",
                    icon = Icons.Default.TableChart,
                    iconTint = PastelBlue,
                    modifier = Modifier.testTag("export_weekly_pdf_button"),
                    onClick = {
                        val file = onExportWeeklyPdf()
                        PdfExporter.sharePdfFile(context, file)
                        onDismiss()
                    }
                )

                // Option 3: Spreadsheet CSV Export
                ExportOptionCard(
                    title = "Spreadsheet Export (CSV)",
                    description = "Raw tabular data for Excel or Google Sheets",
                    icon = Icons.Default.Share,
                    iconTint = SoftPink,
                    modifier = Modifier.testTag("export_csv_button"),
                    onClick = {
                        val csv = onExportCsv()
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, csv)
                            putExtra(Intent.EXTRA_SUBJECT, "StudyBloom_Schedule.csv")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Schedule CSV"))
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ExportOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = LavenderPrimary, modifier = Modifier.size(18.dp))
        }
    }
}
