package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AvailabilitySlotDao
import com.example.data.local.dao.LearningMaterialDao
import com.example.data.local.dao.PendingAiRequestDao
import com.example.data.local.dao.StudySessionDao
import com.example.data.local.dao.StudyTaskDao
import com.example.data.local.dao.TopicDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.AvailabilitySlotEntity
import com.example.data.local.entity.LearningMaterialEntity
import com.example.data.local.entity.PendingAiRequestEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.StudyTaskEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        UserProfileEntity::class,
        LearningMaterialEntity::class,
        TopicEntity::class,
        StudyTaskEntity::class,
        StudySessionEntity::class,
        AvailabilitySlotEntity::class,
        PendingAiRequestEntity::class
    ],
    version = 5,
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

    companion object {
        @Volatile
        private var INSTANCE: StudyBloomDatabase? = null

        fun getInstance(context: Context): StudyBloomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyBloomDatabase::class.java,
                    "studybloom_database.db"
                )
                    .fallbackToDestructiveMigration()
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
