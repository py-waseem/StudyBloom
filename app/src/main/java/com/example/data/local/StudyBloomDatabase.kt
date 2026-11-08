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
    version = 3,
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
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(db: StudyBloomDatabase) {
                val profileDao = db.userProfileDao()
                val slotsDao = db.availabilitySlotDao()
                val materialDao = db.learningMaterialDao()
                val topicDao = db.topicDao()
                val taskDao = db.studyTaskDao()

                // Default Profile
                profileDao.insertProfile(
                    UserProfileEntity(
                        id = 1,
                        name = "Alex",
                        nickname = "Alex",
                        purpose = "Programming and software development",
                        wakeTime = "07:00",
                        sleepTime = "23:00",
                        preferredStudyHours = "Evenings",
                        preferredSessionDurationMinutes = 45,
                        preferredBreakDurationMinutes = 15,
                        dailyGoalMinutes = 180,
                        timeZone = "Asia/Kathmandu",
                        hasCompletedOnboarding = true,
                        currentStreakDays = 3,
                        lastActiveDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    )
                )

                // Default Availability Slots (Monday to Sunday)
                val defaultSlots = mutableListOf<AvailabilitySlotEntity>()
                for (day in 1..5) {
                    defaultSlots.add(AvailabilitySlotEntity(dayOfWeek = day, startTime = "06:30", endTime = "08:00"))
                    defaultSlots.add(AvailabilitySlotEntity(dayOfWeek = day, startTime = "19:00", endTime = "22:00"))
                }
                for (day in 6..7) {
                    defaultSlots.add(AvailabilitySlotEntity(dayOfWeek = day, startTime = "09:00", endTime = "12:00"))
                    defaultSlots.add(AvailabilitySlotEntity(dayOfWeek = day, startTime = "15:00", endTime = "18:00"))
                }
                slotsDao.insertSlots(defaultSlots)

                // Initial sample starter material: Django & Python Mastery
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val matId = materialDao.insertMaterial(
                    LearningMaterialEntity(
                        title = "Django & Python Web Development",
                        fileType = "PDF",
                        uploadDate = today,
                        fileSizeFormatted = "4.2 MB",
                        extractedText = "Django Web Framework guide covering models, ORM, views, and templates.",
                        summary = "Comprehensive technical textbook on building modern backend APIs and database-driven web services.",
                        mainSubject = "Django Development",
                        estimatedDifficulty = "MEDIUM",
                        estimatedTotalHours = 14.5,
                        status = "READY"
                    )
                )

                val initialTopics = listOf(
                    TopicEntity(materialId = matId, title = "1. Introduction to Django Architecture & MVT", subtopicsRaw = "HTTP flow, urls.py, settings configuration", difficulty = "EASY", estimatedMinutes = 45, isPrerequisite = true, orderIndex = 1, isCompleted = true),
                    TopicEntity(materialId = matId, title = "2. Models & ORM QuerySets", subtopicsRaw = "Field types, migrations, filtering, foreign keys", difficulty = "HARD", estimatedMinutes = 90, isPrerequisite = true, orderIndex = 2, isCompleted = false),
                    TopicEntity(materialId = matId, title = "3. Class-Based Views & Templates", subtopicsRaw = "ListView, DetailView, context dictionaries", difficulty = "MEDIUM", estimatedMinutes = 60, isPrerequisite = false, orderIndex = 3, isCompleted = false),
                    TopicEntity(materialId = matId, title = "4. Forms & User Authentication", subtopicsRaw = "ModelForms, CSRF, LoginView, session management", difficulty = "MEDIUM", estimatedMinutes = 75, isPrerequisite = false, orderIndex = 4, isCompleted = false),
                    TopicEntity(materialId = matId, title = "5. Django REST Framework & Serializers", subtopicsRaw = "ModelSerializers, ViewSets, API routers", difficulty = "HARD", estimatedMinutes = 90, isPrerequisite = false, orderIndex = 5, isCompleted = false)
                )
                topicDao.insertTopics(initialTopics)

                // Today's starter schedule
                val initialTasks = listOf(
                    StudyTaskEntity(
                        title = "Python Functions & Lambda Expressions",
                        description = "Review functional programming concepts, closures, and type hints",
                        category = "PROGRAMMING",
                        subject = "Python Core",
                        priority = "HIGH",
                        date = today,
                        startTime = "07:00",
                        endTime = "07:45",
                        durationMinutes = 45,
                        isCompleted = true,
                        taskType = "STUDY",
                        learningObjective = "Master higher-order functions and clean lambda usage",
                        estimatedDifficulty = "MEDIUM"
                    ),
                    StudyTaskEntity(
                        title = "Django Models & QuerySet Filtering",
                        description = "Practice complex lookups with Q objects and annotate() aggregations",
                        category = "CODING_PRACTICE",
                        subject = "Django Development",
                        priority = "HIGH",
                        date = today,
                        startTime = "19:00",
                        endTime = "20:00",
                        durationMinutes = 60,
                        isCompleted = false,
                        taskType = "CODING",
                        learningObjective = "Build optimized ORM queries without N+1 query traps",
                        estimatedDifficulty = "HARD",
                        materialId = matId,
                        topicId = 2
                    ),
                    StudyTaskEntity(
                        title = "Quick Revision: REST API Status Codes",
                        description = "Review 200 vs 201, 400 vs 422, and idempotency",
                        category = "REVISION",
                        subject = "Web Architecture",
                        priority = "LOW",
                        date = today,
                        startTime = "20:15",
                        endTime = "20:45",
                        durationMinutes = 30,
                        isCompleted = false,
                        taskType = "REVISION",
                        learningObjective = "Memorize HTTP status best practices",
                        estimatedDifficulty = "EASY"
                    )
                )
                taskDao.insertTasks(initialTasks)

                // Starter completed study session for today
                db.studySessionDao().insertSession(
                    StudySessionEntity(
                        taskTitle = "Python Functions & Lambda Expressions",
                        subject = "Python Core",
                        durationMinutes = 45,
                        date = today,
                        completedSuccessfully = true,
                        sessionType = "POMODORO"
                    )
                )
            }
        }
    }
}
