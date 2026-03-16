package com.carownerassistant.core.model

enum class AppStartMode {
    ACTIVE_CAR,
    GARAGE,
}

enum class MileageStatus {
    VERIFIED,
    UNVERIFIED,
    CONFLICTED,
}

data class VehicleSummary(
    val id: String,
    val displayName: String,
    val make: String,
    val model: String,
    val year: Int?,
    val plateNumber: String,
    val isActive: Boolean,
)

data class VehicleDraft(
    val displayName: String,
    val make: String = "",
    val model: String = "",
    val year: Int? = null,
    val plateNumber: String = "",
    val makeActive: Boolean = false,
)

data class OdometerReading(
    val km: Double,
    val mi: Double,
)

data class MileageEntrySummary(
    val id: String,
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val reading: OdometerReading,
    val status: MileageStatus,
)

data class FuelEntrySummary(
    val id: String,
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val liters: Double,
    val totalAmount: Double,
    val isFullTank: Boolean,
)

data class ExpenseEntrySummary(
    val id: String,
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val category: String,
    val totalAmount: Double,
)
