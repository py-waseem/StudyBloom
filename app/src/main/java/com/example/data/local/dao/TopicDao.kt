package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM material_topics WHERE materialId = :materialId ORDER BY orderIndex ASC")
    fun getTopicsForMaterial(materialId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM material_topics WHERE materialId = :materialId ORDER BY orderIndex ASC")
    suspend fun getTopicsForMaterialOnce(materialId: Long): List<TopicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("UPDATE material_topics SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTopicCompletion(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM material_topics WHERE id = :id")
    suspend fun deleteTopicById(id: Long)

    @Query("DELETE FROM material_topics WHERE materialId = :materialId")
    suspend fun deleteTopicsForMaterial(materialId: Long)
}
