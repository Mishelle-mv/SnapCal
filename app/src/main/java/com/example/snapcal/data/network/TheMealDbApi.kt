package com.example.snapcal.data.network

import com.example.snapcal.data.model.RecipeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TheMealDbApi {
    @GET("api/json/v1/1/search.php")
    suspend fun searchRecipes(
        @Query("s") query: String
    ): RecipeResponse
}
