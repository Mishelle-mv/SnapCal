package com.example.snapcal.data.model

data class Meal(
    val id: String,
    val userId: String,
    val userDisplayName: String,
    val description: String,
    val calories: Int,
    val imageUrl: String,
    val createdAt: Long
)
