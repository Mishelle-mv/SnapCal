package com.example.snapcal.data.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import com.example.snapcal.data.FirebaseConstants
import com.example.snapcal.data.local.MealDao
import com.example.snapcal.data.local.MealEntity
import com.example.snapcal.data.local.SnapCalDatabase
import com.example.snapcal.data.local.toEntity
import com.example.snapcal.data.model.Meal
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class MealRepository(private val context: Context) {

    @Volatile
    private var mealDao: MealDao? = null

    private suspend fun getMealDao(): MealDao = withContext(Dispatchers.IO) {
        mealDao ?: SnapCalDatabase.getInstance(context.applicationContext)
            .mealDao()
            .also { mealDao = it }
    }

    fun getAuthStatus(): AuthStatus {
        if (FirebaseApp.getApps(context).isEmpty()) {
            return AuthStatus.FirebaseNotConfigured
        }
        val user = FirebaseAuth.getInstance().currentUser
        return if (user != null) {
            AuthStatus.LoggedIn(user.uid)
        } else {
            AuthStatus.NotLoggedIn
        }
    }

    suspend fun observeMealsByUserId(userId: String): LiveData<List<MealEntity>> {
        return getMealDao().getByUserId(userId)
    }

    suspend fun refreshMealsForUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            return@withContext Result.failure(FirebaseNotConfiguredException())
        }
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser?.uid != userId) {
            return@withContext Result.failure(NotLoggedInException())
        }
        try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection(FirebaseConstants.MEALS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val meals = snapshot.documents.mapNotNull { documentToMeal(it) }
            val dao = getMealDao()
            dao.deleteByUserId(userId)
            if (meals.isNotEmpty()) {
                dao.insertAll(meals.map { it.toEntity() })
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

            getMealDao().insert(meal.toEntity())
            Result.success(meal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun documentToMeal(document: DocumentSnapshot): Meal? {
        val userId = document.getString("userId") ?: return null
        val description = document.getString("description") ?: return null
        val imageUrl = document.getString("imageUrl") ?: return null
        val userDisplayName = document.getString("userDisplayName") ?: ""
        val calories = document.getLong("calories")?.toInt() ?: return null
        val createdAt = document.getLong("createdAt") ?: return null
        return Meal(
            id = document.id,
            userId = userId,
            userDisplayName = userDisplayName,
            description = description,
            calories = calories,
            imageUrl = imageUrl,
            createdAt = createdAt
        )
    }

    sealed class AuthStatus {
        data object FirebaseNotConfigured : AuthStatus()
        data object NotLoggedIn : AuthStatus()
        data class LoggedIn(val userId: String) : AuthStatus()
    }

    class NotLoggedInException : Exception("not_logged_in")

    class FirebaseNotConfiguredException : Exception("firebase_not_configured")
}
