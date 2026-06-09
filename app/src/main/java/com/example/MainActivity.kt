package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ProductFinderViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ProductFinderViewModel by viewModels {
        ProductFinderViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkThemeOverride by remember { mutableStateOf<Boolean?>(null) }
            val systemInDark = isSystemInDarkTheme()
            val isDark = darkThemeOverride ?: systemInDark

            MyApplicationTheme(darkTheme = isDark) {
                MainScreen(
                    viewModel = viewModel,
                    isDarkModeEnabled = isDark,
                    onToggleTheme = {
                        darkThemeOverride = !isDark
                    }
                )
            }
        }
    }
}
