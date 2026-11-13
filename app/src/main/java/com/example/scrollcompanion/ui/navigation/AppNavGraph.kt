package com.example.scrollcompanion.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.scrollcompanion.ScrollCompanionApp
import com.example.scrollcompanion.ui.screens.home.HomeScreen
import com.example.scrollcompanion.ui.screens.rediscover.RediscoverScreen
import com.example.scrollcompanion.ui.screens.settings.SettingsScreen
import com.example.scrollcompanion.ui.screens.vault.VaultDetailScreen
import com.example.scrollcompanion.ui.screens.vault.VaultScreen
import com.example.scrollcompanion.viewmodel.HomeViewModel
import com.example.scrollcompanion.viewmodel.RediscoverViewModel
import com.example.scrollcompanion.viewmodel.SettingsViewModel
import com.example.scrollcompanion.viewmodel.VaultDetailViewModel
import com.example.scrollcompanion.viewmodel.VaultViewModel

private const val VAULT_DETAIL_ROUTE = "vault_detail/{id}"

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
fun AppNavGraph(
    requestedRoute: String?,
    onRouteHandled: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as ScrollCompanionApp).container
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    LaunchedEffect(requestedRoute) {
        if (requestedRoute != null) {
            navController.navigateToTopLevel(requestedRoute)
            onRouteHandled()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true ||
                            (destination == TopLevelDestination.VAULT && currentDestination?.route == VAULT_DETAIL_ROUTE),
                        onClick = { navController.navigateToTopLevel(destination.route) },
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
                    onOpenVault = { navController.navigateToTopLevel(TopLevelDestination.VAULT.route) },
                    onItemClick = { id -> navController.navigate("vault_detail/$id") },
                )
            }

            composable(TopLevelDestination.VAULT.route) {
                val viewModel: VaultViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { VaultViewModel(container.contentRepository) }
                    },
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                VaultScreen(
                    uiState = uiState,
                    onItemClick = { id -> navController.navigate("vault_detail/$id") },
                    onCategorySelected = viewModel::selectCategory,
                    onQueryChanged = viewModel::onQueryChanged,
                )
            }

            composable(
                route = VAULT_DETAIL_ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val viewModel: VaultDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { VaultDetailViewModel(container.contentRepository, id) }
                    },
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val categories by viewModel.categories.collectAsStateWithLifecycle()
                val tagSuggestions by viewModel.tagSuggestions.collectAsStateWithLifecycle()
                VaultDetailScreen(
                    uiState = uiState,
                    categories = categories,
                    tagSuggestions = tagSuggestions,
                    onCreateCategory = viewModel::createCategory,
                    onBack = { navController.popBackStack() },
                    onSaveEdits = viewModel::saveEdits,
                    onDelete = { viewModel.delete { navController.popBackStack() } },
                )
            }

            composable(TopLevelDestination.REDISCOVER.route) {
                val viewModel: RediscoverViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { RediscoverViewModel(container.contentRepository) }
                    },
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                RediscoverScreen(
                    uiState = uiState,
                    onItemClick = { id -> navController.navigate("vault_detail/$id") },
                    onReviewed = viewModel::markReviewed,
                )
            }

            composable(TopLevelDestination.SETTINGS.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { SettingsViewModel(container.contentRepository) }
                    },
                )
                val categories by viewModel.categories.collectAsStateWithLifecycle()
                SettingsScreen(
                    categories = categories,
                    onAddCategory = viewModel::addCategory,
                    onRenameCategory = viewModel::renameCategory,
                    onDeleteCategory = viewModel::deleteCategory,
                )
            }
        }
    }
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
