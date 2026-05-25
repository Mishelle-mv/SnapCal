package com.example.snapcal.data.network

import com.example.snapcal.data.model.NutritionSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFoodFactsApi {

    @GET("cgi/search.pl?search_simple=1&action=process&json=1&page_size=20")
    suspend fun searchFood(
        @Query("search_terms") query: String
    ): NutritionSearchResponse
}
