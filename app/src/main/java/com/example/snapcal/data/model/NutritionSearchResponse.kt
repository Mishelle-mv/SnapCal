package com.example.snapcal.data.model

import com.google.gson.annotations.SerializedName

data class NutritionSearchResponse(
    @SerializedName("products")
    val products: List<NutritionProduct>
)

data class NutritionProduct(
    @SerializedName("_id")
    val id: String,
    @SerializedName("product_name")
    val productName: String?,
    @SerializedName("brands")
    val brands: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("nutriments")
    val nutriments: Nutriments?
)

data class Nutriments(
    @SerializedName("energy-kcal_100g")
    val energyKcal100g: Double?
)
