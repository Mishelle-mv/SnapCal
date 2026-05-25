package com.example.snapcal.ui.mymeals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.snapcal.R
import com.example.snapcal.data.local.MealEntity
import com.example.snapcal.data.local.toMeal
import com.example.snapcal.data.model.Meal
import com.example.snapcal.data.repository.MealRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyMealsViewModel(application: Application) : AndroidViewModel(application) {

    private val mealRepository = MealRepository(application)

    private val _screenState = MutableLiveData<MyMealsScreenState>()
    val screenState: LiveData<MyMealsScreenState> = _screenState

    private val _meals = MediatorLiveData<List<Meal>>()
    val meals: LiveData<List<Meal>> = _meals

    private var roomSource: LiveData<List<MealEntity>>? = null

    fun loadMeals() {
        viewModelScope.launch {
            val authStatus = withContext(Dispatchers.IO) {
                mealRepository.getAuthStatus()
            }
            when (authStatus) {
                is MealRepository.AuthStatus.FirebaseNotConfigured -> {
                    clearRoomSource()
                    _meals.value = emptyList()
                    _screenState.value = MyMealsScreenState.FirebaseNotConfigured
                }
                is MealRepository.AuthStatus.NotLoggedIn -> {
                    clearRoomSource()
                    _meals.value = emptyList()
                    _screenState.value = MyMealsScreenState.NotLoggedIn
                }
                is MealRepository.AuthStatus.LoggedIn -> {
                    clearRoomSource()
                    _screenState.value = MyMealsScreenState.Loading
                    val source = withContext(Dispatchers.IO) {
                        mealRepository.observeMealsByUserId(authStatus.userId)
                    }
                    roomSource = source
                    _meals.addSource(source) { entities ->
                        _meals.value = entities.map { it.toMeal() }
                        if (_screenState.value is MyMealsScreenState.Loading ||
                            _screenState.value is MyMealsScreenState.Ready
                        ) {
                            _screenState.value = MyMealsScreenState.Ready(entities.isEmpty())
                        }
                    }
                    refreshRemoteMeals(authStatus.userId)
                }
            }
        }
    }

    private fun refreshRemoteMeals(userId: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                mealRepository.refreshMealsForUser(userId)
            }
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                val cachedMeals = _meals.value.orEmpty()
                when (exception) {
                    is MealRepository.FirebaseNotConfiguredException ->
                        _screenState.value = MyMealsScreenState.FirebaseNotConfigured
                    is MealRepository.NotLoggedInException ->
                        _screenState.value = MyMealsScreenState.NotLoggedIn
                    else -> {
                        _screenState.value = if (cachedMeals.isEmpty()) {
                            MyMealsScreenState.Error(
                                getApplication<Application>().getString(R.string.error_load_meals_failed)
                            )
                        } else {
                            MyMealsScreenState.Ready(isEmpty = false)
                        }
                    }
                }
            }
        }
    }

    private fun clearRoomSource() {
        roomSource?.let { _meals.removeSource(it) }
        roomSource = null
    }
}
