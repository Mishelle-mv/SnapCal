package com.example.snapcal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MealEntity::class, UserProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SnapCalDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var instance: SnapCalDatabase? = null

        fun getInstance(context: Context): SnapCalDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SnapCalDatabase::class.java,
                    "snapcal_database"
                ).build().also { instance = it }
            }
        }
    }
}
