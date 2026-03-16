package com.carownerassistant.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GarageRulesTest {

    @Test
    fun `first vehicle becomes active automatically`() {
        val result = GarageRules.shouldNewVehicleBeActive(
            existingVehicles = emptyList(),
            makeActiveRequested = false,
        )

        assertTrue(result)
    }

    @Test
    fun `new vehicle can become active when requested`() {
        val result = GarageRules.shouldNewVehicleBeActive(
            existingVehicles = listOf(vehicle(id = "1", isActive = true)),
            makeActiveRequested = true,
        )

        assertTrue(result)
    }

    @Test
    fun `existing active vehicle remains active on edit without explicit change`() {
        val result = GarageRules.shouldUpdatedVehicleRemainActive(
            currentlyActive = true,
            makeActiveRequested = false,
        )

        assertTrue(result)
    }

    @Test
    fun `inactive vehicle stays inactive on edit unless explicitly activated`() {
        val result = GarageRules.shouldUpdatedVehicleRemainActive(
            currentlyActive = false,
            makeActiveRequested = false,
        )

        assertFalse(result)
    }

    @Test
    fun `deleting active vehicle promotes first remaining vehicle`() {
        val nextVehicleId = GarageRules.nextActiveVehicleIdAfterDelete(
            existingVehicles = listOf(
                vehicle(id = "active", isActive = true),
                vehicle(id = "next"),
                vehicle(id = "later"),
            ),
            deletedVehicleId = "active",
        )

        assertEquals("next", nextVehicleId)
    }

    @Test
    fun `deleting last vehicle leaves no active vehicle`() {
        val nextVehicleId = GarageRules.nextActiveVehicleIdAfterDelete(
            existingVehicles = listOf(vehicle(id = "only", isActive = true)),
            deletedVehicleId = "only",
        )

        assertNull(nextVehicleId)
    }

    @Test
    fun `launch route opens garage when there are no vehicles`() {
        val route = GarageRules.resolveLaunchRoute(
            vehicles = emptyList(),
            appStartMode = AppStartMode.ACTIVE_CAR,
        )

        assertEquals(GarageLaunchRoute.GARAGE, route)
    }

    @Test
    fun `launch route opens garage when garage mode is selected`() {
        val route = GarageRules.resolveLaunchRoute(
            vehicles = listOf(vehicle(id = "1", isActive = true)),
            appStartMode = AppStartMode.GARAGE,
        )

        assertEquals(GarageLaunchRoute.GARAGE, route)
    }

    @Test
    fun `launch route opens main shell when active car mode is selected and vehicles exist`() {
        val route = GarageRules.resolveLaunchRoute(
            vehicles = listOf(vehicle(id = "1", isActive = true)),
            appStartMode = AppStartMode.ACTIVE_CAR,
        )

        assertEquals(GarageLaunchRoute.MAIN, route)
    }

    private fun vehicle(
        id: String,
        isActive: Boolean = false,
    ): VehicleSummary {
        return VehicleSummary(
            id = id,
            displayName = "Car $id",
            make = "",
            model = "",
            year = null,
            plateNumber = "",
            isActive = isActive,
        )
    }
}
