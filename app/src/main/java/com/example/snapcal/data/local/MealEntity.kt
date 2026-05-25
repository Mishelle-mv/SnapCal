package com.example.snapcal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userDisplayName: String,
    val description: String,
    val calories: Int,
    val imageUrl: String,
    val createdAt: Long
)
