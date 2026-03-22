package com.carownerassistant.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.carownerassistant.app.navigation.MainNavigation
import com.carownerassistant.app.ui.theme.CarOwnerAssistantTheme
import com.carownerassistant.core.model.AppStartMode
import com.carownerassistant.core.model.GarageLaunchRoute
import com.carownerassistant.core.model.GarageRules
import com.carownerassistant.core.navigation.AppRoutes
import kotlinx.coroutines.flow.first

@Composable
fun CarOwnerApp() {
    val appContainer = rememberAppContainer()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val vehicles = appContainer.vehicleRepository.observeVehicles().first()
        val appStartMode = appContainer.settingsRepository.appStartMode().first()
        startDestination = when (
            GarageRules.resolveLaunchRoute(
                vehicles = vehicles,
                appStartMode = appStartMode,
            )
        ) {
            GarageLaunchRoute.GARAGE -> AppRoutes.GARAGE
            GarageLaunchRoute.MAIN -> AppRoutes.FUEL
        }
    }

    CarOwnerAssistantTheme {
        if (startDestination == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Loading...")
            }
        } else {
            MainNavigation(
                startDestination = startDestination!!,
                vehicleRepository = appContainer.vehicleRepository,
                fuelRepository = appContainer.fuelRepository,
                expenseRepository = appContainer.expenseRepository,
                mileageRepository = appContainer.mileageRepository,
                settingsRepository = appContainer.settingsRepository,
                appFileStore = appContainer.fileStore,
            )
        }
    }
}
