package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.LearningMaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningMaterialDao {
    @Query("SELECT * FROM learning_materials ORDER BY id DESC")
    fun getAllMaterials(): Flow<List<LearningMaterialEntity>>

    @Query("SELECT * FROM learning_materials WHERE id = :id LIMIT 1")
    fun getMaterialById(id: Long): Flow<LearningMaterialEntity?>

    @Query("SELECT * FROM learning_materials WHERE id = :id LIMIT 1")
    suspend fun getMaterialByIdOnce(id: Long): LearningMaterialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: LearningMaterialEntity): Long

    @Update
    suspend fun updateMaterial(material: LearningMaterialEntity)

    @Query("DELETE FROM learning_materials WHERE id = :id")
    suspend fun deleteMaterialById(id: Long)

    @Query("SELECT COUNT(*) FROM learning_materials")
    suspend fun getMaterialsCount(): Int

    @Query("SELECT * FROM learning_materials")
    suspend fun getAllMaterialsOnce(): List<LearningMaterialEntity>
}
