package com.carownerassistant.core.database.repository

import com.carownerassistant.core.database.AppDatabase
import com.carownerassistant.core.database.entity.PlaceFavoriteEntity
import com.carownerassistant.core.model.GeoPoint
import com.carownerassistant.core.model.PlaceInfo
import com.carownerassistant.core.model.PlaceRef
import com.carownerassistant.core.model.PlaceSearchCriteria
import com.carownerassistant.core.model.PlaceType
import com.carownerassistant.core.model.PlacesRules
import com.carownerassistant.core.model.repository.PlaceDetailsGateway
import com.carownerassistant.core.model.repository.PlaceGatewayResult
import com.carownerassistant.core.model.repository.PlaceSearchGateway
import com.carownerassistant.core.model.repository.PlacesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPlacesRepository(
    database: AppDatabase,
    private val placeSearchGateway: PlaceSearchGateway,
    private val placeDetailsGateway: PlaceDetailsGateway,
) : PlacesRepository {

    private val favoriteDao = database.placeFavoriteDao()

    override fun observeFavorites(): Flow<List<PlaceInfo>> {
        return favoriteDao.observeAll().map { favorites ->
            favorites.map { it.toPlaceInfo(isFavorite = true) }
        }
    }

    override suspend fun searchPlaces(criteria: PlaceSearchCriteria): PlaceGatewayResult<List<PlaceInfo>> {
        val favoriteIds = favoriteDao.getFavoriteIds().toSet()
        return when (val result = placeSearchGateway.search(criteria)) {
            is PlaceGatewayResult.Success -> {
                PlaceGatewayResult.Success(
                    PlacesRules.applyFavoriteState(
                        places = result.value,
                        favoriteIds = favoriteIds,
                    ),
                )
            }

            PlaceGatewayResult.NoNetwork -> PlaceGatewayResult.NoNetwork
            PlaceGatewayResult.PermissionDenied -> PlaceGatewayResult.PermissionDenied
            PlaceGatewayResult.ProviderUnavailable -> PlaceGatewayResult.ProviderUnavailable
            is PlaceGatewayResult.InvalidRequest -> result
        }
    }

    override suspend fun getPlaceDetails(placeRef: PlaceRef): PlaceGatewayResult<PlaceInfo> {
        val favoriteIds = favoriteDao.getFavoriteIds().toSet()
        return when (val result = placeDetailsGateway.getDetails(placeRef)) {
            is PlaceGatewayResult.Success -> {
                PlaceGatewayResult.Success(
                    PlacesRules.applyFavoriteState(
                        places = listOf(result.value),
                        favoriteIds = favoriteIds,
                    ).first(),
                )
            }

            PlaceGatewayResult.NoNetwork -> PlaceGatewayResult.NoNetwork
            PlaceGatewayResult.PermissionDenied -> PlaceGatewayResult.PermissionDenied
            PlaceGatewayResult.ProviderUnavailable -> PlaceGatewayResult.ProviderUnavailable
            is PlaceGatewayResult.InvalidRequest -> result
        }
    }

    override suspend fun addFavorite(place: PlaceInfo) {
        favoriteDao.upsert(
            PlaceFavoriteEntity(
                normalizedPlaceId = place.ref.normalizedPlaceId,
                providerKey = place.ref.providerKey,
                providerPlaceId = place.ref.providerPlaceId,
                placeType = place.type.name,
                displayName = place.displayName,
                formattedAddress = place.formattedAddress,
                latitude = place.coordinates.latitude,
                longitude = place.coordinates.longitude,
                phone = place.phone,
                rating = place.rating,
                reviewCount = place.reviewCount,
                tagsCsv = place.normalizedTags.joinToString(","),
            ),
        )
    }

    override suspend fun removeFavorite(normalizedPlaceId: String) {
        favoriteDao.deleteById(normalizedPlaceId)
    }

    private fun PlaceFavoriteEntity.toPlaceInfo(
        isFavorite: Boolean,
    ): PlaceInfo {
        return PlaceInfo(
            ref = PlaceRef(
                providerKey = providerKey,
                providerPlaceId = providerPlaceId,
                normalizedPlaceId = normalizedPlaceId,
            ),
            type = enumValueOf<PlaceType>(placeType),
            displayName = displayName,
            formattedAddress = formattedAddress,
            coordinates = GeoPoint(
                latitude = latitude,
                longitude = longitude,
            ),
            distanceKm = null,
            phone = phone,
            rating = rating,
            reviewCount = reviewCount,
            normalizedTags = tagsCsv.split(',').filter { it.isNotBlank() },
            isFavorite = isFavorite,
        )
    }
}
