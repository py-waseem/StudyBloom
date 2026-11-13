package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.StudyBloomDatabase
import com.example.data.local.entity.GoalOwnership
import com.example.data.local.entity.StudyGoalEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class DatabaseMigrationTest {

    private lateinit var context: Context
    private val dbFilesToClean = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        dbFilesToClean.forEach { file ->
            file.delete()
            File(file.path + "-wal").delete()
            File(file.path + "-shm").delete()
            File(file.path + "-journal").delete()
        }
        dbFilesToClean.clear()
    }

    private fun createVersion5Database(dbName: String): File {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()
        dbFilesToClean.add(dbFile)

        val sqliteDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqliteDb.use { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_profile` (
                    `id` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `nickname` TEXT NOT NULL,
                    `purpose` TEXT NOT NULL,
                    `wakeTime` TEXT NOT NULL,
                    `sleepTime` TEXT NOT NULL,
                    `preferredStudyHours` TEXT NOT NULL,
                    `preferredSessionDurationMinutes` INTEGER NOT NULL,
                    `preferredBreakDurationMinutes` INTEGER NOT NULL,
                    `dailyGoalMinutes` INTEGER NOT NULL,
                    `reminderEnabled` INTEGER NOT NULL,
                    `reminderBeforeMinutes` INTEGER NOT NULL,
                    `dailyPlanningReminderEnabled` INTEGER NOT NULL,
                    `dailyPlanningReminderTime` TEXT NOT NULL,
                    `eveningSummaryEnabled` INTEGER NOT NULL,
                    `eveningSummaryTime` TEXT NOT NULL,
                    `timeZone` TEXT NOT NULL,
                    `themeMode` TEXT NOT NULL,
                    `hasCompletedOnboarding` INTEGER NOT NULL,
                    `currentStreakDays` INTEGER NOT NULL,
                    `lastActiveDate` TEXT NOT NULL,
                    `isGuest` INTEGER NOT NULL,
                    `accountEmail` TEXT NOT NULL,
                    `accountName` TEXT NOT NULL,
                    `isEmailVerified` INTEGER NOT NULL,
                    `isCloudBackupEnabled` INTEGER NOT NULL,
                    `lastCloudSyncTimestamp` INTEGER NOT NULL,
                    `hasSeenAccountReminder` INTEGER NOT NULL,
                    `completedSessionsCount` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `user_profile` VALUES (
                    1, 'Maya', 'Maya', 'Final Year CS Prep', '07:00', '23:00', 'Evenings',
                    45, 15, 180, 1, 15, 1, '08:00', 1, '21:30', 'UTC', 'LIGHT',
                    1, 4, '2026-11-11', 1, '', '', 0, 0, 0, 0, 2
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `learning_materials` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `fileType` TEXT NOT NULL,
                    `uriString` TEXT NOT NULL,
                    `uploadDate` TEXT NOT NULL,
                    `fileSizeFormatted` TEXT NOT NULL,
                    `extractedText` TEXT NOT NULL,
                    `summary` TEXT NOT NULL,
                    `mainSubject` TEXT NOT NULL,
                    `estimatedDifficulty` TEXT NOT NULL,
                    `estimatedTotalHours` REAL NOT NULL,
                    `status` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `learning_materials` VALUES (
                    10, 'DBMS Notes.pdf', 'PDF', 'content://dbms', '2026-11-10', '1.8 MB',
                    'Normalization & B-Trees', 'Database indexing and ACID properties',
                    'DBMS', 'MEDIUM', 6.5, 'READY'
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `material_topics` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `materialId` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `subtopicsRaw` TEXT NOT NULL,
                    `difficulty` TEXT NOT NULL,
                    `estimatedMinutes` INTEGER NOT NULL,
                    `isPrerequisite` INTEGER NOT NULL,
                    `orderIndex` INTEGER NOT NULL,
                    `isCompleted` INTEGER NOT NULL,
                    FOREIGN KEY(`materialId`) REFERENCES `learning_materials`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_material_topics_materialId` ON `material_topics` (`materialId`)")
            db.execSQL(
                """
                INSERT INTO `material_topics` VALUES (
                    101, 10, 'B+ Tree Indexing', 'Leaf nodes, splitting', 'HARD', 60, 1, 1, 0
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `study_tasks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `subject` TEXT NOT NULL,
                    `priority` TEXT NOT NULL,
                    `date` TEXT NOT NULL,
                    `startTime` TEXT NOT NULL,
                    `endTime` TEXT NOT NULL,
                    `durationMinutes` INTEGER NOT NULL,
                    `actualDurationMinutes` INTEGER NOT NULL,
                    `overtimeMinutes` INTEGER NOT NULL,
                    `isCompleted` INTEGER NOT NULL,
                    `isSkipped` INTEGER NOT NULL,
                    `notes` TEXT NOT NULL,
                    `materialId` INTEGER,
                    `topicId` INTEGER,
                    `taskType` TEXT NOT NULL,
                    `learningObjective` TEXT NOT NULL,
                    `estimatedDifficulty` TEXT NOT NULL,
                    `isAIGenerated` INTEGER NOT NULL,
                    `startedAtTimestamp` INTEGER,
                    `completedAtTimestamp` INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `study_tasks` VALUES (
                    501, 'Practice SQL Joins', 'Inner, Left, Lateral joins', 'UNIVERSITY', 'DBMS', 'HIGH',
                    '2026-11-12', '18:00', '19:00', 60, 60, 0, 1, 0, 'Completed cleanly',
                    10, 101, 'STUDY', 'Master join execution plans', 'MEDIUM', 0, 1700000000000, 1700003600000
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `study_tasks` VALUES (
                    502, 'Solve 2 Graph Problems', 'BFS and Dijkstra', 'CODING_PRACTICE', 'Algorithms', 'HIGH',
                    '2026-11-12', '19:30', '20:30', 60, 0, 0, 0, 0, '',
                    NULL, NULL, 'CODING', 'Shortest path mastery', 'HARD', 0, NULL, NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `study_sessions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `taskId` INTEGER,
                    `taskTitle` TEXT NOT NULL,
                    `subject` TEXT NOT NULL,
                    `durationMinutes` INTEGER NOT NULL,
                    `plannedDurationMinutes` INTEGER NOT NULL,
                    `overtimeMinutes` INTEGER NOT NULL,
                    `pausedMinutes` INTEGER NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `date` TEXT NOT NULL,
                    `completedSuccessfully` INTEGER NOT NULL,
                    `sessionType` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `study_sessions` VALUES (
                    901, 501, 'Practice SQL Joins', 'DBMS', 60, 60, 0, 0, 1700003600000, '2026-11-12', 1, 'TASK_CLOCK'
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `availability_slots` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `dayOfWeek` INTEGER NOT NULL,
                    `startTime` TEXT NOT NULL,
                    `endTime` TEXT NOT NULL,
                    `isEnabled` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO `availability_slots` VALUES (1, 4, '18:00', '21:30', 1)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pending_ai_requests` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `requestType` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `prompt` TEXT NOT NULL,
                    `relatedEntityId` INTEGER,
                    `createdAtTimestamp` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `responsePreview` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `pending_ai_requests` VALUES (
                    301, 'DOCUMENT_ANALYSIS', 'DBMS Notes.pdf', 'Analyze chapters', 10, 1700001000000, 'QUEUED', 'Pending offline'
                )
                """.trimIndent()
            )

            db.version = 5
        }
        return dbFile
    }

    private fun createVersion1Database(dbName: String): File {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()
        dbFilesToClean.add(dbFile)

        val sqliteDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqliteDb.use { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_profile` (
                    `id` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `nickname` TEXT NOT NULL,
                    `purpose` TEXT NOT NULL,
                    `wakeTime` TEXT NOT NULL,
                    `sleepTime` TEXT NOT NULL,
                    `preferredStudyHours` TEXT NOT NULL,
                    `preferredSessionDurationMinutes` INTEGER NOT NULL,
                    `preferredBreakDurationMinutes` INTEGER NOT NULL,
                    `dailyGoalMinutes` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "INSERT INTO `user_profile` VALUES (1, 'Aarav', 'Aarav', 'GATE CS', '06:30', '22:30', 'Mornings', 45, 15, 120)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `study_tasks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `subject` TEXT NOT NULL,
                    `priority` TEXT NOT NULL,
                    `date` TEXT NOT NULL,
                    `startTime` TEXT NOT NULL,
                    `endTime` TEXT NOT NULL,
                    `durationMinutes` INTEGER NOT NULL,
                    `isCompleted` INTEGER NOT NULL,
                    `isSkipped` INTEGER NOT NULL,
                    `notes` TEXT NOT NULL,
                    `materialId` INTEGER,
                    `topicId` INTEGER,
                    `taskType` TEXT NOT NULL,
                    `learningObjective` TEXT NOT NULL,
                    `estimatedDifficulty` TEXT NOT NULL,
                    `isAIGenerated` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `study_tasks` VALUES (
                    1, 'Legacy OS Scheduling Task', 'CPU schedulers', 'UNIVERSITY', 'OS', 'HIGH',
                    '2026-11-01', '08:00', '09:00', 60, 1, 0, '', NULL, NULL, 'STUDY', 'Understand CFS', 'MEDIUM', 0
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `study_sessions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `taskId` INTEGER,
                    `taskTitle` TEXT NOT NULL,
                    `subject` TEXT NOT NULL,
                    `durationMinutes` INTEGER NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `date` TEXT NOT NULL,
                    `completedSuccessfully` INTEGER NOT NULL,
                    `sessionType` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "INSERT INTO `study_sessions` VALUES (1, 1, 'Legacy OS Scheduling Task', 'OS', 60, 1699000000000, '2026-11-01', 1, 'POMODORO')"
            )

            db.version = 1
        }
        return dbFile
    }

    @Test
    fun migrate_v5_to_v6_preserves_all_existing_records_and_supports_multiple_isolated_goals() = runBlocking {
        val dbName = "migration_test_v5_to_v6.db"
        createVersion5Database(dbName)

        val migratedDb = Room.databaseBuilder(context, StudyBloomDatabase::class.java, dbName)
            .addMigrations(*StudyBloomDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            // 1. Verify profile survived intact
            val profile = migratedDb.userProfileDao().getUserProfileOnce()
            assertNotNull(profile)
            assertEquals("Maya", profile!!.nickname)
            assertEquals(180, profile.dailyGoalMinutes)
            assertEquals(4, profile.currentStreakDays)
            assertEquals(15, profile.reminderBeforeMinutes)

            // 2. Verify existing tasks survived with goalId == null
            val tasks = migratedDb.studyTaskDao().getAllTasksOnce()
            assertEquals(2, tasks.size)
            assertEquals("Practice SQL Joins", tasks[0].title)
            assertNull("Existing v5 task must have goalId = NULL", tasks[0].goalId)
            assertEquals(60, tasks[0].actualDurationMinutes)
            assertEquals("Solve 2 Graph Problems", tasks[1].title)
            assertNull("Existing v5 task must have goalId = NULL", tasks[1].goalId)

            // 3. Verify existing sessions survived with goalId == null & null submission key
            val sessions = migratedDb.studySessionDao().getAllSessionsOnce()
            assertEquals(1, sessions.size)
            assertEquals(901L, sessions[0].id)
            assertEquals(60, sessions[0].durationMinutes)
            assertNull(sessions[0].goalId)
            assertNull(sessions[0].sessionSubmissionKey)

            // 4. Verify materials, topics, availability slots, pending AI requests survived
            assertEquals(1, migratedDb.learningMaterialDao().getMaterialsCount())
            assertEquals(1, migratedDb.topicDao().getTopicsCount())
            assertEquals(1, migratedDb.availabilitySlotDao().getAllSlotsOnce().size)
            assertEquals(1, migratedDb.pendingAiRequestDao().getQueuedRequests().size)

            // 5. Verify multiple goals on same date and strict owner isolation
            val goalDao = migratedDb.studyGoalDao()
            val guestId1 = goalDao.insertGoal(
                StudyGoalEntity(
                    ownerId = GoalOwnership.GUEST_OWNER_ID,
                    title = "Python learning",
                    subject = "Python",
                    estimatedMinutes = 120,
                    priority = "HIGH",
                    preferredDate = "2026-11-12"
                )
            )
            val guestId2 = goalDao.insertGoal(
                StudyGoalEntity(
                    ownerId = GoalOwnership.GUEST_OWNER_ID,
                    title = "Coding practice",
                    subject = "Algorithms",
                    estimatedMinutes = 60,
                    priority = "HIGH",
                    preferredDate = "2026-11-12"
                )
            )
            val guestId3 = goalDao.insertGoal(
                StudyGoalEntity(
                    ownerId = GoalOwnership.GUEST_OWNER_ID,
                    title = "DBMS preparation",
                    subject = "DBMS",
                    estimatedMinutes = 120,
                    priority = "MEDIUM",
                    preferredDate = "2026-11-12"
                )
            )
            val guestId4 = goalDao.insertGoal(
                StudyGoalEntity(
                    ownerId = GoalOwnership.GUEST_OWNER_ID,
                    title = "Mathematics revision",
                    subject = "Mathematics",
                    estimatedMinutes = 60,
                    priority = "MEDIUM",
                    preferredDate = "2026-11-12"
                )
            )
            assertTrue(guestId1 > 0 && guestId2 > 0 && guestId3 > 0 && guestId4 > 0)

            // Registered user's goal must remain isolated from guest goals
            goalDao.insertGoal(
                StudyGoalEntity(
                    ownerId = "user:other@example.com",
                    title = "Private Registered Goal",
                    subject = "AI",
                    estimatedMinutes = 90,
                    priority = "HIGH",
                    preferredDate = "2026-11-12"
                )
            )

            val guestGoalsToday = goalDao.getGoalsForOwnerAndDateOnce(GoalOwnership.GUEST_OWNER_ID, "2026-11-12")
            assertEquals(4, guestGoalsToday.size)

            val otherUserGoalsToday = goalDao.getGoalsForOwnerAndDateOnce("user:other@example.com", "2026-11-12")
            assertEquals(1, otherUserGoalsToday.size)
            assertEquals("Private Registered Goal", otherUserGoalsToday[0].title)
        } finally {
            migratedDb.close()
        }
    }

    @Test
    fun migrate_v1_through_v6_preserves_legacy_records_without_destructive_reset() = runBlocking {
        val dbName = "migration_test_v1_to_v6.db"
        createVersion1Database(dbName)

        val migratedDb = Room.databaseBuilder(context, StudyBloomDatabase::class.java, dbName)
            .addMigrations(*StudyBloomDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            val profile = migratedDb.userProfileDao().getUserProfileOnce()
            assertNotNull(profile)
            assertEquals("Aarav", profile!!.name)
            assertEquals(120, profile.dailyGoalMinutes)

            val tasks = migratedDb.studyTaskDao().getAllTasksOnce()
            assertEquals(1, tasks.size)
            assertEquals("Legacy OS Scheduling Task", tasks[0].title)
            assertNull(tasks[0].goalId)

            val sessions = migratedDb.studySessionDao().getAllSessionsOnce()
            assertEquals(1, sessions.size)
            assertEquals(60, sessions[0].durationMinutes)
            assertNull(sessions[0].goalId)
        } finally {
            migratedDb.close()
        }
    }
}
