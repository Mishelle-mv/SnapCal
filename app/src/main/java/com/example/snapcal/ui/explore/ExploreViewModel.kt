package com.example.snapcal.ui.explore

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snapcal.data.model.Meal
import com.example.snapcal.data.repository.MealRepository
import kotlinx.coroutines.launch

class ExploreViewModel(private val mealRepository: MealRepository) : ViewModel() {

    val feedMeals: LiveData<List<Meal>> = mealRepository.getFeedMeals()

    init {
        fetchFeed()
    }

    fun fetchFeed() {
        viewModelScope.launch {
            mealRepository.fetchMealsFromRemote()
        }
    }
}

class ExploreViewModelFactory(
    private val mealRepository: MealRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExploreViewModel(mealRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
