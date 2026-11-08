package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entity.StudyTaskEntity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH = 595 // A4 standard width in points at 72 dpi
    private const val PAGE_HEIGHT = 842 // A4 standard height

    fun generateDailySchedulePdf(
        context: Context,
        userName: String,
        date: String,
        dailyGoalMinutes: Int,
        tasks: List<StudyTaskEntity>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(122, 90, 248) // LavenderPrimary
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(71, 84, 103)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerBoxPaint = Paint().apply {
            color = Color.rgb(244, 241, 235) // Cream accent
            style = Paint.Style.FILL
        }

        val textPaint = Paint().apply {
            color = Color.rgb(29, 41, 57)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(29, 41, 57)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(237, 232, 225)
            strokeWidth = 1f
        }

        val completedPaint = Paint().apply {
            color = Color.rgb(50, 213, 131)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val pendingPaint = Paint().apply {
            color = Color.rgb(247, 144, 9)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Top Header Banner
        canvas.drawRoundRect(30f, 30f, (PAGE_WIDTH - 30).toFloat(), 115f, 12f, 12f, headerBoxPaint)
        canvas.drawText("StudyBloom • Daily Study Schedule", 45f, 65f, titlePaint)
        canvas.drawText("Plan gently. Learn consistently. Grow every day.", 45f, 85f, subtitlePaint)
        canvas.drawText("Learner: $userName  |  Date: $date  |  Goal: ${dailyGoalMinutes / 60}h ${dailyGoalMinutes % 60}m  |  Total Tasks: ${tasks.size}", 45f, 102f, subtitlePaint)

        // Table Header
        var y = 145f
        canvas.drawRect(30f, y - 18f, (PAGE_WIDTH - 30).toFloat(), y + 8f, headerBoxPaint)
        canvas.drawText("STATUS", 35f, y, boldTextPaint)
        canvas.drawText("TIME", 85f, y, boldTextPaint)
        canvas.drawText("TOPIC / TASK", 150f, y, boldTextPaint)
        canvas.drawText("SUBJECT", 330f, y, boldTextPaint)
        canvas.drawText("DURATION", 440f, y, boldTextPaint)
        canvas.drawText("PRIORITY", 510f, y, boldTextPaint)

        y += 24f

        val totalMinutes = tasks.sumOf { it.durationMinutes }

        for (task in tasks) {
            if (y > PAGE_HEIGHT - 60) break

            // Checkbox & Status
            val statusText = if (task.isCompleted) "[✔] DONE" else "[  ] PENDING"
            val statusStyle = if (task.isCompleted) completedPaint else pendingPaint
            canvas.drawText(statusText, 35f, y, statusStyle)

            // Time
            canvas.drawText("${task.startTime} - ${task.endTime}", 85f, y, textPaint)

            // Title
            val titleSnippet = if (task.title.length > 28) task.title.take(26) + "..." else task.title
            canvas.drawText(titleSnippet, 150f, y, boldTextPaint)

            // Subject
            val subjSnippet = if (task.subject.length > 18) task.subject.take(16) + "..." else task.subject
            canvas.drawText(subjSnippet, 330f, y, textPaint)

            // Duration
            canvas.drawText("${task.durationMinutes} min", 440f, y, textPaint)

            // Priority
            canvas.drawText(task.priority, 510f, y, textPaint)

            // Description sub-line if available
            if (task.description.isNotBlank()) {
                val descSnippet = if (task.description.length > 55) task.description.take(52) + "..." else task.description
                canvas.drawText("↳ $descSnippet", 150f, y + 12f, subtitlePaint)
                canvas.drawLine(30f, y + 20f, (PAGE_WIDTH - 30).toFloat(), y + 20f, linePaint)
                y += 30f
            } else {
                canvas.drawLine(30f, y + 8f, (PAGE_WIDTH - 30).toFloat(), y + 8f, linePaint)
                y += 22f
            }
        }

        // Summary footer
        y = (PAGE_HEIGHT - 45).toFloat()
        canvas.drawLine(30f, y - 10f, (PAGE_WIDTH - 30).toFloat(), y - 10f, linePaint)
        val hours = String.format(Locale.US, "%.1f", totalMinutes / 60.0)
        canvas.drawText("Total Planned Study Time: $hours hours ($totalMinutes minutes)  •  Page 1 of 1", 35f, y + 5f, subtitlePaint)
        canvas.drawText("Generated by StudyBloom Android", (PAGE_WIDTH - 190).toFloat(), y + 5f, subtitlePaint)

        document.finishPage(page)

        val outputFile = File(context.cacheDir, "StudyBloom_Daily_Schedule_$date.pdf")
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        return outputFile
    }

    fun generateWeeklySchedulePdf(
        context: Context,
        userName: String,
        startDate: String,
        tasks: List<StudyTaskEntity>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(122, 90, 248)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(71, 84, 103)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerBoxPaint = Paint().apply {
            color = Color.rgb(244, 241, 235)
            style = Paint.Style.FILL
        }

        val textPaint = Paint().apply {
            color = Color.rgb(29, 41, 57)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(29, 41, 57)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(237, 232, 225)
            strokeWidth = 1f
        }

        // Header
        canvas.drawRoundRect(30f, 30f, (PAGE_WIDTH - 30).toFloat(), 110f, 12f, 12f, headerBoxPaint)
        canvas.drawText("StudyBloom • 7-Day Weekly Master Plan", 45f, 62f, titlePaint)
        canvas.drawText("Week Starting: $startDate  |  Learner: $userName", 45f, 80f, subtitlePaint)
        val totalMins = tasks.sumOf { it.durationMinutes }
        val completedCount = tasks.count { it.isCompleted }
        canvas.drawText("Total Planned: ${String.format(Locale.US, "%.1f", totalMins / 60.0)} hrs  |  Completed: $completedCount/${tasks.size} tasks", 45f, 96f, subtitlePaint)

        var y = 135f
        canvas.drawRect(30f, y - 16f, (PAGE_WIDTH - 30).toFloat(), y + 6f, headerBoxPaint)
        canvas.drawText("DATE", 35f, y, boldTextPaint)
        canvas.drawText("TIME", 105f, y, boldTextPaint)
        canvas.drawText("TOPIC / TASK", 175f, y, boldTextPaint)
        canvas.drawText("SUBJECT", 360f, y, boldTextPaint)
        canvas.drawText("DURATION", 460f, y, boldTextPaint)
        canvas.drawText("STATUS", 520f, y, boldTextPaint)

        y += 20f

        val groupedTasks = tasks.groupBy { it.date }

        for ((date, dayTasks) in groupedTasks) {
            if (y > PAGE_HEIGHT - 60) break

            canvas.drawRect(30f, y - 12f, (PAGE_WIDTH - 30).toFloat(), y + 4f, headerBoxPaint)
            canvas.drawText("📅 $date (${dayTasks.size} sessions, ${dayTasks.sumOf { it.durationMinutes }}m)", 35f, y, boldTextPaint)
            y += 18f

            for (task in dayTasks) {
                if (y > PAGE_HEIGHT - 60) break

                canvas.drawText(task.date.takeLast(5), 35f, y, textPaint)
                canvas.drawText("${task.startTime}-${task.endTime}", 105f, y, textPaint)

                val titleSnippet = if (task.title.length > 30) task.title.take(28) + "..." else task.title
                canvas.drawText(titleSnippet, 175f, y, boldTextPaint)

                val subjSnippet = if (task.subject.length > 16) task.subject.take(14) + "..." else task.subject
                canvas.drawText(subjSnippet, 360f, y, textPaint)

                canvas.drawText("${task.durationMinutes}m", 460f, y, textPaint)

                val status = if (task.isCompleted) "DONE" else "PENDING"
                canvas.drawText(status, 520f, y, textPaint)

                canvas.drawLine(30f, y + 4f, (PAGE_WIDTH - 30).toFloat(), y + 4f, linePaint)
                y += 18f
            }
            y += 4f
        }

        y = (PAGE_HEIGHT - 35).toFloat()
        canvas.drawText("Generated by StudyBloom Android  •  Keep growing every day", 35f, y, subtitlePaint)
        document.finishPage(page)

        val outputFile = File(context.cacheDir, "StudyBloom_Weekly_Schedule_$startDate.pdf")
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        return outputFile
    }

    fun generateCsvExport(tasks: List<StudyTaskEntity>): String {
        val sb = StringBuilder()
        sb.append("Date,StartTime,EndTime,Title,Subject,Category,DurationMinutes,Priority,Status,Objective\n")
        for (task in tasks) {
            val status = if (task.isCompleted) "Completed" else if (task.isSkipped) "Skipped" else "Pending"
            sb.append("\"${task.date}\",")
            sb.append("\"${task.startTime}\",")
            sb.append("\"${task.endTime}\",")
            sb.append("\"${task.title.replace("\"", "\"\"")}\",")
            sb.append("\"${task.subject.replace("\"", "\"\"")}\",")
            sb.append("\"${task.category}\",")
            sb.append("${task.durationMinutes},")
            sb.append("\"${task.priority}\",")
            sb.append("\"$status\",")
            sb.append("\"${task.learningObjective.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    fun sharePdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share StudyBloom Schedule"))
    }
}
