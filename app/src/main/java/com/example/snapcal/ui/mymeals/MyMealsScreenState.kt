package com.example.snapcal.ui.mymeals

sealed class MyMealsScreenState {
    data object Loading : MyMealsScreenState()
    data object FirebaseNotConfigured : MyMealsScreenState()
    data object NotLoggedIn : MyMealsScreenState()
    data class Ready(val isEmpty: Boolean) : MyMealsScreenState()
    data class Error(val message: String) : MyMealsScreenState()
}
