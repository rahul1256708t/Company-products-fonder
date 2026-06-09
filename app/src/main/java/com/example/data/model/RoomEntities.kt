package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_products")
data class FavoriteProductEntity(
    @PrimaryKey val id: String, // format: "companyName_productName"
    val companyName: String,
    val name: String,
    val category: String,
    val description: String,
    val price: String,
    val imageUrl: String,
    val productUrl: String,
    val rating: Double = 4.5,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "company_caches")
data class CompanyCacheEntity(
    @PrimaryKey val companyName: String,
    val cachedJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
