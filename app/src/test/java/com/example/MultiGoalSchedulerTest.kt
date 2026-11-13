package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.ai.AiRepositoryImpl
import com.example.data.ai.MultiGoalScheduleRequest
import com.example.data.ai.MultiGoalScheduleResult
import com.example.data.ai.ScheduledGoalAllocation
import com.example.data.local.entity.AvailabilitySlotEntity
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.ui.screens.ScheduleWizardScreen
import com.example.ui.theme.StudyBloomTheme
import com.example.ui.viewmodel.ScheduleWizardData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class MultiGoalSchedulerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val aiRepository = AiRepositoryImpl()

    private fun toMins(hhmm: String): Int {
        val parts = hhmm.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun assertNoOverlaps(tasks: List<StudyTaskEntity>) {
        tasks.groupBy { it.date }.forEach { (date, dayTasks) ->
            val sorted = dayTasks.sortedBy { toMins(it.startTime) }
            for (i in 0 until sorted.size - 1) {
                val curEnd = toMins(sorted[i].endTime)
                val nextStart = toMins(sorted[i + 1].startTime)
                assertTrue(
                    "Overlap on $date: ${sorted[i].startTime}-${sorted[i].endTime} and ${sorted[i + 1].startTime}-${sorted[i + 1].endTime}",
                    curEnd <= nextStart
                )
            }
        }
    }

    @Test
    fun schedules_multiple_daily_goals_on_same_day_without_overlaps_and_with_breaks() = runBlocking {
        val date = "2026-11-12"
        val goals = listOf(
            StudyGoalEntity(id = 1, title = "Python learning", subject = "Python", category = "PROGRAMMING", estimatedMinutes = 120, difficulty = "HARD", priority = "HIGH", preferredDate = date),
            StudyGoalEntity(id = 2, title = "Coding practice", subject = "DSA", category = "CODING_PRACTICE", estimatedMinutes = 60, difficulty = "MEDIUM", priority = "HIGH", preferredDate = date),
            StudyGoalEntity(id = 3, title = "DBMS preparation", subject = "DBMS", category = "UNIVERSITY", estimatedMinutes = 60, difficulty = "MEDIUM", priority = "MEDIUM", preferredDate = date),
            StudyGoalEntity(id = 4, title = "Math revision", subject = "Math", category = "REVISION", estimatedMinutes = 60, difficulty = "EASY", priority = "LOW", preferredDate = date)
        )

        val result = aiRepository.generateMultiGoalSchedule(
            MultiGoalScheduleRequest(
                goals = goals,
                existingTasks = emptyList(),
                startDate = date,
                targetEndDate = date,
                dailyGoalMinutes = 300,
                sessionDurationMinutes = 60,
                breakDurationMinutes = 15,
                availabilitySlots = emptyList()
            )
        ).getOrThrow()

        assertEquals(0, result.overflowMinutes)
        assertTrue(result.unscheduledGoals.isEmpty())
        assertTrue(result.partialGoals.isEmpty())
        assertEquals(setOf(1L, 2L, 3L, 4L), result.scheduledGoalIds)
        assertEquals(300, result.tasks.sumOf { it.durationMinutes })
        assertTrue(result.tasks.all { it.date == date && it.goalId != null })
        assertNoOverlaps(result.tasks)
    }

    @Test
    fun respects_priorities_deadlines_and_avoids_overlapping_existing_tasks() = runBlocking {
        // 2026-11-12 is Thursday (dayOfWeek = 4)
        val date = "2026-11-12"
        val existingTask = StudyTaskEntity(
            id = 99,
            title = "Morning Standup Study",
            subject = "General",
            date = date,
            startTime = "08:00",
            endTime = "09:00",
            durationMinutes = 60
        )
        val slots = listOf(
            AvailabilitySlotEntity(dayOfWeek = 4, startTime = "07:00", endTime = "12:00", isEnabled = true)
        )
        val goals = listOf(
            StudyGoalEntity(id = 1, title = "Low Priority Reading", subject = "Literature", estimatedMinutes = 90, priority = "LOW", preferredDate = date, deadlineDate = "2026-11-20"),
            StudyGoalEntity(id = 2, title = "Urgent Exam Prep", subject = "Algorithms", estimatedMinutes = 90, priority = "HIGH", preferredDate = date, deadlineDate = "2026-11-13")
        )

        val result = aiRepository.generateMultiGoalSchedule(
            MultiGoalScheduleRequest(
                goals = goals,
                existingTasks = listOf(existingTask),
                startDate = date,
                targetEndDate = date,
                dailyGoalMinutes = 180, // 60m already used by existingTask -> 120m left
                sessionDurationMinutes = 45,
                breakDurationMinutes = 15,
                availabilitySlots = slots
            )
        ).getOrThrow()

        assertNoOverlaps(result.tasks + existingTask)
        val highAlloc = result.allocations.first { it.goalId == 2L }
        val lowAlloc = result.allocations.first { it.goalId == 1L }
        assertEquals("SCHEDULED", highAlloc.status)
        assertEquals(90, highAlloc.scheduledMinutes)
        assertTrue("High priority urgent goal must be prioritized ahead of low priority overflow", lowAlloc.scheduledMinutes < 90)
        assertEquals(60, result.overflowMinutes)
        assertNotNull(result.warningMessage)
    }

    @Test
    fun detects_capacity_overflow_and_supports_multi_day_redistribution() = runBlocking {
        val startDate = "2026-11-12"
        val goals = listOf(
            StudyGoalEntity(id = 1, title = "Python Backend", subject = "Python", estimatedMinutes = 120, priority = "HIGH", preferredDate = startDate),
            StudyGoalEntity(id = 2, title = "DSA Graphs", subject = "DSA", estimatedMinutes = 120, priority = "MEDIUM", preferredDate = startDate),
            StudyGoalEntity(id = 3, title = "DBMS Indexing", subject = "DBMS", estimatedMinutes = 120, priority = "LOW", preferredDate = startDate)
        )

        // Single-day attempt with 180m daily target for 360m total goals -> 180m overflow
        val singleDayResult = aiRepository.generateMultiGoalSchedule(
            MultiGoalScheduleRequest(
                goals = goals,
                startDate = startDate,
                targetEndDate = startDate,
                dailyGoalMinutes = 180,
                sessionDurationMinutes = 60,
                breakDurationMinutes = 15
            )
        ).getOrThrow()

        assertEquals(180, singleDayResult.overflowMinutes)
        assertTrue(singleDayResult.unscheduledGoals.isNotEmpty() || singleDayResult.partialGoals.isNotEmpty())
        assertNotNull(singleDayResult.warningMessage)
        assertTrue(singleDayResult.suggestedMultiDayDates.isNotEmpty())

        // Expand window to suggested multi-day end date -> schedules all 360m cleanly
        val expandedEndDate = singleDayResult.suggestedMultiDayDates.last()
        val multiDayResult = aiRepository.generateMultiGoalSchedule(
            MultiGoalScheduleRequest(
                goals = goals,
                startDate = startDate,
                targetEndDate = expandedEndDate,
                dailyGoalMinutes = 180,
                sessionDurationMinutes = 60,
                breakDurationMinutes = 15
            )
        ).getOrThrow()

        assertEquals(0, multiDayResult.overflowMinutes)
        assertTrue(multiDayResult.unscheduledGoals.isEmpty())
        assertEquals(360, multiDayResult.tasks.sumOf { it.durationMinutes })
        assertTrue(multiDayResult.tasks.map { it.date }.distinct().size >= 2)
        assertNoOverlaps(multiDayResult.tasks)
    }

    @Test
    fun schedule_wizard_step5_renders_overflow_warning_and_redistribute_action() {
        var redistributeClicked = false
        val overflowGoal = StudyGoalEntity(id = 3, title = "DBMS Indexing", subject = "DBMS", estimatedMinutes = 120, priority = "LOW", preferredDate = "2026-11-12")
        val wizardData = ScheduleWizardData(
            step = 5,
            wizardMode = "GOALS",
            selectedGoalIds = setOf(1L, 3L),
            startDate = "2026-11-12",
            targetEndDate = "2026-11-12",
            dailyMinutesTarget = 120,
            generatedPreview = listOf(
                StudyTaskEntity(id = 1, goalId = 1, title = "Python Backend", subject = "Python", priority = "HIGH", date = "2026-11-12", startTime = "07:00", endTime = "09:00", durationMinutes = 120)
            ),
            multiGoalResult = MultiGoalScheduleResult(
                tasks = listOf(
                    StudyTaskEntity(id = 1, goalId = 1, title = "Python Backend", subject = "Python", priority = "HIGH", date = "2026-11-12", startTime = "07:00", endTime = "09:00", durationMinutes = 120)
                ),
                allocations = listOf(
                    ScheduledGoalAllocation(1L, "Python Backend", 120, 120, 0, "SCHEDULED"),
                    ScheduledGoalAllocation(3L, "DBMS Indexing", 120, 0, 120, "UNSCHEDULED")
                ),
                scheduledGoalIds = setOf(1L),
                unscheduledGoals = listOf(overflowGoal),
                partialGoals = emptyList(),
                overflowMinutes = 120,
                totalRequestedMinutes = 240,
                totalAvailableCapacityMinutes = 120,
                warningMessage = "Capacity overflow (2h unscheduled): DBMS Indexing could not fully fit within your daily target.",
                suggestedMultiDayDates = listOf("2026-11-13")
            )
        )

        composeTestRule.setContent {
            StudyBloomTheme(themeMode = "LIGHT") {
                ScheduleWizardScreen(
                    wizardData = wizardData,
                    materials = emptyList(),
                    activeGoals = listOf(overflowGoal),
                    onStepChange = {},
                    onSetSubject = { _, _ -> },
                    onSetTarget = { _, _, _ -> },
                    onSetSessionSettings = { _, _, _ -> },
                    onGenerate = {},
                    onRedistributeOverflow = { redistributeClicked = true },
                    onSaveSchedule = {},
                    onClose = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("wizard_overflow_warning").assertIsDisplayed()
        composeTestRule.onNodeWithText("Overflow & Unscheduled Goals Detected").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wizard_redistribute_days_button").performClick()
        assertTrue(redistributeClicked)
    }
}
