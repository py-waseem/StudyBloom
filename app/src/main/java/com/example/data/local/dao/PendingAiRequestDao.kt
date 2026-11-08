package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PendingAiRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingAiRequestDao {
    @Query("SELECT * FROM pending_ai_requests ORDER BY createdAtTimestamp DESC")
    fun getAllRequests(): Flow<List<PendingAiRequestEntity>>

    @Query("SELECT * FROM pending_ai_requests WHERE status = 'QUEUED' ORDER BY createdAtTimestamp ASC")
    suspend fun getQueuedRequests(): List<PendingAiRequestEntity>

    @Query("SELECT COUNT(*) FROM pending_ai_requests WHERE status = 'QUEUED'")
    fun getQueuedCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: PendingAiRequestEntity): Long

    @Update
    suspend fun updateRequest(request: PendingAiRequestEntity)

    @Query("DELETE FROM pending_ai_requests WHERE id = :id")
    suspend fun deleteRequest(id: Long)

    @Query("DELETE FROM pending_ai_requests WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()
}
