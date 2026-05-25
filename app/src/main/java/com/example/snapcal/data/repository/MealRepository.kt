package com.example.snapcal.data.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.snapcal.data.FirebaseConstants
import com.example.snapcal.data.local.MealDao
import com.example.snapcal.data.local.MealEntity
import com.example.snapcal.data.local.SnapCalDatabase
import com.example.snapcal.data.local.toEntity
import com.example.snapcal.data.local.toMeal
import com.example.snapcal.data.model.Meal
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    suspend fun getMealById(mealId: String): Result<Meal> = withContext(Dispatchers.IO) {
        val authResult = requireLoggedInUser()
        if (authResult.isFailure) {
            return@withContext Result.failure(authResult.exceptionOrNull()!!)
        }
        val userId = authResult.getOrNull()!!
        try {
            val dao = getMealDao()
            val cached = dao.getById(mealId)?.toMeal()
            if (cached != null) {
                return@withContext if (cached.userId == userId) {
                    Result.success(cached)
                } else {
                    Result.failure(UnauthorizedMealException())
                }
            }
            if (FirebaseApp.getApps(context).isEmpty()) {
                return@withContext Result.failure(FirebaseNotConfiguredException())
            }
            val firestore = FirebaseFirestore.getInstance()
            val document = firestore.collection(FirebaseConstants.MEALS_COLLECTION)
                .document(mealId)
                .get()
                .await()
            if (!document.exists()) {
                return@withContext Result.failure(MealNotFoundException())
            }
            val meal = documentToMeal(document) ?: return@withContext Result.failure(MealNotFoundException())
            if (meal.userId != userId) {
                return@withContext Result.failure(UnauthorizedMealException())
            }
            dao.insert(meal.toEntity())
            Result.success(meal)
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

            val mealData = mealToMap(meal)
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

    suspend fun updateMeal(
        mealId: String,
        description: String,
        calories: Int,
        newImageUri: Uri?
    ): Result<Meal> = withContext(Dispatchers.IO) {
        val authResult = requireLoggedInUser()
        if (authResult.isFailure) {
            return@withContext Result.failure(authResult.exceptionOrNull()!!)
        }
        val userId = authResult.getOrNull()!!
        if (FirebaseApp.getApps(context).isEmpty()) {
            return@withContext Result.failure(FirebaseNotConfiguredException())
        }
        try {
            val existing = loadOwnedMeal(mealId, userId)
                ?: return@withContext Result.failure(MealNotFoundException())
            val firestore = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            var imageUrl = existing.imageUrl
            if (newImageUri != null) {
                val storageRef = storage.reference
                    .child("meal_images")
                    .child(userId)
                    .child("$mealId.jpg")
                storageRef.putFile(newImageUri).await()
                imageUrl = storageRef.downloadUrl.await().toString()
            }
            val updatedMeal = existing.copy(
                description = description.trim(),
                calories = calories,
                imageUrl = imageUrl
            )
            firestore.collection(FirebaseConstants.MEALS_COLLECTION)
                .document(mealId)
                .set(mealToMap(updatedMeal))
                .await()
            getMealDao().insert(updatedMeal.toEntity())
            Result.success(updatedMeal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMeal(mealId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val authResult = requireLoggedInUser()
        if (authResult.isFailure) {
            return@withContext Result.failure(authResult.exceptionOrNull()!!)
        }
        val userId = authResult.getOrNull()!!
        if (FirebaseApp.getApps(context).isEmpty()) {
            return@withContext Result.failure(FirebaseNotConfiguredException())
        }
        try {
            val existing = loadOwnedMeal(mealId, userId)
                ?: return@withContext Result.failure(MealNotFoundException())
            val firestore = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            firestore.collection(FirebaseConstants.MEALS_COLLECTION)
                .document(mealId)
                .delete()
                .await()
            try {
                storage.reference
                    .child("meal_images")
                    .child(userId)
                    .child("$mealId.jpg")
                    .delete()
                    .await()
            } catch (_: Exception) {
            }
            getMealDao().deleteById(mealId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFeedMeals(): LiveData<List<Meal>> {
        val dao = SnapCalDatabase.getInstance(context.applicationContext).mealDao()
        return dao.getAll().map { entities ->
            entities.map { it.toMeal() }
        }
    }

    suspend fun fetchMealsFromRemote() {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val firestore = FirebaseFirestore.getInstance()
        try {
            val result = firestore.collection(FirebaseConstants.MEALS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val remoteMeals = result.documents.mapNotNull { documentToMeal(it) }
            getMealDao().insertAll(remoteMeals.map { it.toEntity() })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadOwnedMeal(mealId: String, userId: String): Meal? {
        val dao = getMealDao()
        val cached = dao.getById(mealId)?.toMeal()
        if (cached != null) {
            return if (cached.userId == userId) cached else null
        }
        val firestore = FirebaseFirestore.getInstance()
        val document = firestore.collection(FirebaseConstants.MEALS_COLLECTION)
            .document(mealId)
            .get()
            .await()
        if (!document.exists()) {
            return null
        }
        val meal = documentToMeal(document) ?: return null
        if (meal.userId != userId) {
            return null
        }
        dao.insert(meal.toEntity())
        return meal
    }

    private fun requireLoggedInUser(): Result<String> {
        if (FirebaseApp.getApps(context).isEmpty()) {
            return Result.failure(FirebaseNotConfiguredException())
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(NotLoggedInException())
        return Result.success(userId)
    }

    private fun mealToMap(meal: Meal): HashMap<String, Any> {
        return hashMapOf(
            "userId" to meal.userId,
            "userDisplayName" to meal.userDisplayName,
            "description" to meal.description,
            "calories" to meal.calories,
            "imageUrl" to meal.imageUrl,
            "createdAt" to meal.createdAt
        )
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

    class MealNotFoundException : Exception("meal_not_found")

    class UnauthorizedMealException : Exception("unauthorized_meal")
}
