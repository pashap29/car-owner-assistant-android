package com.carownerassistant.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_entry")
data class ServiceEntryEntity(
    @PrimaryKey val serviceEntryId: String,
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val title: String,
    val notes: String,
    val totalAmount: Double,
    val address: String?,
    val phone: String?,
    val contact: String?,
    val odometerKm: Double?,
    val odometerMi: Double?,
    val linkedMileageEntryId: String?,
)
