package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiRepositoryImpl
import com.example.data.ai.MultiGoalScheduleRequest
import com.example.data.auth.AuthManager
import com.example.data.auth.MigrationStep
import com.example.data.local.StudyBloomDatabase
import com.example.data.local.entity.GoalOwnership
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.StudyBloomRepository
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncResult
import com.example.util.NetworkConnectivityMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class Stage5IntegrationAndSyncTest {

    private lateinit var context: Context
    private lateinit var database: StudyBloomDatabase
    private lateinit var aiRepository: AiRepositoryImpl
    private lateinit var repository: StudyBloomRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, StudyBloomDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.userProfileDao().insertProfile(
            UserProfileEntity(
                id = 1,
                isGuest = true,
                hasCompletedOnboarding = true,
                dailyGoalMinutes = 300
            )
        )
        aiRepository = AiRepositoryImpl()
        repository = StudyBloomRepository(database, aiRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun setConnectivity(online: Boolean) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadowCm = shadowOf(cm)
        val activeNetwork = cm.activeNetwork ?: cm.allNetworks.firstOrNull()
        val capabilities = ShadowNetworkCapabilities.newInstance()
        val shadowCaps = shadowOf(capabilities)
        shadowCaps.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        if (online) {
            shadowCaps.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        if (activeNetwork != null) {
            shadowCm.setNetworkCapabilities(activeNetwork, capabilities)
        }
    }

    @Test
    fun offline_goal_creation_multi_goal_scheduling_progress_guest_migration_and_sync_preserve_ownership() = runBlocking {
        val today = "2026-11-12"

        // 1. Seed a goal belonging to a different registered account to verify strict owner isolation
        database.studyGoalDao().insertGoal(
            StudyGoalEntity(
                ownerId = "user:other@studybloom.app",
                title = "Other Account Goal",
                subject = "Rust",
                estimatedMinutes = 90,
                preferredDate = today,
                isSynced = false
            )
        )

        // 2. Work Offline as Guest: create multiple goals on the same day
        setConnectivity(false)
        val offlineMonitor = NetworkConnectivityMonitor(context)
        val syncManager = SyncManager(context, repository, aiRepository, offlineMonitor)
        val authManager = AuthManager(context, repository)

        val g1Id = repository.createGoal("Python learning", subject = "Python", category = "PROGRAMMING", estimatedMinutes = 120, difficulty = "HARD", priority = "HIGH", preferredDate = today)
        val g2Id = repository.createGoal("Coding practice", subject = "DSA", category = "CODING_PRACTICE", estimatedMinutes = 60, difficulty = "MEDIUM", priority = "HIGH", preferredDate = today)
        val g3Id = repository.createGoal("DBMS prep", subject = "DBMS", category = "UNIVERSITY", estimatedMinutes = 60, difficulty = "MEDIUM", priority = "MEDIUM", preferredDate = today)

        val guestGoals = repository.allGoals.first()
        assertEquals(3, guestGoals.size)
        assertTrue(guestGoals.all { it.ownerId == GoalOwnership.GUEST_OWNER_ID && !it.isSynced })

        // Offline sync returns SyncResult.Offline and keeps all guest goals safely intact
        assertTrue(syncManager.performSync() is SyncResult.Offline)

        // 3. Schedule all 3 goals together cleanly
        val scheduleResult = repository.generateMultiGoalSchedule(
            MultiGoalScheduleRequest(
                goals = guestGoals,
                startDate = today,
                targetEndDate = today,
                dailyGoalMinutes = 300,
                sessionDurationMinutes = 60,
                breakDurationMinutes = 15
            )
        ).getOrThrow()
        assertEquals(0, scheduleResult.overflowMinutes)
        assertEquals(240, scheduleResult.tasks.sumOf { it.durationMinutes })
        repository.insertTasks(scheduleResult.tasks)

        // 4. Complete one scheduled task for Python learning via task clock
        val pythonTask = repository.getTasksForDateOnce(today).first { it.goalId == g1Id }
        val logged = repository.recordCompletedTaskSession(
            taskId = pythonTask.id,
            plannedDurationMinutes = pythonTask.durationMinutes,
            actualDurationMinutes = pythonTask.durationMinutes,
            overtimeMinutes = 0,
            pausedMinutes = 0,
            startTimestamp = 1700020000000L,
            endTimestamp = 1700023600000L,
            markTaskAsCompleted = true
        )
        assertTrue(logged)
        assertEquals(pythonTask.durationMinutes, repository.getGoalByIdForCurrentOwnerOnce(g1Id)!!.actualMinutes)
        assertEquals(0, repository.getGoalByIdForCurrentOwnerOnce(g2Id)!!.actualMinutes)

        // 5. Reconnect online and migrate Guest -> Registered Account
        setConnectivity(true)
        val migrationRes = authManager.createAccountAndMigrate(
            name = "Learner",
            email = "Learner@StudyBloom.app",
            password = "password123",
            isNetworkConnected = true
        )
        assertTrue(migrationRes.isSuccess)
        assertEquals(MigrationStep.COMPLETED, authManager.migrationState.value.step)
        assertEquals(3, authManager.migrationState.value.dataCounts?.goalsCount)

        // Verify all 3 goals now belong to user:learner@studybloom.app and keep their linked progress
        val migratedGoals = repository.allGoals.first()
        assertEquals(3, migratedGoals.size)
        assertTrue(migratedGoals.all { it.ownerId == "user:learner@studybloom.app" && it.isSynced })
        assertEquals(pythonTask.durationMinutes, migratedGoals.first { it.id == g1Id }.actualMinutes)

        // 6. Add a new goal offline as the registered user, then sync online -> preserves ownerId & keeps Other Account untouched
        val g4Id = repository.createGoal("Math revision", subject = "Math", category = "REVISION", estimatedMinutes = 60, priority = "LOW", preferredDate = today)
        assertFalse(repository.getGoalByIdForCurrentOwnerOnce(g4Id)!!.isSynced)

        val syncRes = syncManager.performSync()
        assertTrue(
            "Expected SyncResult.Success but got $syncRes (netState=${offlineMonitor.networkState.value})",
            syncRes is SyncResult.Success
        )
        val syncedNewGoal = repository.getGoalByIdForCurrentOwnerOnce(g4Id)
        assertNotNull(syncedNewGoal)
        assertTrue(syncedNewGoal!!.isSynced)
        assertEquals("user:learner@studybloom.app", syncedNewGoal.ownerId)

        // Other account's unsynced goal MUST NOT be touched by Learner's sync
        val otherAccountGoals = database.studyGoalDao().getAllGoalsForOwnerOnce("user:other@studybloom.app")
        assertEquals(1, otherAccountGoals.size)
        assertFalse(otherAccountGoals.first().isSynced)
        assertEquals("user:other@studybloom.app", otherAccountGoals.first().ownerId)

        // Signed-out Guest view shows 0 goals (since guest goals migrated cleanly to Learner)
        authManager.signOut()
        assertEquals(0, repository.allGoals.first().size)
    }

    @Test
    fun fresh_database_contains_no_demo_goals_tasks_sessions_or_materials() = runBlocking {
        assertEquals(0, repository.allGoals.first().size)
        assertEquals(0, repository.allTasks.first().size)
        assertEquals(0, repository.allSessions.first().size)
        assertEquals(0, repository.allMaterials.first().size)
    }
}
