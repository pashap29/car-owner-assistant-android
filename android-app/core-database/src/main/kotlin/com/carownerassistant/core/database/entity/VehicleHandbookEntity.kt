package com.carownerassistant.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle_handbook")
data class VehicleHandbookEntity(
    @PrimaryKey val vehicleId: String,
    val vin: String,
)
