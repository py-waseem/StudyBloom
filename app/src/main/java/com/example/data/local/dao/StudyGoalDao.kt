package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.StudyGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyGoalDao {
    @Query(
        "SELECT * FROM study_goals WHERE ownerId = :ownerId " +
            "ORDER BY isCompleted ASC, preferredDate ASC, " +
            "CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC, id ASC"
    )
    fun getAllGoalsForOwner(ownerId: String): Flow<List<StudyGoalEntity>>

    @Query(
        "SELECT * FROM study_goals WHERE ownerId = :ownerId " +
            "ORDER BY isCompleted ASC, preferredDate ASC, id ASC"
    )
    suspend fun getAllGoalsForOwnerOnce(ownerId: String): List<StudyGoalEntity>

    @Query(
        "SELECT * FROM study_goals WHERE ownerId = :ownerId AND isCompleted = 0 " +
            "ORDER BY preferredDate ASC, CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC, id ASC"
    )
    fun getActiveGoalsForOwner(ownerId: String): Flow<List<StudyGoalEntity>>

    @Query(
        "SELECT * FROM study_goals WHERE ownerId = :ownerId AND preferredDate = :date " +
            "ORDER BY isCompleted ASC, CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC, id ASC"
    )
    fun getGoalsForOwnerAndDate(ownerId: String, date: String): Flow<List<StudyGoalEntity>>

    @Query(
        "SELECT * FROM study_goals WHERE ownerId = :ownerId AND preferredDate = :date " +
            "ORDER BY isCompleted ASC, CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC, id ASC"
    )
    suspend fun getGoalsForOwnerAndDateOnce(ownerId: String, date: String): List<StudyGoalEntity>

    @Query("SELECT * FROM study_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalByIdOnce(id: Long): StudyGoalEntity?

    @Query("SELECT * FROM study_goals WHERE id = :id AND ownerId = :ownerId LIMIT 1")
    suspend fun getGoalByIdAndOwnerOnce(id: Long, ownerId: String): StudyGoalEntity?

    @Query("SELECT COUNT(*) FROM study_goals WHERE ownerId = :ownerId")
    suspend fun getGoalsCountForOwner(ownerId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: StudyGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<StudyGoalEntity>)

    @Update
    suspend fun updateGoal(goal: StudyGoalEntity)

    @Query("UPDATE study_goals SET isCompleted = :completed, isSynced = 0, updatedAtTimestamp = :updatedAt WHERE id = :id AND ownerId = :ownerId")
    suspend fun setGoalCompleted(id: Long, ownerId: String, completed: Boolean, updatedAt: Long)

    @Query("UPDATE study_goals SET actualMinutes = actualMinutes + :deltaMinutes, isSynced = 0, updatedAtTimestamp = :updatedAt WHERE id = :id")
    suspend fun incrementGoalActualMinutes(id: Long, deltaMinutes: Int, updatedAt: Long)

    @Query("DELETE FROM study_goals WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteGoalByIdAndOwner(id: Long, ownerId: String): Int

    @Query(
        "UPDATE study_goals SET ownerId = :newOwnerId, isSynced = 0, updatedAtTimestamp = :updatedAt " +
            "WHERE ownerId = :guestOwnerId"
    )
    suspend fun reassignGoalsOwner(guestOwnerId: String, newOwnerId: String, updatedAt: Long): Int

    @Query("SELECT * FROM study_goals WHERE ownerId = :ownerId AND isSynced = 0")
    suspend fun getUnsyncedGoalsForOwner(ownerId: String): List<StudyGoalEntity>

    @Query("UPDATE study_goals SET isSynced = 1, updatedAtTimestamp = :updatedAt WHERE ownerId = :ownerId")
    suspend fun markGoalsSyncedForOwner(ownerId: String, updatedAt: Long)
}
