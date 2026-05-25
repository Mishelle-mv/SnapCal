package com.example.snapcal.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    fun getByUserId(userId: String): LiveData<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getByUserIdOnce(userId: String): UserProfileEntity?
}
