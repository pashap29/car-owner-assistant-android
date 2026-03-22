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
    val fuelType: FuelType,
    val entryMethod: FuelEntryMethod,
    val odometerReading: OdometerReading?,
    val mileageDeltaKm: Double?,
    val qrPayloadRaw: String?,
)

enum class FuelType {
    GASOLINE_92,
    GASOLINE_95,
    GASOLINE_98,
    DIESEL,
    LPG,
    CNG,
    OTHER,
}

enum class FuelEntryMethod {
    MANUAL,
    QR_ASSISTED,
}

data class FuelEntryDraft(
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val liters: Double,
    val totalAmount: Double,
    val fuelType: FuelType,
    val isFullTank: Boolean,
    val odometerValue: Double?,
    val odometerUnit: DistanceUnit?,
    val entryMethod: FuelEntryMethod,
    val qrPayloadRaw: String?,
)

data class FuelQrPrefill(
    val timestampEpochMillis: Long?,
    val totalAmount: Double?,
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

data class ServiceWorkItem(
    val title: String,
    val totalAmount: Double,
)

data class ServicePartItem(
    val title: String,
    val quantity: Int,
    val totalAmount: Double,
)

data class ServiceEntrySummary(
    val id: String,
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val title: String,
    val notes: String,
    val totalAmount: Double,
    val address: String?,
    val phone: String?,
    val contact: String?,
    val workItems: List<ServiceWorkItem>,
    val partItems: List<ServicePartItem>,
    val mileageReading: OdometerReading?,
)

data class ServiceEntryDraft(
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val title: String,
    val notes: String,
    val address: String?,
    val phone: String?,
    val contact: String?,
    val workItems: List<ServiceWorkItem>,
    val partItems: List<ServicePartItem>,
    val mileageValue: Double?,
    val mileageUnit: DistanceUnit?,
)
