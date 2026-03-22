package com.carownerassistant.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "place_favorite")
data class PlaceFavoriteEntity(
    @PrimaryKey val normalizedPlaceId: String,
    val providerKey: String,
    val providerPlaceId: String,
    val placeType: String,
    val displayName: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val rating: Double?,
    val reviewCount: Int?,
    val tagsCsv: String,
)
