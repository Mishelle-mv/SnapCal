package com.example.snapcal.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://world.openfoodfacts.org/"

    val openFoodFactsApi: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    val theMealDbApi: TheMealDbApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.themealdb.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TheMealDbApi::class.java)
    }
}
