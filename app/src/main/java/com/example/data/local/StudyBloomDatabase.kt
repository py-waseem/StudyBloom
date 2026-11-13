package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AvailabilitySlotDao
import com.example.data.local.dao.LearningMaterialDao
import com.example.data.local.dao.PendingAiRequestDao
import com.example.data.local.dao.StudyGoalDao
import com.example.data.local.dao.StudySessionDao
import com.example.data.local.dao.StudyTaskDao
import com.example.data.local.dao.TopicDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.AvailabilitySlotEntity
import com.example.data.local.entity.LearningMaterialEntity
import com.example.data.local.entity.PendingAiRequestEntity
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        LearningMaterialEntity::class,
        TopicEntity::class,
        StudyTaskEntity::class,
        StudySessionEntity::class,
        AvailabilitySlotEntity::class,
        PendingAiRequestEntity::class,
        StudyGoalEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class StudyBloomDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun learningMaterialDao(): LearningMaterialDao
    abstract fun topicDao(): TopicDao
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun availabilitySlotDao(): AvailabilitySlotDao
    abstract fun pendingAiRequestDao(): PendingAiRequestDao
    abstract fun studyGoalDao(): StudyGoalDao

    companion object {
        @Volatile
        private var INSTANCE: StudyBloomDatabase? = null

        private fun hasColumn(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String
        ): Boolean {
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) {
                        return true
                    }
                }
            }
            return false
        }

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnDefinition: String
        ) {
            if (!hasColumn(db, tableName, columnName)) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
            }
        }

        private fun ensureV5BaselineSchema(db: SupportSQLiteDatabase) {
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
            addColumnIfMissing(db, "user_profile", "isGuest", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "user_profile", "accountEmail", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "user_profile", "accountName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "user_profile", "isEmailVerified", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "user_profile", "isCloudBackupEnabled", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "user_profile", "lastCloudSyncTimestamp", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "user_profile", "hasSeenAccountReminder", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "user_profile", "completedSessionsCount", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "user_profile", "reminderEnabled", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "user_profile", "reminderBeforeMinutes", "INTEGER NOT NULL DEFAULT 10")
            addColumnIfMissing(db, "user_profile", "dailyPlanningReminderEnabled", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "user_profile", "dailyPlanningReminderTime", "TEXT NOT NULL DEFAULT '08:00'")
            addColumnIfMissing(db, "user_profile", "eveningSummaryEnabled", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "user_profile", "eveningSummaryTime", "TEXT NOT NULL DEFAULT '21:30'")
            addColumnIfMissing(db, "user_profile", "timeZone", "TEXT NOT NULL DEFAULT 'UTC'")
            addColumnIfMissing(db, "user_profile", "themeMode", "TEXT NOT NULL DEFAULT 'SYSTEM'")
            addColumnIfMissing(db, "user_profile", "hasCompletedOnboarding", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "user_profile", "currentStreakDays", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "user_profile", "lastActiveDate", "TEXT NOT NULL DEFAULT ''")

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
            addColumnIfMissing(db, "study_tasks", "actualDurationMinutes", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "study_tasks", "overtimeMinutes", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "study_tasks", "startedAtTimestamp", "INTEGER DEFAULT NULL")
            addColumnIfMissing(db, "study_tasks", "completedAtTimestamp", "INTEGER DEFAULT NULL")

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
            addColumnIfMissing(db, "study_sessions", "plannedDurationMinutes", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "study_sessions", "overtimeMinutes", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "study_sessions", "pausedMinutes", "INTEGER NOT NULL DEFAULT 0")

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
        }

        private fun upgradeV5ToV6(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `study_goals` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `ownerId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `subject` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `estimatedMinutes` INTEGER NOT NULL,
                    `actualMinutes` INTEGER NOT NULL,
                    `difficulty` TEXT NOT NULL,
                    `priority` TEXT NOT NULL,
                    `preferredDate` TEXT NOT NULL,
                    `deadlineDate` TEXT NOT NULL,
                    `isCompleted` INTEGER NOT NULL,
                    `isSynced` INTEGER NOT NULL,
                    `createdAtTimestamp` INTEGER NOT NULL,
                    `updatedAtTimestamp` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_goals_ownerId` ON `study_goals` (`ownerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_goals_preferredDate` ON `study_goals` (`preferredDate`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_goals_ownerId_preferredDate` ON `study_goals` (`ownerId`, `preferredDate`)")

            addColumnIfMissing(db, "study_tasks", "goalId", "INTEGER DEFAULT NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_tasks_goalId` ON `study_tasks` (`goalId`)")

            addColumnIfMissing(db, "study_sessions", "goalId", "INTEGER DEFAULT NULL")
            addColumnIfMissing(db, "study_sessions", "sessionSubmissionKey", "TEXT DEFAULT NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_goalId` ON `study_sessions` (`goalId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_study_sessions_sessionSubmissionKey` ON `study_sessions` (`sessionSubmissionKey`)")
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureV5BaselineSchema(db)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureV5BaselineSchema(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureV5BaselineSchema(db)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureV5BaselineSchema(db)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                upgradeV5ToV6(db)
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6
        )

        fun getInstance(context: Context): StudyBloomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyBloomDatabase::class.java,
                    "studybloom_database.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateCleanInitialState(database)
                    }
                }
            }

            private suspend fun populateCleanInitialState(db: StudyBloomDatabase) {
                val profileDao = db.userProfileDao()

                // Clean empty profile with zero demo/sample data
                profileDao.insertProfile(
                    UserProfileEntity(
                        id = 1,
                        name = "",
                        nickname = "",
                        purpose = "",
                        wakeTime = "07:00",
                        sleepTime = "23:00",
                        preferredStudyHours = "Evenings",
                        preferredSessionDurationMinutes = 45,
                        preferredBreakDurationMinutes = 15,
                        dailyGoalMinutes = 120,
                        timeZone = "UTC",
                        hasCompletedOnboarding = false,
                        currentStreakDays = 0,
                        lastActiveDate = "",
                        isGuest = true,
                        accountEmail = "",
                        accountName = "",
                        isEmailVerified = false,
                        isCloudBackupEnabled = false,
                        lastCloudSyncTimestamp = 0L,
                        hasSeenAccountReminder = false,
                        completedSessionsCount = 0
                    )
                )
                // No demo materials, topics, tasks, or study sessions are inserted.
                // The app starts in a completely clean, pristine state.
            }
        }
    }
}
