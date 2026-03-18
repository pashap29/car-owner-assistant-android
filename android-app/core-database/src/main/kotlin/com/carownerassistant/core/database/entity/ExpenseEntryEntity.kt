package com.carownerassistant.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_entry")
data class ExpenseEntryEntity(
    @PrimaryKey val expenseEntryId: String,
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val category: String,
    val totalAmount: Double,
    val note: String,
    val attachmentPath: String?,
    val odometerKm: Double?,
    val odometerMi: Double?,
)
