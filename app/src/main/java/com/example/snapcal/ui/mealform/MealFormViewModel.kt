package com.example.snapcal.ui.mealform

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.snapcal.R
import com.example.snapcal.data.repository.MealRepository
import com.example.snapcal.util.Resource
import kotlinx.coroutines.launch

class MealFormViewModel(application: Application) : AndroidViewModel(application) {

    private val mealRepository = MealRepository(application)

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    private val _saveState = MutableLiveData<Resource<Unit>>()
    val saveState: LiveData<Resource<Unit>> = _saveState

    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun resetSaveState() {
        _saveState.value = null
    }

    fun saveMeal(description: String, caloriesText: String) {
        val validationError = validate(description, caloriesText, _selectedImageUri.value)
        if (validationError != null) {
            _saveState.value = Resource.Error(validationError)
            return
        }

        val calories = caloriesText.trim().toInt()
        _saveState.value = Resource.Loading
        viewModelScope.launch {
            val result = mealRepository.addMeal(
                description = description,
                calories = calories,
                imageUri = _selectedImageUri.value!!
            )
            _saveState.value = when {
                result.isSuccess -> Resource.Success(Unit)
                result.exceptionOrNull() is MealRepository.FirebaseNotConfiguredException ->
                    Resource.Error(getApplication<Application>().getString(R.string.error_firebase_not_configured))
                result.exceptionOrNull() is MealRepository.NotLoggedInException ->
                    Resource.Error(getApplication<Application>().getString(R.string.error_not_logged_in))
                else ->
                    Resource.Error(getApplication<Application>().getString(R.string.error_save_failed))
            }
        }
    }

    private fun validate(description: String, caloriesText: String, imageUri: Uri?): String? {
        val context = getApplication<Application>()
        if (imageUri == null) {
            return context.getString(R.string.error_image_required)
        }
        if (description.trim().isEmpty()) {
            return context.getString(R.string.error_description_required)
        }
        val calories = caloriesText.trim().toIntOrNull()
        if (calories == null || calories <= 0) {
            return context.getString(R.string.error_calories_invalid)
        }
        return null
    }
}
