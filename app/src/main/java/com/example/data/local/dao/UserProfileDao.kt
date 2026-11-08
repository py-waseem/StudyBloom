package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET themeMode = :mode WHERE id = 1")
    suspend fun updateThemeMode(mode: String)

    @Query("UPDATE user_profile SET hasCompletedOnboarding = 1 WHERE id = 1")
    suspend fun markOnboardingComplete()

    @Query("UPDATE user_profile SET currentStreakDays = :streak, lastActiveDate = :date WHERE id = 1")
    suspend fun updateStreak(streak: Int, date: String)
}
