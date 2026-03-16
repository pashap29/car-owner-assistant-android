package com.carownerassistant.core.model

object GarageRules {
    fun shouldNewVehicleBeActive(
        existingVehicles: List<VehicleSummary>,
        makeActiveRequested: Boolean,
    ): Boolean {
        return existingVehicles.isEmpty() || makeActiveRequested
    }

    fun shouldUpdatedVehicleRemainActive(
        currentlyActive: Boolean,
        makeActiveRequested: Boolean,
    ): Boolean {
        return currentlyActive || makeActiveRequested
    }

    fun nextActiveVehicleIdAfterDelete(
        existingVehicles: List<VehicleSummary>,
        deletedVehicleId: String,
    ): String? {
        return existingVehicles
            .filterNot { it.id == deletedVehicleId }
            .firstOrNull()
            ?.id
    }

    fun resolveLaunchRoute(
        vehicles: List<VehicleSummary>,
        appStartMode: AppStartMode,
    ): GarageLaunchRoute {
        return if (vehicles.isEmpty() || appStartMode == AppStartMode.GARAGE) {
            GarageLaunchRoute.GARAGE
        } else {
            GarageLaunchRoute.MAIN
        }
    }
}

enum class GarageLaunchRoute {
    GARAGE,
    MAIN,
}
