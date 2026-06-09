package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CompanyProductsResponse
import com.example.data.model.FavoriteProductEntity
import com.example.data.model.ProductItem
import com.example.data.repository.ProductFinderRepository
import com.example.data.repository.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption {
    POPULARITY_DESC,
    PRICE_ASC,
    PRICE_DESC,
    NAME_ASC
}

class ProductFinderViewModel(
    application: Application,
    private val repository: ProductFinderRepository
) : AndroidViewModel(application) {

    // --- State StateFlows ---
    val searchQuery = MutableStateFlow("")
    val filterQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val selectedSortOption = MutableStateFlow(SortOption.POPULARITY_DESC)

    private val _searchResult = MutableStateFlow<Resource<CompanyProductsResponse>?>(null)
    val searchResult: StateFlow<Resource<CompanyProductsResponse>?> = _searchResult.asStateFlow()

    // --- Autocomplete Suggestions ---
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    // --- Raw DB streams ---
    val recentSearches = repository.recentSearches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteProducts = repository.favoriteProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // A static list of popular world-class brand names for autocomplete
    private val popularBrands = listOf(
        "Apple", "Microsoft", "Google", "Amazon", "Tesla", "Samsung", "Sony", "Nike", 
        "Adidas", "Toyota", "Starbucks", "Netflix", "Spotify", "Coca-Cola", "PepsiCo", 
        "McDonald's", "Nintendo", "Airbnb", "Uber", "Adobe", "IKEA", "Rolex", "Lego", 
        "Disney", "Ford", "Dell", "HP", "Intel", "AMD", "NVIDIA", "Meta", "Slack", 
        "Zoom", "Dropbox", "Shopify", "PayPal", "Visa", "Mastercard", "Audi", "BMW", 
        "Mercedes-Benz", "Porsche", "Honda", "Hyundai", "Canon", "Nikon", "Panasonic", 
        "LG", "Asus", "Lenovo", "Logitech", "Razer", "Patagonia", "Lululemon", "Sephora"
    )

    init {
        // Collect searchQuery and update suggestions list
        searchQuery
            .debounce(200)
            .distinctUntilChanged()
            .onEach { query ->
                updateSuggestions(query)
            }
            .launchIn(viewModelScope)
    }

    private fun updateSuggestions(query: String) {
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            return
        }
        val trimmed = query.trim().lowercase()
        val filteredBrands = popularBrands.filter { 
            it.lowercase().contains(trimmed) && !it.lowercase().equals(trimmed)
        }.take(5)

        _suggestions.value = filteredBrands
    }

    // --- Search Trigger ---
    fun searchCompany(companyName: String) {
        if (companyName.isBlank()) return
        searchQuery.value = companyName
        selectedCategory.value = null // reset category selection upon new company search

        viewModelScope.launch {
            repository.getCompanyProducts(companyName).collect { resource ->
                _searchResult.value = resource
            }
        }
    }

    fun forceRefresh() {
        val currentCompany = searchQuery.value
        if (currentCompany.isBlank()) return
        viewModelScope.launch {
            repository.getCompanyProducts(currentCompany, forceRefresh = true).collect { resource ->
                _searchResult.value = resource
            }
        }
    }

    // --- Category Extraction ---
    val categories: StateFlow<List<String>> = _searchResult.map { resource ->
        when (resource) {
            is Resource.Success -> {
                resource.data.products.map { it.category }.distinct().sorted()
            }
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Display (Filtered & Sorted) Products ---
    val filteredAndSortedProducts: StateFlow<List<ProductItem>> = combine(
        _searchResult,
        selectedCategory,
        selectedSortOption
    ) { resource, category, sortOption ->
        if (resource is Resource.Success) {
            var list = resource.data.products
            if (category != null) {
                list = list.filter { it.category == category }
            }
            when (sortOption) {
                SortOption.POPULARITY_DESC -> list.sortedByDescending { it.popularity }
                SortOption.PRICE_ASC -> list.sortedBy { it.priceNumeric }
                SortOption.PRICE_DESC -> list.sortedByDescending { it.priceNumeric }
                SortOption.NAME_ASC -> list.sortedBy { it.name }
            }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Favorites Actions ---
    fun toggleFavorite(product: ProductItem, companyName: String) {
        val compoundId = "${companyName}_${product.name}"
        viewModelScope.launch {
            if (repository.isFavoriteDirect(compoundId)) {
                repository.deleteFavorite(compoundId)
            } else {
                repository.addFavorite(
                    FavoriteProductEntity(
                        id = compoundId,
                        companyName = companyName,
                        name = product.name,
                        category = product.category,
                        description = product.description,
                        price = product.price,
                        imageUrl = product.imageUrl,
                        productUrl = product.productUrl,
                        rating = product.popularity
                    )
                )
            }
        }
    }

    fun removeFavoriteById(id: String) {
        viewModelScope.launch {
            repository.deleteFavorite(id)
        }
    }

    fun isFavoriteStream(productName: String, companyName: String): Flow<Boolean> {
        return repository.isFavorite("${companyName}_$productName")
    }

    // --- History Actions ---
    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            repository.clearRecentSearches()
        }
    }

    // --- ViewModel Factory ---
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(application)
                val repository = ProductFinderRepository(database.productFinderDao())
                return ProductFinderViewModel(application, repository) as T
            }
        }
    }
}
