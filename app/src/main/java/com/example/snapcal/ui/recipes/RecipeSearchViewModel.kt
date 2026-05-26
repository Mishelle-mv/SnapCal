package com.example.snapcal.ui.recipes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapcal.data.model.Recipe
import com.example.snapcal.data.network.RetrofitClient
import kotlinx.coroutines.launch

class RecipeSearchViewModel : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun searchRecipes(query: String) {
        if (query.isBlank()) {
            _recipes.value = emptyList()
            return
        }
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = RetrofitClient.theMealDbApi.searchRecipes(query)
                _recipes.value = response.meals ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load recipes. Please try again."
                _recipes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearch() {
        _recipes.value = emptyList()
        _error.value = null
    }
}
