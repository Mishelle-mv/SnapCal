package com.example.snapcal.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meals: List<MealEntity>)

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteById(mealId: String)

    @Query("DELETE FROM meals WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("DELETE FROM meals")
    suspend fun deleteAll()

    @Query("SELECT * FROM meals ORDER BY createdAt DESC")
    fun getAll(): LiveData<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getByUserId(userId: String): LiveData<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :mealId LIMIT 1")
    suspend fun getById(mealId: String): MealEntity?
}
