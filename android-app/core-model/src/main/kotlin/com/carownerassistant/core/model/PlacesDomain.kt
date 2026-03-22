package com.carownerassistant.core.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object PlacesRules {
    fun distanceKm(
        from: GeoPoint,
        to: GeoPoint,
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(from.latitude)) *
            cos(Math.toRadians(to.latitude)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    fun applyFavoriteState(
        places: List<PlaceInfo>,
        favoriteIds: Set<String>,
    ): List<PlaceInfo> {
        return places.map { place ->
            place.copy(isFavorite = place.ref.normalizedPlaceId in favoriteIds)
        }
    }

    fun groupFavoritesByType(
        favorites: List<PlaceInfo>,
    ): Map<PlaceType, List<PlaceInfo>> {
        return favorites
            .sortedWith(
                compareBy<PlaceInfo> { it.type.name }
                    .thenBy { it.displayName.lowercase() },
            )
            .groupBy { it.type }
    }
}
