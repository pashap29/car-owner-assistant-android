package com.carownerassistant.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mileage_entry")
data class MileageEntryEntity(
    @PrimaryKey val mileageEntryId: String,
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val odometerKm: Double,
    val odometerMi: Double,
    val status: String,
    val criticalAnomaly: Boolean,
)
