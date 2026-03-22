package com.carownerassistant.app.places

import com.carownerassistant.core.model.GeoPoint
import com.carownerassistant.core.model.PlaceInfo
import com.carownerassistant.core.model.PlaceRef
import com.carownerassistant.core.model.PlaceSearchCriteria
import com.carownerassistant.core.model.PlaceType
import com.carownerassistant.core.model.PlacesRules
import com.carownerassistant.core.model.repository.PlaceDetailsGateway
import com.carownerassistant.core.model.repository.PlaceGatewayResult
import com.carownerassistant.core.model.repository.PlaceSearchGateway

class SamplePlacesGateway : PlaceSearchGateway, PlaceDetailsGateway {
    private val places = listOf(
        samplePlace(
            normalizedId = "gas_shell_tverskaya",
            providerPlaceId = "shell_tverskaya",
            type = PlaceType.GAS_STATION,
            name = "Shell Tverskaya",
            address = "Tverskaya St, Moscow",
            latitude = 55.7608,
            longitude = 37.6057,
            phone = "+74950001010",
            rating = 4.4,
            reviewCount = 218,
            tags = listOf("24_7", "coffee", "air_pump"),
        ),
        samplePlace(
            normalizedId = "gas_lukoil_leninsky",
            providerPlaceId = "lukoil_leninsky",
            type = PlaceType.GAS_STATION,
            name = "Lukoil Leninsky",
            address = "Leninsky Ave, Moscow",
            latitude = 55.7094,
            longitude = 37.5868,
            phone = "+74950002020",
            rating = 4.1,
            reviewCount = 142,
            tags = listOf("car_wash", "shop"),
        ),
        samplePlace(
            normalizedId = "service_nord_auto",
            providerPlaceId = "nord_auto",
            type = PlaceType.SERVICE_CENTER,
            name = "Nord Auto Service",
            address = "Prospekt Mira, Moscow",
            latitude = 55.7922,
            longitude = 37.6366,
            phone = "+74950003030",
            rating = 4.7,
            reviewCount = 95,
            tags = listOf("diagnostics", "oil_change"),
        ),
        samplePlace(
            normalizedId = "service_garage_one",
            providerPlaceId = "garage_one",
            type = PlaceType.SERVICE_CENTER,
            name = "Garage One",
            address = "Kutuzovsky Ave, Moscow",
            latitude = 55.7419,
            longitude = 37.5375,
            phone = "+74950004040",
            rating = 4.3,
            reviewCount = 167,
            tags = listOf("suspension", "electrics"),
        ),
        samplePlace(
            normalizedId = "tire_black_round",
            providerPlaceId = "black_round",
            type = PlaceType.TIRE_SHOP,
            name = "Black Round Tires",
            address = "Varshavskoye Hwy, Moscow",
            latitude = 55.6547,
            longitude = 37.6208,
            phone = "+74950005050",
            rating = 4.6,
            reviewCount = 84,
            tags = listOf("tire_storage", "alignment"),
        ),
        samplePlace(
            normalizedId = "tire_pitstop_center",
            providerPlaceId = "pitstop_center",
            type = PlaceType.TIRE_SHOP,
            name = "PitStop Tire Center",
            address = "Leningradsky Ave, Moscow",
            latitude = 55.8001,
            longitude = 37.5312,
            phone = "+74950006060",
            rating = 4.2,
            reviewCount = 132,
            tags = listOf("runflat", "seasonal_swap"),
        ),
    )

    override suspend fun search(criteria: PlaceSearchCriteria): PlaceGatewayResult<List<PlaceInfo>> {
        if (criteria.radiusKm <= 0) {
            return PlaceGatewayResult.InvalidRequest("Radius must be greater than zero.")
        }
        if (criteria.types.isEmpty()) {
            return PlaceGatewayResult.InvalidRequest("Choose at least one place type.")
        }

        val matches = places
            .asSequence()
            .filter { it.type in criteria.types }
            .map { place ->
                val distanceKm = PlacesRules.distanceKm(criteria.center, place.coordinates)
                place.copy(distanceKm = distanceKm)
            }
            .filter { (it.distanceKm ?: Double.MAX_VALUE) <= criteria.radiusKm }
            .sortedWith(compareBy<PlaceInfo> { it.distanceKm ?: Double.MAX_VALUE }.thenBy { it.displayName })
            .toList()

        return PlaceGatewayResult.Success(matches)
    }

    override suspend fun getDetails(placeRef: PlaceRef): PlaceGatewayResult<PlaceInfo> {
        val place = places.firstOrNull { it.ref.normalizedPlaceId == placeRef.normalizedPlaceId }
            ?: return PlaceGatewayResult.InvalidRequest("Place details were not found for ${placeRef.normalizedPlaceId}.")
        return PlaceGatewayResult.Success(place)
    }

    private fun samplePlace(
        normalizedId: String,
        providerPlaceId: String,
        type: PlaceType,
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        phone: String,
        rating: Double,
        reviewCount: Int,
        tags: List<String>,
    ): PlaceInfo {
        return PlaceInfo(
            ref = PlaceRef(
                providerKey = "sample",
                providerPlaceId = providerPlaceId,
                normalizedPlaceId = normalizedId,
            ),
            type = type,
            displayName = name,
            formattedAddress = address,
            coordinates = GeoPoint(latitude = latitude, longitude = longitude),
            distanceKm = null,
            phone = phone,
            rating = rating,
            reviewCount = reviewCount,
            normalizedTags = tags,
            isFavorite = false,
        )
    }
}
