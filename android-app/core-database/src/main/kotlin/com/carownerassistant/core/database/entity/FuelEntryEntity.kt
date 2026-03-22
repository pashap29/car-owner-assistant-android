package com.carownerassistant.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_entry")
data class FuelEntryEntity(
    @PrimaryKey val fuelEntryId: String,
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val liters: Double,
    val totalAmount: Double,
    val isFullTank: Boolean,
    val fuelType: String,
    val entryMethod: String,
    val odometerKm: Double?,
    val odometerMi: Double?,
    val mileageDeltaKm: Double?,
    val qrPayloadRaw: String?,
)
