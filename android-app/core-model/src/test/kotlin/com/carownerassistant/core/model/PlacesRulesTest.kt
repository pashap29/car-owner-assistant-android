package com.carownerassistant.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacesRulesTest {

    @Test
    fun `distance calculator returns positive value for nearby coordinates`() {
        val distance = PlacesRules.distanceKm(
            from = GeoPoint(55.7558, 37.6176),
            to = GeoPoint(55.7512, 37.6184),
        )

        assertTrue(distance > 0.0)
        assertTrue(distance < 1.0)
    }

    @Test
    fun `favorite state is applied by normalized id`() {
        val places = PlacesRules.applyFavoriteState(
            places = listOf(place(id = "a"), place(id = "b")),
            favoriteIds = setOf("b"),
        )

        assertEquals(listOf(false, true), places.map { it.isFavorite })
    }

    @Test
    fun `favorites are grouped by place type`() {
        val grouped = PlacesRules.groupFavoritesByType(
            listOf(
                place(id = "1", type = PlaceType.TIRE_SHOP, name = "Wheel A"),
                place(id = "2", type = PlaceType.GAS_STATION, name = "Fuel B"),
                place(id = "3", type = PlaceType.GAS_STATION, name = "Fuel A"),
            ),
        )

        assertEquals(listOf("Fuel A", "Fuel B"), grouped.getValue(PlaceType.GAS_STATION).map { it.displayName })
        assertEquals(listOf("Wheel A"), grouped.getValue(PlaceType.TIRE_SHOP).map { it.displayName })
    }

    private fun place(
        id: String,
        type: PlaceType = PlaceType.GAS_STATION,
        name: String = "Place $id",
    ): PlaceInfo {
        return PlaceInfo(
            ref = PlaceRef(
                providerKey = "sample",
                providerPlaceId = id,
                normalizedPlaceId = id,
            ),
            type = type,
            displayName = name,
            formattedAddress = "Address",
            coordinates = GeoPoint(55.75, 37.61),
            distanceKm = 1.0,
            phone = null,
            rating = 4.5,
            reviewCount = 12,
            normalizedTags = listOf("24/7"),
            isFavorite = false,
        )
    }
}
