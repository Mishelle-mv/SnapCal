package com.example.snapcal.ui.mealform

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.snapcal.R
import com.example.snapcal.data.model.Meal
import com.example.snapcal.data.repository.MealRepository
import com.example.snapcal.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MealFormViewModel(application: Application) : AndroidViewModel(application) {

    private val mealRepository = MealRepository(application)

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    private val _loadState = MutableLiveData<Resource<Meal>>()
    val loadState: LiveData<Resource<Meal>> = _loadState

    private val _saveState = MutableLiveData<Resource<Unit>>()
    val saveState: LiveData<Resource<Unit>> = _saveState

    private var isEditMode = false
    private var mealId: String? = null
    private var existingImageUrl: String? = null
    private var imageChanged = false

    fun init(mode: String, mealId: String?) {
        isEditMode = mode == MODE_EDIT
        if (isEditMode) {
            val id = mealId?.takeIf { it.isNotBlank() }
            if (id == null) {
                _loadState.value = Resource.Error(
                    getApplication<Application>().getString(R.string.error_meal_not_found)
                )
                return
            }
            this.mealId = id
            loadMeal(id)
        }
    }

    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
        if (uri != null && isEditMode) {
            imageChanged = true
        }
    }

    fun resetSaveState() {
        _saveState.value = null
    }

    fun saveMeal(description: String, caloriesText: String) {
        val validationError = validate(description, caloriesText)
        if (validationError != null) {
            _saveState.value = Resource.Error(validationError)
            return
        }
        val calories = caloriesText.trim().toInt()
        _saveState.value = Resource.Loading
        viewModelScope.launch {
            val result = if (isEditMode) {
                mealRepository.updateMeal(
                    mealId = mealId!!,
                    description = description,
                    calories = calories,
                    newImageUri = if (imageChanged) _selectedImageUri.value else null
                )
            } else {
                mealRepository.addMeal(
                    description = description,
                    calories = calories,
                    imageUri = _selectedImageUri.value!!
                )
            }
            _saveState.value = when {
                result.isSuccess -> Resource.Success(Unit)
                else -> Resource.Error(mapExceptionToMessage(result.exceptionOrNull()))
            }
        }
    }

    fun deleteMeal() {
        val id = mealId ?: return
        _saveState.value = Resource.Loading
        viewModelScope.launch {
            val result = mealRepository.deleteMeal(id)
            _saveState.value = when {
                result.isSuccess -> Resource.Success(Unit)
                else -> Resource.Error(mapExceptionToMessage(result.exceptionOrNull()))
            }
        }
    }

    private fun loadMeal(mealId: String) {
        _loadState.value = Resource.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                mealRepository.getMealById(mealId)
            }
            _loadState.value = when {
                result.isSuccess -> {
                    val meal = result.getOrNull()!!
                    existingImageUrl = meal.imageUrl
                    Resource.Success(meal)
                }
                else -> Resource.Error(mapExceptionToMessage(result.exceptionOrNull()))
            }
        }
    }

    private fun validate(description: String, caloriesText: String): String? {
        val context = getApplication<Application>()
        val hasImage = _selectedImageUri.value != null || !existingImageUrl.isNullOrBlank()
        if (!hasImage) {
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

    private fun mapExceptionToMessage(exception: Throwable?): String {
        val context = getApplication<Application>()
        return when (exception) {
            is MealRepository.FirebaseNotConfiguredException ->
                context.getString(R.string.error_firebase_not_configured)
            is MealRepository.NotLoggedInException ->
                context.getString(R.string.error_not_logged_in)
            is MealRepository.MealNotFoundException ->
                context.getString(R.string.error_meal_not_found)
            is MealRepository.UnauthorizedMealException ->
                context.getString(R.string.error_unauthorized_meal)
            else -> context.getString(R.string.error_save_failed)
        }
    }

    companion object {
        const val MODE_ADD = "add"
        const val MODE_EDIT = "edit"
    }
}
