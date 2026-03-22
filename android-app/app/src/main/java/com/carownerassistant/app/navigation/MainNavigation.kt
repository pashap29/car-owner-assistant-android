package com.carownerassistant.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.carownerassistant.core.files.AppFileStore
import com.carownerassistant.core.navigation.AppRoutes
import com.carownerassistant.core.navigation.BottomTabRoutes
import com.carownerassistant.core.model.repository.ExpenseRepository
import com.carownerassistant.core.model.repository.FuelRepository
import com.carownerassistant.core.model.repository.HandbookRepository
import com.carownerassistant.core.model.repository.MileageRepository
import com.carownerassistant.core.model.repository.ServiceRepository
import com.carownerassistant.core.model.repository.SettingsRepository
import com.carownerassistant.core.model.repository.VehicleRepository
import com.carownerassistant.feature.expense.ExpenseTabScreen
import com.carownerassistant.feature.fuel.FuelTabScreen
import com.carownerassistant.feature.mileage.MileageRoute
import com.carownerassistant.feature.service.ServiceTabScreen
import com.carownerassistant.feature.settings.SettingsTabScreen
import com.carownerassistant.feature.statistics.StatisticsTabScreen
import com.carownerassistant.feature.vehicle.GarageRoute
import com.carownerassistant.feature.vehicle.VehicleHandbookRoute

data class BottomTab(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

private val tabs = listOf(
    BottomTab(AppRoutes.FUEL, "Fuel") { Icon(Icons.Filled.LocalGasStation, contentDescription = "Fuel") },
    BottomTab(AppRoutes.EXPENSE, "Expense") { Icon(Icons.Filled.Payments, contentDescription = "Expense") },
    BottomTab(AppRoutes.SERVICE, "Service") { Icon(Icons.Filled.Build, contentDescription = "Service") },
    BottomTab(AppRoutes.STATISTICS, "Statistics") { Icon(Icons.Filled.Analytics, contentDescription = "Statistics") },
    BottomTab(AppRoutes.SETTINGS, "Settings") { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
)

@Composable
fun MainNavigation(
    startDestination: String,
    vehicleRepository: VehicleRepository,
    fuelRepository: FuelRepository,
    handbookRepository: HandbookRepository,
    expenseRepository: ExpenseRepository,
    mileageRepository: MileageRepository,
    serviceRepository: ServiceRepository,
    settingsRepository: SettingsRepository,
    appFileStore: AppFileStore,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route in BottomTabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = tab.icon,
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppRoutes.GARAGE) {
                GarageRoute(
                    vehicleRepository = vehicleRepository,
                    settingsRepository = settingsRepository,
                    launchedAsStartDestination = startDestination == AppRoutes.GARAGE,
                    onContinueToMain = {
                        navController.navigate(AppRoutes.FUEL) {
                            popUpTo(AppRoutes.GARAGE) {
                                inclusive = true
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.VEHICLE_HANDBOOK) {
                VehicleHandbookRoute(
                    vehicleRepository = vehicleRepository,
                    handbookRepository = handbookRepository,
                    appFileStore = appFileStore,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.MILEAGE_LEDGER) {
                MileageRoute(
                    vehicleRepository = vehicleRepository,
                    mileageRepository = mileageRepository,
                    appFileStore = appFileStore,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.FUEL) {
                FuelTabScreen(
                    vehicleRepository = vehicleRepository,
                    fuelRepository = fuelRepository,
                    mileageRepository = mileageRepository,
                    onOpenMileage = { navController.navigate(AppRoutes.MILEAGE_LEDGER) },
                )
            }
            composable(AppRoutes.EXPENSE) {
                ExpenseTabScreen(
                    vehicleRepository = vehicleRepository,
                    expenseRepository = expenseRepository,
                    appFileStore = appFileStore,
                )
            }
            composable(AppRoutes.SERVICE) {
                ServiceTabScreen(
                    vehicleRepository = vehicleRepository,
                    serviceRepository = serviceRepository,
                    onOpenMileage = { navController.navigate(AppRoutes.MILEAGE_LEDGER) },
                )
            }
            composable(AppRoutes.STATISTICS) {
                StatisticsTabScreen(
                    onOpenMileage = { navController.navigate(AppRoutes.MILEAGE_LEDGER) },
                )
            }
            composable(AppRoutes.SETTINGS) {
                SettingsTabScreen(
                    onOpenGarage = { navController.navigate(AppRoutes.GARAGE) },
                    onOpenHandbook = { navController.navigate(AppRoutes.VEHICLE_HANDBOOK) },
                )
            }
        }
    }
}
