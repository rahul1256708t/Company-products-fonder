package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CompanyProductsResponse
import com.example.data.model.FavoriteProductEntity
import com.example.data.model.ProductItem
import com.example.data.repository.Resource
import com.example.ui.viewmodel.ProductFinderViewModel
import com.example.ui.viewmodel.SortOption
import kotlinx.coroutines.flow.Flow
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ProductFinderViewModel,
    isDarkModeEnabled: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteProducts.collectAsStateWithLifecycle()
    val searchResultState by viewModel.searchResult.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredAndSortedProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSortOption by viewModel.selectedSortOption.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Search, 1: Favorites
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedProductForDetail by remember { mutableStateOf<ProductItem?>(null) }
    var enteredQueryText by remember { mutableStateOf("") }

    // Synchronize initial input text if search query shifts
    LaunchedEffect(searchQuery) {
        if (enteredQueryText != searchQuery) {
            enteredQueryText = searchQuery
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val successState = searchResultState as? Resource.Success
                        val logoUrl = successState?.data?.companyLogoUrl
                        val companyName = successState?.data?.companyName

                        if (successState != null && activeTab == 0) {
                            var isLogoError by remember(logoUrl) { mutableStateOf(logoUrl.isNullOrEmpty()) }
                            
                            if (!isLogoError && !logoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = "$companyName Logo",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(4.dp),
                                    contentScale = ContentScale.Fit,
                                    onError = { isLogoError = true }
                                )
                            } else {
                                val brandNameChar = companyName?.firstOrNull()?.toString()?.uppercase() ?: "C"
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = brandNameChar,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = "Explore logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Text(
                            text = if (successState != null && activeTab == 0) (companyName ?: "Profile") else "Product Finder",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkModeEnabled) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                NavigationBar(
                    containerColor = if (isDarkModeEnabled) MaterialTheme.colorScheme.surface else Color(0xFFF3F4F9),
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(80.dp)
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Default.TravelExplore, contentDescription = "Search") },
                        label = { Text("Explore", fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("tab_explore"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorites", fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("tab_favorites"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeTab == 0) {
                // --- EXPLORE TAB ---
                
                // 1. Search Box Component
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TextField(
                        value = enteredQueryText,
                        onValueChange = {
                            enteredQueryText = it
                            viewModel.searchQuery.value = it
                        },
                        placeholder = { Text("Search companies... (e.g. Apple, Tesla, Nike)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                        trailingIcon = {
                            if (enteredQueryText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        enteredQueryText = ""
                                        viewModel.searchQuery.value = ""
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search,
                            capitalization = KeyboardCapitalization.Words
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (enteredQueryText.isNotBlank()) {
                                    viewModel.searchCompany(enteredQueryText)
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("search_input"),
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if (isDarkModeEnabled) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEFF1F8),
                            unfocusedContainerColor = if (isDarkModeEnabled) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEFF1F8),
                            disabledContainerColor = if (isDarkModeEnabled) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEFF1F8),
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )

                    // Helper inline trends matching HTML design
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf("iPhone 15", "MacBook Pro", "AirPods")) { trend ->
                            val isFirst = trend == "iPhone 15"
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isFirst) MaterialTheme.colorScheme.primaryContainer else if (isDarkModeEnabled) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3F4F9),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        enteredQueryText = "Apple"
                                        viewModel.searchCompany("Apple")
                                        keyboardController?.hide()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = trend,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isFirst) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Autocomplete Suggestions popup list
                    AnimatedVisibility(
                        visible = suggestions.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            shadowElevation = 8.dp
                        ) {
                            Column {
                                suggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                enteredQueryText = suggestion
                                                viewModel.searchCompany(suggestion)
                                                keyboardController?.hide()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .testTag("suggestion_item_$suggestion"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                }

                // 2. Recent Searches Row
                if (recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { viewModel.clearRecentSearches() }
                                .testTag("clear_history_button")
                        )
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentSearches) { searchItem ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    enteredQueryText = searchItem.query
                                    viewModel.searchCompany(searchItem.query)
                                    keyboardController?.hide()
                                },
                                label = { Text(searchItem.query) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove from history",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.removeRecentSearch(searchItem.query) }
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                // 3. Core content states based on searchResultState
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (val state = searchResultState) {
                        null -> {
                            // Onboarding/Welcome State
                            WelcomeOnboardingView(
                                onSuggestionClick = { name ->
                                    enteredQueryText = name
                                    viewModel.searchCompany(name)
                                }
                            )
                        }
                        is Resource.Loading -> {
                            LoadingView()
                        }
                        is Resource.Error -> {
                            ErrorStateView(
                                message = state.message,
                                onRetry = { viewModel.forceRefresh() }
                            )
                        }
                        is Resource.Success -> {
                            SuccessProductsView(
                                companyData = state.data,
                                categories = categories,
                                selectedCategory = selectedCategory,
                                onSelectCategory = { viewModel.selectedCategory.value = it },
                                selectedSortOption = selectedSortOption,
                                onSelectSort = { viewModel.selectedSortOption.value = it },
                                displayedProducts = filteredProducts,
                                onToggleFavorite = { product -> viewModel.toggleFavorite(product, state.data.companyName) },
                                onProductClick = { selectedProductForDetail = it },
                                isFavoriteCheck = { product -> viewModel.isFavoriteStream(product.name, state.data.companyName) },
                                showSortMenu = showSortMenu,
                                onToggleSortMenu = { showSortMenu = it },
                                isDarkModeEnabled = isDarkModeEnabled
                            )
                        }
                    }
                }

            } else {
                // --- FAVORITES TAB ---
                FavoritesTabView(
                    favorites = favorites,
                    onRemoveFavorite = { viewModel.removeFavoriteById(it.id) },
                    onProductClick = { fav ->
                        selectedProductForDetail = ProductItem(
                            name = fav.name,
                            category = fav.category,
                            description = fav.description,
                            price = fav.price,
                            priceNumeric = 0.0,
                            productUrl = fav.productUrl,
                            imageUrl = fav.imageUrl,
                            imageSearchQuery = "",
                            popularity = fav.rating
                        )
                    }
                )
            }
        }
    }

    // --- IMMERSIVE DIALOG / DETAIL SHEET ---
    selectedProductForDetail?.let { product ->
        val currentCompany = if (activeTab == 0) {
            (searchResultState as? Resource.Success)?.data?.companyName ?: "Company"
        } else {
            favorites.firstOrNull { it.name == product.name }?.companyName ?: "Company"
        }

        ProductDetailDialog(
            companyName = currentCompany,
            product = product,
            onDismiss = { selectedProductForDetail = null },
            onLaunchLink = { url ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open external link.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

// --- ONBOARDING STUNNING VIEW ---
@Composable
fun WelcomeOnboardingView(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Company Product Finder",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Instantly explore core metrics and premium product catalogs for any global company with AI assistance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Try these world-famous brands:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        val promptSuggestions = listOf("Apple", "Tesla", "Sony", "Starbucks", "Nike")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(promptSuggestions) { brand ->
                Button(
                    onClick = { onSuggestionClick(brand) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(text = brand, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// --- SPINNER LOADING VIEW ---
@Composable
fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.9.dp))
        Text(
            text = "Scraping & compiling official catalogs...",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "This takes a few seconds via Gemini 3.5 AI Engine",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

// --- FRIENDLY ERROR STATE VIEW ---
@Composable
fun ErrorStateView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Company Profile Not Found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry Fetch")
        }
    }
}

// --- SUCCESS COMPANY PRODUCT PORTFOLIO SCREEN ---
@Composable
fun SuccessProductsView(
    companyData: CompanyProductsResponse,
    categories: List<String>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    selectedSortOption: SortOption,
    onSelectSort: (SortOption) -> Unit,
    displayedProducts: List<ProductItem>,
    onToggleFavorite: (ProductItem) -> Unit,
    onProductClick: (ProductItem) -> Unit,
    isFavoriteCheck: @Composable (ProductItem) -> Flow<Boolean>,
    showSortMenu: Boolean,
    onToggleSortMenu: (Boolean) -> Unit,
    isDarkModeEnabled: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        
        // 1. Company Logo Banner and info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var isLogoError by remember(companyData.companyLogoUrl) { mutableStateOf(companyData.companyLogoUrl.isNullOrEmpty()) }

                if (!isLogoError && !companyData.companyLogoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = companyData.companyLogoUrl,
                        contentDescription = "Brand visual logo",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(6.dp),
                        contentScale = ContentScale.Fit,
                        onError = { isLogoError = true }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = companyData.companyName.take(2).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = companyData.companyName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Est. ${companyData.companyFounded}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = companyData.companyDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2. Filter list by Category and Sorting controllers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Products (${displayedProducts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Box {
                IconButton(onClick = { onToggleSortMenu(true) }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Sort Products",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { onToggleSortMenu(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sort by Popularity (Descending)") },
                        onClick = {
                            onSelectSort(SortOption.POPULARITY_DESC)
                            onToggleSortMenu(false)
                        },
                        leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Price: Low to High") },
                        onClick = {
                            onSelectSort(SortOption.PRICE_ASC)
                            onToggleSortMenu(false)
                        },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Price: High to Low") },
                        onClick = {
                            onSelectSort(SortOption.PRICE_DESC)
                            onToggleSortMenu(false)
                        },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Name: A to Z") },
                        onClick = {
                            onSelectSort(SortOption.NAME_ASC)
                            onToggleSortMenu(false)
                        },
                        leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) }
                    )
                }
            }
        }

        // 3. Horizontal category pills list
        if (categories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("All") },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = if (isDarkModeEnabled) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3F4F9),
                            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == null,
                            borderColor = if (isDarkModeEnabled) Color.Transparent else Color(0xFFDDE2EB),
                            selectedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("category_chip_all")
                    )
                }
                items(categories) { categoryName ->
                    FilterChip(
                        selected = selectedCategory == categoryName,
                        onClick = { onSelectCategory(categoryName) },
                        label = { Text(categoryName) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = if (isDarkModeEnabled) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3F4F9),
                            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == categoryName,
                            borderColor = if (isDarkModeEnabled) Color.Transparent else Color(0xFFDDE2EB),
                            selectedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("category_chip_$categoryName")
                    )
                }
            }
        }

        // 4. Dual Grid list layout depending on size
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (displayedProducts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No products found for this category.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedProducts) { product ->
                        ProductItemCard(
                            product = product,
                            onToggleFavorite = { onToggleFavorite(product) },
                            onProductClick = { onProductClick(product) },
                            isFavorite = isFavoriteCheck(product).collectAsState(initial = false).value,
                            isDarkModeEnabled = isDarkModeEnabled
                        )
                    }
                }
            }
        }
    }
}

