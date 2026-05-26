package com.example.snapcal.data.model

import com.google.gson.annotations.SerializedName

data class RecipeResponse(
    @SerializedName("meals")
    val meals: List<Recipe>?
)

data class Recipe(
    @SerializedName("idMeal")
    val idMeal: String,
    @SerializedName("strMeal")
    val title: String,
    @SerializedName("strCategory")
    val category: String?,
    @SerializedName("strArea")
    val area: String?,
    @SerializedName("strInstructions")
    val instructions: String?,
    @SerializedName("strMealThumb")
    val imageUrl: String?
)
