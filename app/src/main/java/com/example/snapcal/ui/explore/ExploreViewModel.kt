package com.example.snapcal.ui.explore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snapcal.data.model.Meal
import com.example.snapcal.data.model.NutritionProduct
import com.example.snapcal.data.network.RetrofitClient
import com.example.snapcal.data.repository.MealRepository
import kotlinx.coroutines.launch

class ExploreViewModel(private val mealRepository: MealRepository) : ViewModel() {

    val feedMeals: LiveData<List<Meal>> = mealRepository.getFeedMeals()

    private val _searchResults = MutableLiveData<List<NutritionProduct>>()
    val searchResults: LiveData<List<NutritionProduct>> = _searchResults

    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = _isSearching

    init {
        fetchFeed()
    }

    fun fetchFeed() {
        viewModelScope.launch {
            mealRepository.fetchMealsFromRemote()
        }
    }

    fun searchNutrition(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.openFoodFactsApi.searchFood(query)
                _searchResults.value = response.products
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
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
