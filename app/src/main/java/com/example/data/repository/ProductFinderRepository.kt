package com.example.data.repository

import android.util.Log
import com.example.data.db.ProductFinderDao
import com.example.data.model.CompanyCacheEntity
import com.example.data.model.CompanyProductsResponse
import com.example.data.model.FavoriteProductEntity
import com.example.data.model.RecentSearchEntity
import com.example.data.api.GeminiClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Resource<Nothing>()
}

class ProductFinderRepository(private val dao: ProductFinderDao) {
    private val TAG = "ProductFinderRepository"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val responseAdapter = moshi.adapter(CompanyProductsResponse::class.java)

    // --- Recent Searches ---
    val recentSearches: Flow<List<RecentSearchEntity>> = dao.getRecentSearches()

    suspend fun addRecentSearch(query: String) = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext
        val trimmed = query.trim()
        // Delete older duplicate if any (to update position with fresh timestamp)
        dao.deleteRecentSearch(trimmed)
        dao.insertRecentSearch(RecentSearchEntity(query = trimmed))
    }

    suspend fun deleteRecentSearch(query: String) = withContext(Dispatchers.IO) {
        dao.deleteRecentSearch(query.trim())
    }

    suspend fun clearRecentSearches() = withContext(Dispatchers.IO) {
        dao.clearRecentSearches()
    }

    // --- Favorites ---
    val favoriteProducts: Flow<List<FavoriteProductEntity>> = dao.getFavorites()

    suspend fun addFavorite(product: FavoriteProductEntity) = withContext(Dispatchers.IO) {
        dao.insertFavorite(product)
    }

    suspend fun deleteFavorite(productId: String) = withContext(Dispatchers.IO) {
        dao.deleteFavorite(productId)
    }

    fun isFavorite(productId: String): Flow<Boolean> {
        return dao.isFavoriteFlow(productId)
    }

    suspend fun isFavoriteDirect(productId: String): Boolean = withContext(Dispatchers.IO) {
        dao.isFavoriteDirect(productId)
    }

    // --- Company Products Loading & Offline Caching ---
    fun getCompanyProducts(companyName: String, forceRefresh: Boolean = false): Flow<Resource<CompanyProductsResponse>> = flow {
        val queryName = companyName.trim().lowercase()
        if (queryName.isEmpty()) {
            emit(Resource.Error("Search query cannot be empty."))
            return@flow
        }

        emit(Resource.Loading)

        // 1. Check local DB caching first
        if (!forceRefresh) {
            try {
                val cached = dao.getCompanyCache(queryName)
                if (cached != null) {
                    val parsed = responseAdapter.fromJson(cached.cachedJson)
                    if (parsed != null) {
                        Log.d(TAG, "Serving from database cache for: $queryName")
                        emit(Resource.Success(parsed))
                        return@flow
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Cache parsing failed: ${e.message}", e)
                // If parsing fails, fall back to fetching fresh data from API
            }
        }

        // 2. Fetch fresh data from Gemini API
        try {
            Log.d(TAG, "Fetching fresh data from Gemini API for: $queryName")
            val apiResponse = GeminiClient.findCompanyProducts(companyName)
            if (apiResponse != null) {
                // Save success to search history since it returned clean results
                addRecentSearch(companyName)

                // Cache in database
                val jsonString = responseAdapter.toJson(apiResponse)
                dao.insertCompanyCache(
                    CompanyCacheEntity(
                        companyName = queryName,
                        cachedJson = jsonString
                    )
                )

                emit(Resource.Success(apiResponse))
            } else {
                emit(Resource.Error("No products found for '$companyName'. Please verify the name."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "API call or data save failed for: $queryName: ${e.message}", e)
            
            // Fallback: If network fails but cache is present, offer cached data as recovery
            val cachedFallback = dao.getCompanyCache(queryName)
            if (cachedFallback != null) {
                try {
                    val parsed = responseAdapter.fromJson(cachedFallback.cachedJson)
                    if (parsed != null) {
                        Log.w(TAG, "Network failed; showing cached copy for: $queryName")
                        emit(Resource.Success(parsed))
                        return@flow
                    }
                } catch (pe: Exception) {
                    Log.e(TAG, "Fallback cache parse failed: ${pe.message}")
                }
            }
            
            val friendlyMsg = if (e.message?.contains("API Key") == true) {
                "Gemini API key is not configured. Please search for help in settings or enter your API key securely into AI Studio Secrets."
            } else {
                "Unable to find products. Please check your internet connection or verify the company spelling."
            }
            emit(Resource.Error(friendlyMsg, e))
        }
    }
}
