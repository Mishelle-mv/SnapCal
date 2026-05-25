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

    suspend fun getUserProfile(userId: String): UserProfile? {
        val snapshot = firestore.collection(FirebaseConstants.USERS_COLLECTION)
            .document(userId)
            .get()
            .await()
        return snapshot.toObject(UserProfile::class.java)
    }

    suspend fun updateDisplayName(userId: String, displayName: String) {
        firestore.collection(FirebaseConstants.USERS_COLLECTION)
            .document(userId)
            .update("displayName", displayName)
            .await()

        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null && user.uid == userId) {
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                this.displayName = displayName
            }
            user.updateProfile(profileUpdates).await()
        }
    }

    suspend fun uploadProfilePicture(userId: String, imageUri: android.net.Uri): String {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            .child("profile_images")
            .child("$userId.jpg")

        storageRef.putFile(imageUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        firestore.collection(FirebaseConstants.USERS_COLLECTION)
            .document(userId)
            .update("photoUrl", downloadUrl)
            .await()

        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null && user.uid == userId) {
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                this.photoUri = android.net.Uri.parse(downloadUrl)
            }
            user.updateProfile(profileUpdates).await()
        }

        return downloadUrl
    }
}