// --- DETAILED PRODUCT CARD COMPONENT ---
@Composable
fun ProductItemCard(
    product: ProductItem,
    onToggleFavorite: () -> Unit,
    onProductClick: () -> Unit,
    isFavorite: Boolean,
    isDarkModeEnabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductClick() }
            .testTag("product_card_${product.name}"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left block - Product image
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                ProductImageComponent(
                    imageUrl = product.imageUrl,
                    fallbackQuery = product.imageSearchQuery,
                    productName = product.name,
                    category = product.category,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Right block - Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("favorite_toggle_${product.name}")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Toggle favorite",
                                tint = if (isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = product.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.price,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    TextButton(
                        onClick = onProductClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "View Site",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// --- COIL / GENERATED FALLBACK COMPONENT ---
@Composable
fun ProductImageComponent(
    imageUrl: String?,
    fallbackQuery: String,
    productName: String,
    category: String,
    modifier: Modifier = Modifier
) {
    var isError by remember(imageUrl) { mutableStateOf(imageUrl.isNullOrEmpty()) }
    if (!isError && !imageUrl.isNullOrEmpty()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Image representation of $productName",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onError = { isError = true }
        )
    } else {
        ProductImageFallback(productName, category, modifier)
    }
}

@Composable
fun ProductImageFallback(productName: String, category: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = when {
                    category.lowercase().contains("phone") || category.lowercase().contains("mobile") -> Icons.Default.PhoneAndroid
                    category.lowercase().contains("laptop") || category.lowercase().contains("computer") -> Icons.Default.Laptop
                    category.lowercase().contains("wearable") || category.lowercase().contains("accessory") || category.lowercase().contains("watch") -> Icons.Default.Watch
                    category.lowercase().contains("audio") || category.lowercase().contains("headphone") || category.lowercase().contains("sound") -> Icons.Default.Headphones
                    category.lowercase().contains("software") || category.lowercase().contains("cloud") || category.lowercase().contains("web") -> Icons.Default.Cloud
                    category.lowercase().contains("coffee") || category.lowercase().contains("food") || category.lowercase().contains("drink") -> Icons.Default.Coffee
                    category.lowercase().contains("clothing") || category.lowercase().contains("shoe") || category.lowercase().contains("wear") -> Icons.Default.Checkroom
                    else -> Icons.Default.ShoppingBag
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = productName.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// --- PERSISTENT FAVORITES TAB SCREEN ---
@Composable
fun FavoritesTabView(
    favorites: List<FavoriteProductEntity>,
    onRemoveFavorite: (FavoriteProductEntity) -> Unit,
    onProductClick: (FavoriteProductEntity) -> Unit
) {
    if (favorites.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No saved favorites yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap the star button on any product in explore to add them to your persistent favorite tray.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Favorite Items (${favorites.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favorites, key = { it.id }) { favItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProductClick(favItem) }
                            .testTag("favorite_card_${favItem.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            // Product Coil visual
                            ProductImageComponent(
                                imageUrl = favItem.imageUrl,
                                fallbackQuery = "",
                                productName = favItem.name,
                                category = favItem.category,
                                modifier = Modifier
                                    .width(100.dp)
                                    .fillMaxHeight()
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = favItem.companyName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = favItem.price,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = favItem.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = favItem.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            // Remove favorite
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .wrapContentWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = { onRemoveFavorite(favItem) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        tint = MaterialTheme.colorScheme.error,
                                        contentDescription = "Remove from favorites"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- DIALOG DETAIL COMPONENT ---
@Composable
fun ProductDetailDialog(
    companyName: String,
    product: ProductItem,
    onDismiss: () -> Unit,
    onLaunchLink: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Main product picture banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    ProductImageComponent(
                        imageUrl = product.imageUrl,
                        fallbackQuery = product.imageSearchQuery,
                        productName = product.name,
                        category = product.category,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Close button upper-left
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.White.copy(alpha = 0.8f), shape = CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close detail",
                            tint = Color.DarkGray
                        )
                    }
                }

                // Scrollable product specifications and pricing controls
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "$companyName • ${product.category}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Rating and Price
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${product.popularity} Rating",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = product.price,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Button action to open link
                    Button(
                        onClick = { onLaunchLink(product.productUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Launch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Visit Official Webpage",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
