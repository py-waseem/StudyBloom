package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.ui.components.AddEditGoalDialog
import com.example.ui.components.DeleteGoalConfirmationDialog
import com.example.ui.screens.MyPlanScreen
import com.example.ui.theme.StudyBloomTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class GoalUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun my_plan_screen_displays_multiple_daily_goals_with_scheduled_and_unscheduled_status() {
        val date = "2026-11-12"
        val goals = listOf(
            StudyGoalEntity(id = 1, title = "Python learning", subject = "Python", estimatedMinutes = 120, actualMinutes = 45, priority = "HIGH", preferredDate = date),
            StudyGoalEntity(id = 2, title = "Coding practice", subject = "DSA", estimatedMinutes = 60, actualMinutes = 0, priority = "HIGH", preferredDate = date),
            StudyGoalEntity(id = 3, title = "DBMS preparation", subject = "DBMS", estimatedMinutes = 120, actualMinutes = 0, priority = "MEDIUM", preferredDate = date)
        )
        val tasks = listOf(
            StudyTaskEntity(id = 10, title = "Python Decorators", subject = "Python", date = date, startTime = "09:00", endTime = "10:00", durationMinutes = 60, goalId = 1),
            StudyTaskEntity(id = 11, title = "Python Generators", subject = "Python", date = date, startTime = "10:15", endTime = "11:15", durationMinutes = 60, goalId = 1),
            StudyTaskEntity(id = 12, title = "Two Pointers", subject = "DSA", date = date, startTime = "19:00", endTime = "19:30", durationMinutes = 30, goalId = 2)
        )

        composeTestRule.setContent {
            StudyBloomTheme(themeMode = "LIGHT") {
                MyPlanScreen(
                    selectedDate = date,
                    onSelectDate = {},
                    dayTasks = tasks,
                    dayGoals = goals,
                    allTasks = tasks,
                    onToggleComplete = {},
                    onStartFocus = {},
                    onSkipTask = {},
                    onDeleteTask = {},
                    onRescheduleTask = { _, _, _, _ -> },
                    onSmartRescheduleMissed = {},
                    onExportWeeklyPdf = {},
                    onOpenAddTask = {},
                    onOpenWizard = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("my_plan_list")
            .performScrollToNode(hasTestTag("goal_card_1"))
        composeTestRule.onNodeWithText("Python learning").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scheduled (2h)").assertIsDisplayed()

        composeTestRule.onNodeWithTag("my_plan_list")
            .performScrollToNode(hasTestTag("goal_card_2"))
        composeTestRule.onNodeWithText("Coding practice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Partial (30m/1h)").assertIsDisplayed()

        composeTestRule.onNodeWithTag("my_plan_list")
            .performScrollToNode(hasTestTag("goal_card_3"))
        composeTestRule.onNodeWithText("DBMS preparation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unscheduled").assertIsDisplayed()
    }

    @Test
    fun add_and_edit_goal_dialog_creates_and_updates_goal_cleanly() {
        var savedTitle = ""
        var savedMinutes = 0
        var savedPriority = ""

        composeTestRule.setContent {
            StudyBloomTheme(themeMode = "LIGHT") {
                AddEditGoalDialog(
                    initialDate = "2026-11-12",
                    goalToEdit = null,
                    onDismiss = {},
                    onSaveGoal = { title, _, _, _, mins, _, prio, _, _ ->
                        savedTitle = title
                        savedMinutes = mins
                        savedPriority = prio
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("goal_title_input").performTextInput("Mathematics revision")
        composeTestRule.onNodeWithTag("goal_subject_input").performTextInput("Linear Algebra")
        composeTestRule.onNodeWithTag("goal_duration_120").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("goal_priority_MEDIUM").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("goal_confirm_button").performClick()

        assertEquals("Mathematics revision", savedTitle)
        assertEquals(120, savedMinutes)
        assertEquals("MEDIUM", savedPriority)
    }

    @Test
    fun delete_goal_confirmation_dialog_defaults_to_preserving_scheduled_tasks() {
        var deletedRemoveFlag: Boolean? = null
        val goal = StudyGoalEntity(
            id = 5,
            title = "Python learning",
            subject = "Python",
            estimatedMinutes = 120,
            preferredDate = "2026-11-12"
        )

        composeTestRule.setContent {
            StudyBloomTheme(themeMode = "LIGHT") {
                DeleteGoalConfirmationDialog(
                    goal = goal,
                    linkedUncompletedCount = 2,
                    onDismiss = {},
                    onConfirmDelete = { removeTasks ->
                        deletedRemoveFlag = removeTasks
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("delete_goal_confirm_button").performClick()
        assertFalse("By default, uncompleted tasks should be kept unlinked rather than deleted", deletedRemoveFlag!!)
    }

    @Test
    fun edit_goal_dialog_prefills_existing_goal_and_updates_title() {
        val existing = StudyGoalEntity(
            id = 9,
            title = "DBMS prep",
            subject = "DBMS",
            estimatedMinutes = 90,
            priority = "MEDIUM",
            preferredDate = "2026-11-12"
        )
        var updatedTitle = ""

        composeTestRule.setContent {
            StudyBloomTheme(themeMode = "LIGHT") {
                AddEditGoalDialog(
                    initialDate = "2026-11-12",
                    goalToEdit = existing,
                    onDismiss = {},
                    onSaveGoal = { title, _, _, _, _, _, _, _, _ ->
                        updatedTitle = title
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("goal_title_input").performTextReplacement("DBMS Normalization & Indexing")
        composeTestRule.onNodeWithTag("goal_confirm_button").performClick()
        assertEquals("DBMS Normalization & Indexing", updatedTitle)
        assertTrue(updatedTitle.contains("Indexing"))
    }
}
