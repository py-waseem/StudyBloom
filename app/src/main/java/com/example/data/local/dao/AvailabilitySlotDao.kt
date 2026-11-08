package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AvailabilitySlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvailabilitySlotDao {
    @Query("SELECT * FROM availability_slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllSlots(): Flow<List<AvailabilitySlotEntity>>

    @Query("SELECT * FROM availability_slots WHERE dayOfWeek = :dayOfWeek AND isEnabled = 1 ORDER BY startTime ASC")
    suspend fun getSlotsForDay(dayOfWeek: Int): List<AvailabilitySlotEntity>

    @Query("SELECT * FROM availability_slots ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAllSlotsOnce(): List<AvailabilitySlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<AvailabilitySlotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: AvailabilitySlotEntity): Long

    @Update
    suspend fun updateSlot(slot: AvailabilitySlotEntity)

    @Query("DELETE FROM availability_slots WHERE id = :id")
    suspend fun deleteSlotById(id: Long)

    @Query("DELETE FROM availability_slots")
    suspend fun clearAll()
}
