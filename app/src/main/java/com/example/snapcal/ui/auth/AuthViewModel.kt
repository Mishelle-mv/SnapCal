package com.example.snapcal.ui.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snapcal.data.repository.AuthRepository
import com.example.snapcal.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
        private const val PROFILE_CREATION_TIMEOUT_MS = 15_000L
    }

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    init {
        if (authRepository.currentUser != null) {
            _authState.value = AuthState.Success
        }
    }

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                authRepository.login(email, password)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = authRepository.register(email, password)
                if (user != null) {
                    // Profile creation is best-effort: if it fails or times out,
                    // the user account is already created so we still navigate forward.
                    try {
                        withTimeoutOrNull(PROFILE_CREATION_TIMEOUT_MS) {
                            userRepository.createUserProfile(user.uid, email, displayName)
                        } ?: Log.w(TAG, "Profile creation timed out, continuing with registration")
                    } catch (e: Exception) {
                        Log.w(TAG, "Profile creation failed, continuing with registration", e)
                    }
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Registration failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun googleSignIn(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = authRepository.googleSignIn(idToken)
                if (user != null) {
                    // Create profile if it doesn't exist — best-effort with timeout.
                    try {
                        withTimeoutOrNull(PROFILE_CREATION_TIMEOUT_MS) {
                            userRepository.createUserProfile(user.uid, user.email ?: "")
                        } ?: Log.w(TAG, "Profile creation timed out during Google sign-in")
                    } catch (e: Exception) {
                        Log.w(TAG, "Profile creation failed during Google sign-in", e)
                    }
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Google Sign-In failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
