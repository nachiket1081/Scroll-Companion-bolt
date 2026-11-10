package com.example.scrollcompanion.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.scrollcompanion.ScrollCompanionApp
import com.example.scrollcompanion.ui.screens.home.HomeScreen
import com.example.scrollcompanion.ui.screens.rediscover.RediscoverScreen
import com.example.scrollcompanion.ui.screens.settings.SettingsScreen
import com.example.scrollcompanion.ui.screens.vault.VaultScreen
import com.example.scrollcompanion.viewmodel.HomeViewModel
import com.example.scrollcompanion.viewmodel.VaultViewModel

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Filled.Home),
    VAULT("vault", "Vault", Icons.Filled.Lock),
    REDISCOVER("rediscover", "Rediscover", Icons.Filled.Refresh),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

@Composable
fun AppNavGraph() {
    val container = (LocalContext.current.applicationContext as ScrollCompanionApp).container
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.HOME.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { HomeViewModel(container.contentRepository) }
                    },
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    onOpenVault = {
                        navController.navigate(TopLevelDestination.VAULT.route) { launchSingleTop = true }
                    },
                )
            }

            composable(TopLevelDestination.VAULT.route) {
                val viewModel: VaultViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { VaultViewModel(container.contentRepository) }
                    },
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                VaultScreen(uiState = uiState)
            }

            composable(TopLevelDestination.REDISCOVER.route) {
                RediscoverScreen()
            }

            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen()
            }
        }
    }
}
