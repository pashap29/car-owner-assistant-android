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

enum class DistanceUnit {
    KM,
    MI,
}

enum class MileageEntryOrigin {
    DEDICATED_MILEAGE,
    FUEL_CONTEXT,
    SERVICE_CONTEXT,
}

enum class MileageAnomalyType {
    REGRESSION,
    SAME_DAY_VARIANCE,
    SAME_DAY_CONFLICT,
    LARGE_JUMP_SUSPECTED,
    NEARBY_TOLERANCE_APPLIED,
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
    val origin: MileageEntryOrigin,
    val userEnteredValue: Double,
    val userEnteredUnit: DistanceUnit,
    val photoFilePath: String?,
    val manualEntry: Boolean,
    val criticalAnomaly: Boolean,
    val largeJumpSuspected: Boolean,
    val trustedReferenceEntryId: String?,
    val trustScore: Int,
    val anomalyTypes: List<MileageAnomalyType>,
)

data class MileageEntryDraft(
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val value: Double,
    val inputUnit: DistanceUnit,
    val photoFilePath: String?,
    val origin: MileageEntryOrigin = MileageEntryOrigin.DEDICATED_MILEAGE,
    val manualEntry: Boolean = true,
)

data class MileageLedgerSummary(
    val entries: List<MileageEntrySummary>,
    val anomalyLog: List<MileageAnomalyLogItem>,
    val overallTrustScore: Int,
)

data class MileageAnomalyLogItem(
    val mileageEntryId: String,
    val timestampEpochMillis: Long,
    val type: MileageAnomalyType,
    val message: String,
)

data class FuelEntrySummary(
    val id: String,
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val liters: Double,
    val totalAmount: Double,
    val isFullTank: Boolean,
)

enum class ExpenseCategory {
    MAINTENANCE,
    REPAIR,
    TIRES,
    INSURANCE,
    TAX,
    FINES,
    PARKING,
    WASHING,
    OTHER,
}

data class ExpenseEntrySummary(
    val id: String,
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val category: ExpenseCategory,
    val totalAmount: Double,
    val note: String,
    val attachmentPath: String?,
    val mileageReading: OdometerReading?,
)

data class ExpenseEntryDraft(
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val category: ExpenseCategory,
    val totalAmount: Double,
    val note: String,
    val attachmentPath: String?,
    val mileageValue: Double?,
    val mileageUnit: DistanceUnit?,
)

data class ExpenseFilter(
    val query: String = "",
    val category: ExpenseCategory? = null,
)
