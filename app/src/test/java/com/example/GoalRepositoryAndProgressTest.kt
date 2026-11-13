package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiRepositoryImpl
import com.example.data.local.StudyBloomDatabase
import com.example.data.local.entity.GoalOwnership
import com.example.data.local.entity.StudyTaskEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.StudyBloomRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class GoalRepositoryAndProgressTest {

    private lateinit var database: StudyBloomDatabase
    private lateinit var repository: StudyBloomRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StudyBloomDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.userProfileDao().insertProfile(
            UserProfileEntity(
                id = 1,
                isGuest = true,
                hasCompletedOnboarding = true,
                dailyGoalMinutes = 240
            )
        )
        repository = StudyBloomRepository(database, AiRepositoryImpl())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun supports_unlimited_goals_per_day_and_enforces_user_guest_isolation_and_migration() = runBlocking {
        val date = "2026-11-12"

        // Guest adds 4 distinct goals for the same day
        repository.createGoal("Python learning", subject = "Python", estimatedMinutes = 120, priority = "HIGH", preferredDate = date)
        repository.createGoal("Coding practice", subject = "Algorithms", estimatedMinutes = 60, priority = "HIGH", preferredDate = date)
        repository.createGoal("DBMS preparation", subject = "DBMS", estimatedMinutes = 120, priority = "MEDIUM", preferredDate = date)
        repository.createGoal("Mathematics revision", subject = "Math", estimatedMinutes = 60, priority = "MEDIUM", preferredDate = date)

        val guestGoals = repository.getGoalsForDateOnce(date)
        assertEquals(4, guestGoals.size)
        assertTrue(guestGoals.all { it.ownerId == GoalOwnership.GUEST_OWNER_ID })

        // Switch to an already-existing registered user A -> guest goals must stay isolated
        val guestProfile = repository.getProfileOnce()
        repository.updateProfile(
            guestProfile.copy(
                isGuest = false,
                accountEmail = "alice@studybloom.app",
                accountName = "Alice"
            )
        )
        assertEquals(0, repository.getGoalsForDateOnce(date).size)

        repository.createGoal("Alice Private Goal", subject = "AI", estimatedMinutes = 90, priority = "HIGH", preferredDate = date)
        assertEquals(1, repository.getGoalsForDateOnce(date).size)
        assertEquals("user:alice@studybloom.app", repository.getGoalsForDateOnce(date)[0].ownerId)

        // Switch back to Guest -> sees only the 4 guest goals, never Alice's
        repository.updateProfile(guestProfile.copy(isGuest = true, accountEmail = ""))
        assertEquals(4, repository.getGoalsForDateOnce(date).size)

        // Guest creates account -> all 4 guest goals migrate cleanly and keep Alice isolated
        val migratedCount = repository.migrateGuestGoalsToAccount("Bob@StudyBloom.app")
        assertEquals(4, migratedCount)

        repository.updateProfile(
            guestProfile.copy(
                isGuest = false,
                accountEmail = "Bob@StudyBloom.app",
                accountName = "Bob"
            )
        )
        val bobGoals = repository.getGoalsForDateOnce(date)
        assertEquals(4, bobGoals.size)
        assertTrue(bobGoals.all { it.ownerId == "user:bob@studybloom.app" && it.isSynced })
        assertEquals(4, repository.getLocalDataSummary().goalsCount)
    }

    @Test
    fun transactional_timer_and_completion_tracking_prevents_double_counting_and_updates_correct_goal() = runBlocking {
        val date = "2026-11-12"
        val goal1Id = repository.createGoal("Python learning", subject = "Python", estimatedMinutes = 120, priority = "HIGH", preferredDate = date)
        val goal2Id = repository.createGoal("DBMS prep", subject = "DBMS", estimatedMinutes = 120, priority = "MEDIUM", preferredDate = date)

        val task1Id = repository.insertTask(
            StudyTaskEntity(
                title = "Python Decorators",
                subject = "Python",
                date = date,
                startTime = "09:00",
                endTime = "09:45",
                durationMinutes = 45,
                goalId = goal1Id
            )
        )
        val task2Id = repository.insertTask(
            StudyTaskEntity(
                title = "DBMS Indexing",
                subject = "DBMS",
                date = date,
                startTime = "10:00",
                endTime = "10:45",
                durationMinutes = 45,
                goalId = goal2Id
            )
        )

        // Submit timer session for task 1
        val key = "TASK_CLOCK:$task1Id:1700000000000"
        val firstSubmit = repository.recordCompletedTaskSession(
            taskId = task1Id,
            plannedDurationMinutes = 45,
            actualDurationMinutes = 45,
            overtimeMinutes = 0,
            pausedMinutes = 0,
            startTimestamp = 1700000000000L,
            endTimestamp = 1700002700000L,
            markTaskAsCompleted = true,
            submissionKey = key
        )
        assertTrue(firstSubmit)

        // Duplicate submission with same key MUST be rejected
        val duplicateSubmit = repository.recordCompletedTaskSession(
            taskId = task1Id,
            plannedDurationMinutes = 45,
            actualDurationMinutes = 45,
            overtimeMinutes = 0,
            pausedMinutes = 0,
            startTimestamp = 1700000000000L,
            endTimestamp = 1700002700000L,
            markTaskAsCompleted = true,
            submissionKey = key
        )
        assertFalse(duplicateSubmit)

        // Toggling task1 uncompleted/completed after already timed MUST NOT double count
        repository.setTaskCompleted(task1Id, false)
        repository.setTaskCompleted(task1Id, true)

        val goal1 = repository.getGoalByIdForCurrentOwnerOnce(goal1Id)!!
        val goal2 = repository.getGoalByIdForCurrentOwnerOnce(goal2Id)!!
        assertEquals(45, goal1.actualMinutes)
        assertEquals(0, goal2.actualMinutes)
        assertEquals(1, database.studySessionDao().getSessionsCount())

        // Pomodoro completion on task2 updates goal2 exactly once
        val pomoId = repository.recordFocusSession(
            taskId = task2Id,
            taskTitle = "DBMS Indexing",
            subject = "DBMS",
            durationMinutes = 30,
            markTaskCompleted = true,
            submissionKey = "POMODORO:$task2Id:1"
        )
        assertTrue(pomoId > 0)
        assertEquals(30, repository.getGoalByIdForCurrentOwnerOnce(goal2Id)!!.actualMinutes)
        assertEquals(2, database.studySessionDao().getSessionsCount())
    }

    @Test
    fun editing_and_deleting_goal_preserves_other_goals_unrelated_tasks_and_timer_history() = runBlocking {
        val date = "2026-11-12"
        val goalAId = repository.createGoal("Goal A", subject = "Python", estimatedMinutes = 120, priority = "HIGH", preferredDate = date)
        val goalBId = repository.createGoal("Goal B", subject = "Math", estimatedMinutes = 60, priority = "LOW", preferredDate = date)

        val goalAUncompletedTask = repository.insertTask(
            StudyTaskEntity(title = "A Pending", subject = "Python", date = date, startTime = "09:00", endTime = "10:00", durationMinutes = 60, goalId = goalAId)
        )
        val goalACompletedTask = repository.insertTask(
            StudyTaskEntity(title = "A Done", subject = "Python", date = date, startTime = "10:15", endTime = "11:00", durationMinutes = 45, goalId = goalAId)
        )
        val standaloneTask = repository.insertTask(
            StudyTaskEntity(title = "Standalone Task", subject = "General", date = date, startTime = "14:00", endTime = "14:30", durationMinutes = 30, goalId = null)
        )

        // Record timer session on A Done
        repository.recordCompletedTaskSession(
            taskId = goalACompletedTask,
            plannedDurationMinutes = 45,
            actualDurationMinutes = 45,
            overtimeMinutes = 0,
            pausedMinutes = 0,
            startTimestamp = 1700010000000L,
            endTimestamp = 1700012700000L,
            markTaskAsCompleted = true
        )

        // Edit Goal A -> Goal B stays identical
        val goalABefore = repository.getGoalByIdForCurrentOwnerOnce(goalAId)!!
        val goalBBefore = repository.getGoalByIdForCurrentOwnerOnce(goalBId)!!
        assertTrue(repository.updateGoal(goalABefore.copy(title = "Goal A Updated", estimatedMinutes = 150)))
        val goalBAfter = repository.getGoalByIdForCurrentOwnerOnce(goalBId)!!
        assertEquals(goalBBefore.title, goalBAfter.title)
        assertEquals(goalBBefore.estimatedMinutes, goalBAfter.estimatedMinutes)

        // Delete Goal A keeping scheduled tasks -> unlinks tasks (goalId = NULL) and preserves timer history & standalone task
        assertTrue(repository.deleteGoal(goalAId, removeUncompletedTasks = false))
        assertNull(repository.getGoalByIdForCurrentOwnerOnce(goalAId))
        assertNotNull(repository.getGoalByIdForCurrentOwnerOnce(goalBId))

        val pendingAfter = database.studyTaskDao().getTaskByIdOnce(goalAUncompletedTask)
        assertNotNull("Scheduled uncompleted task should be preserved when unlinked", pendingAfter)
        assertNull("Unlinked task must have goalId = NULL", pendingAfter!!.goalId)

        val doneAfter = database.studyTaskDao().getTaskByIdOnce(goalACompletedTask)
        assertNotNull(doneAfter)
        assertNull(doneAfter!!.goalId)

        assertNotNull(database.studyTaskDao().getTaskByIdOnce(standaloneTask))
        assertEquals(1, database.studySessionDao().getSessionsCount())
        assertEquals(1, repository.allGoals.first().size)
    }
}
