package com.example.snapcal.data.repository

import android.content.Context
import com.example.snapcal.data.FirebaseConstants
import com.example.snapcal.data.model.UserProfile
import com.example.snapcal.util.ImageCompressor
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun createUserProfile(userId: String, email: String, displayName: String? = null) {
        val name = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
        val profile = UserProfile(
            userId = userId,
            displayName = name,
            photoUrl = ""
        )
        firestore.collection(FirebaseConstants.USERS_COLLECTION)
            .document(userId)
            .set(profile)
            .await()
        
        // Also update Firebase Auth profile so it's instantly available
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null && user.uid == userId) {
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                this.displayName = name
            }
            user.updateProfile(profileUpdates).await()
        }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        val snapshot = firestore.collection(FirebaseConstants.USERS_COLLECTION)
            .document(userId)
            .get()
            .await()
        var profile = snapshot.toObject(UserProfile::class.java)
        
        // Fallback to Auth data if Firestore document is missing
        if (profile == null) {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user != null && user.uid == userId) {
                profile = UserProfile(
                    userId = userId,
                    displayName = user.displayName ?: "",
                    photoUrl = user.photoUrl?.toString() ?: ""
                )
                // Attempt to create the document so future updates work normally
                try {
                    firestore.collection(FirebaseConstants.USERS_COLLECTION)
                        .document(userId)
                        .set(profile)
                        .await()
                } catch (e: Exception) {
                    // Ignore, we will just return the fallback profile
                }
            }
        }
        return profile
    }

    suspend fun updateDisplayName(userId: String, displayName: String) {
        firestore.collection(FirebaseConstants.USERS_COLLECTION)
            .document(userId)
            .set(mapOf("displayName" to displayName), SetOptions.merge())
            .await()

        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null && user.uid == userId) {
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                this.displayName = displayName
            }
            user.updateProfile(profileUpdates).await()
        }
    }

    suspend fun uploadProfilePicture(userId: String, imageUri: android.net.Uri): String = withContext(Dispatchers.IO) {
        val storageRef = storage.reference
            .child("profile_images")
            .child("$userId.jpg")

        val compressedUri = ImageCompressor.compressImage(context, imageUri)
        storageRef.putFile(compressedUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        firestore.collection(FirebaseConstants.USERS_COLLECTION)
            .document(userId)
            .set(mapOf("photoUrl" to downloadUrl), SetOptions.merge())
            .await()

        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null && user.uid == userId) {
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                this.photoUri = android.net.Uri.parse(downloadUrl)
            }
            user.updateProfile(profileUpdates).await()
        }

        downloadUrl
    }
}
