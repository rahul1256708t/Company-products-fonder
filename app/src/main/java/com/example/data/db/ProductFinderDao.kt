package com.example.data.db

import androidx.room.*
import com.example.data.model.CompanyCacheEntity
import com.example.data.model.FavoriteProductEntity
import com.example.data.model.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductFinderDao {

    // --- Recent Searches ---
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()

    // --- Favorite Products ---
    @Query("SELECT * FROM favorite_products ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<FavoriteProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(product: FavoriteProductEntity)

    @Query("DELETE FROM favorite_products WHERE id = :id")
    suspend fun deleteFavorite(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_products WHERE id = :id)")
    fun isFavoriteFlow(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_products WHERE id = :id)")
    suspend fun isFavoriteDirect(id: String): Boolean

    // --- Company Cache ---
    @Query("SELECT * FROM company_caches WHERE companyName = :companyName LIMIT 1")
    suspend fun getCompanyCache(companyName: String): CompanyCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanyCache(cache: CompanyCacheEntity)

    @Query("DELETE FROM company_caches WHERE companyName = :companyName")
    suspend fun deleteCompanyCache(companyName: String)
}
