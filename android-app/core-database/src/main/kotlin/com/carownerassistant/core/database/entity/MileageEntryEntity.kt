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
    val origin: String,
    val userEnteredValue: Double,
    val userEnteredUnit: String,
    val photoFilePath: String?,
    val manualEntry: Boolean,
    val status: String,
    val criticalAnomaly: Boolean,
    val largeJumpSuspected: Boolean,
    val trustedReferenceMileageEntryId: String?,
    val trustScore: Int,
    val anomalyFlagsCsv: String,
)
