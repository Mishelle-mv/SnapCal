package com.example.snapcal.data.repository

import android.content.Context
import android.net.Uri
import com.example.snapcal.data.FirebaseConstants
import com.example.snapcal.data.local.SnapCalDatabase
import com.example.snapcal.data.local.toEntity
import com.example.snapcal.data.model.Meal
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MealRepository(private val context: Context) {

    private val mealDao = SnapCalDatabase.getInstance(context).mealDao()

    suspend fun addMeal(description: String, calories: Int, imageUri: Uri): Result<Meal> {
        if (FirebaseApp.getApps(context).isEmpty()) {
            return Result.failure(FirebaseNotConfiguredException())
        }

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        val user = auth.currentUser ?: return Result.failure(NotLoggedInException())
        val userId = user.uid
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.takeIf { it.isNotBlank() }
            ?: "User"
        val mealId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()

        return try {
            val storageRef = storage.reference
                .child("meal_images")
                .child(userId)
                .child("$mealId.jpg")
            storageRef.putFile(imageUri).await()
            val imageUrl = storageRef.downloadUrl.await().toString()

            val meal = Meal(
                id = mealId,
                userId = userId,
                userDisplayName = displayName,
                description = description.trim(),
                calories = calories,
                imageUrl = imageUrl,
                createdAt = createdAt
            )

            val mealData = hashMapOf(
                "userId" to meal.userId,
                "userDisplayName" to meal.userDisplayName,
                "description" to meal.description,
                "calories" to meal.calories,
                "imageUrl" to meal.imageUrl,
                "createdAt" to meal.createdAt
            )
            firestore.collection(FirebaseConstants.MEALS_COLLECTION)
                .document(mealId)
                .set(mealData)
                .await()

            mealDao.insert(meal.toEntity())
            Result.success(meal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    class NotLoggedInException : Exception("not_logged_in")

    class FirebaseNotConfiguredException : Exception("firebase_not_configured")
}
