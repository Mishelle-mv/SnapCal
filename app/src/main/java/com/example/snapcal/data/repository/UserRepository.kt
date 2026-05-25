package com.example.snapcal.data.repository

import com.example.snapcal.data.FirebaseConstants
import com.example.snapcal.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun createUserProfile(userId: String, email: String) {
        val name = email.substringBefore("@")
        val profile = UserProfile(
            userId = userId,
            displayName = name,
            photoUrl = ""
        )
        firestore.collection(FirebaseConstants.USERS_COLLECTION)
            .document(userId)
            .set(profile)
            .await()
    }
}
