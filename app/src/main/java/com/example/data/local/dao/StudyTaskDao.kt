package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.StudyTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM study_tasks WHERE date = :date ORDER BY startTime ASC")
    fun getTasksForDate(date: String): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE date = :date ORDER BY startTime ASC")
    suspend fun getTasksForDateOnce(date: String): List<StudyTaskEntity>

    @Query("SELECT * FROM study_tasks WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getTasksBetweenDates(startDate: String, endDate: String): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    suspend fun getTasksBetweenDatesOnce(startDate: String, endDate: String): List<StudyTaskEntity>

    @Query("SELECT * FROM study_tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: Long): Flow<StudyTaskEntity?>

    @Query("SELECT * FROM study_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdOnce(id: Long): StudyTaskEntity?

    @Query("SELECT * FROM study_tasks WHERE isCompleted = 0 AND isSkipped = 0 ORDER BY date ASC, startTime ASC")
    fun getAllPendingTasks(): Flow<List<StudyTaskEntity>>

    @Query("SELECT COUNT(*) FROM study_tasks WHERE date = :date AND isCompleted = 1")
    fun getCompletedCountForDate(date: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<StudyTaskEntity>)

    @Update
    suspend fun updateTask(task: StudyTaskEntity)

    @Query("UPDATE study_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, completed: Boolean)

    @Query("UPDATE study_tasks SET isSkipped = :skipped WHERE id = :id")
    suspend fun setTaskSkipped(id: Long, skipped: Boolean)

    @Query("UPDATE study_tasks SET date = :newDate, startTime = :newStart, endTime = :newEnd WHERE id = :id")
    suspend fun rescheduleTask(id: Long, newDate: String, newStart: String, newEnd: String)

    @Query("DELETE FROM study_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM study_tasks WHERE date = :date")
    suspend fun deleteTasksForDate(date: String)

    @Query("SELECT COUNT(*) FROM study_tasks")
    suspend fun getTasksCount(): Int

    @Query("SELECT * FROM study_tasks")
    suspend fun getAllTasksOnce(): List<StudyTaskEntity>

    @Query("SELECT * FROM study_tasks ORDER BY date ASC, startTime ASC")
    fun getAllTasksFlow(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE goalId = :goalId ORDER BY date ASC, startTime ASC")
    suspend fun getTasksForGoalOnce(goalId: Long): List<StudyTaskEntity>

    @Query("UPDATE study_tasks SET goalId = NULL WHERE goalId = :goalId")
    suspend fun unlinkTasksFromGoal(goalId: Long)

    @Query("DELETE FROM study_tasks WHERE goalId = :goalId AND isCompleted = 0")
    suspend fun deleteUncompletedTasksForGoal(goalId: Long)

    @Query("UPDATE study_tasks SET date = :newDate WHERE goalId = :goalId AND isCompleted = 0")
    suspend fun rescheduleUncompletedTasksForGoal(goalId: Long, newDate: String)

    @Query(
        "DELETE FROM study_tasks WHERE actualDurationMinutes = 0 AND (" +
            "(title = 'Python Functions & Lambda Expressions' AND description = 'Review functional programming concepts, closures, and type hints') OR " +
            "(title = 'Django Models & QuerySet Filtering' AND description = 'Practice complex lookups with Q objects and annotate() aggregations') OR " +
            "(title = 'Quick Revision: REST API Status Codes' AND description = 'Review 200 vs 201, 400 vs 422, and idempotency')" +
            ")"
    )
    suspend fun deleteDemoStarterTasks()
}
