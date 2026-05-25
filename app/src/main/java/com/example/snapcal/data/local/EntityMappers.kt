package com.example.snapcal.data.local

import com.example.snapcal.data.model.Meal
import com.example.snapcal.data.model.UserProfile

fun MealEntity.toMeal(): Meal {
    return Meal(
        id = id,
        userId = userId,
        userDisplayName = userDisplayName,
        description = description,
        calories = calories,
        imageUrl = imageUrl,
        createdAt = createdAt
    )
}

fun Meal.toEntity(): MealEntity {
    return MealEntity(
        id = id,
        userId = userId,
        userDisplayName = userDisplayName,
        description = description,
        calories = calories,
        imageUrl = imageUrl,
        createdAt = createdAt
    )
}

fun UserProfileEntity.toUserProfile(): UserProfile {
    return UserProfile(
        userId = userId,
        displayName = displayName,
        photoUrl = photoUrl
    )
}

fun UserProfile.toEntity(): UserProfileEntity {
    return UserProfileEntity(
        userId = userId,
        displayName = displayName,
        photoUrl = photoUrl
    )
}
