package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CompanyProductsResponse(
    val companyName: String,
    val companyDescription: String,
    val companyFounded: String,
    val companyLogoUrl: String?,
    val products: List<ProductItem>
)

@JsonClass(generateAdapter = true)
data class ProductItem(
    val name: String,
    val category: String,
    val description: String,
    val price: String, // String representation e.g. "$1,299" or "Free"
    val priceNumeric: Double, // Double for sorting e.g. 1299.00
    val productUrl: String, // Official link e.g. https://www.apple.com/iphone-15/
    val imageUrl: String, // Unsplash image e.g. https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500
    val imageSearchQuery: String, // Backup search tag e.g. "smartphone"
    val popularity: Double // For popularity sorting (rating 1.0 to 5.0)
)
