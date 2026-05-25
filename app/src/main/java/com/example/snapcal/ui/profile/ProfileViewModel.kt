package com.example.snapcal.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapcal.data.model.UserProfile
import com.example.snapcal.data.repository.AuthRepository
import com.example.snapcal.data.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _logoutSuccess = MutableLiveData<Boolean>()
    val logoutSuccess: LiveData<Boolean> = _logoutSuccess

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = userRepository.getUserProfile(user.uid)
                _userProfile.value = profile
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(displayName: String, imageUri: Uri?) {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (imageUri != null) {
                    userRepository.uploadProfilePicture(user.uid, imageUri)
                }
                userRepository.updateDisplayName(user.uid, displayName)
                loadProfile() // refresh
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _logoutSuccess.value = true
    }
}
