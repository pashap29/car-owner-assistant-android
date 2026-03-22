package com.carownerassistant.core.database.repository

import com.carownerassistant.core.database.AppDatabase
import com.carownerassistant.core.database.entity.MileageEntryEntity
import com.carownerassistant.core.model.DistanceUnit
import com.carownerassistant.core.model.MileageAnomalyType
import com.carownerassistant.core.model.MileageEntryDraft
import com.carownerassistant.core.model.MileageEntryOrigin
import com.carownerassistant.core.model.MileageEntrySummary
import com.carownerassistant.core.model.MileageLedgerEvaluator
import com.carownerassistant.core.model.MileageStatus
import com.carownerassistant.core.model.MileageUnitConverter
import com.carownerassistant.core.model.OdometerReading
import com.carownerassistant.core.model.RawMileageEntry

internal suspend fun persistMileageEntry(
    database: AppDatabase,
    draft: MileageEntryDraft,
    entryId: String,
): String {
    val mileageDao = database.mileageDao()
    val reading = MileageUnitConverter.toReading(
        value = draft.value,
        inputUnit = draft.inputUnit,
    )
    val existingEntries = mileageDao.getForVehicle(draft.vehicleId)
    val newEntry = MileageEntryEntity(
        mileageEntryId = entryId,
        vehicleId = draft.vehicleId,
        timestampEpochMillis = draft.timestampEpochMillis,
        odometerKm = reading.km,
        odometerMi = reading.mi,
        origin = draft.origin.name,
        userEnteredValue = draft.value,
        userEnteredUnit = draft.inputUnit.name,
        photoFilePath = draft.photoFilePath,
        manualEntry = draft.manualEntry,
        status = MileageStatus.UNVERIFIED.name,
        criticalAnomaly = false,
        largeJumpSuspected = false,
        trustedReferenceMileageEntryId = null,
        trustScore = 0,
        anomalyFlagsCsv = "",
    )

    val evaluated = MileageLedgerEvaluator.evaluate(
        entries = (existingEntries + newEntry).map { it.toRawEntry() },
    )

    mileageDao.upsertAll(
        entries = evaluated.map { it.toEntity() },
    )

    return entryId
}

internal fun MileageEntryEntity.toRawEntry(): RawMileageEntry {
    return RawMileageEntry(
        id = mileageEntryId,
        vehicleId = vehicleId,
        timestampEpochMillis = timestampEpochMillis,
        odometerKm = odometerKm,
        odometerMi = odometerMi,
        origin = enumValueOf<MileageEntryOrigin>(origin),
        userEnteredValue = userEnteredValue,
        userEnteredUnit = enumValueOf<DistanceUnit>(userEnteredUnit),
        photoFilePath = photoFilePath,
        manualEntry = manualEntry,
    )
}

internal fun MileageEntrySummary.toEntity(): MileageEntryEntity {
    return MileageEntryEntity(
        mileageEntryId = id,
        vehicleId = vehicleId,
        timestampEpochMillis = timestampEpochMillis,
        odometerKm = reading.km,
        odometerMi = reading.mi,
        origin = origin.name,
        userEnteredValue = userEnteredValue,
        userEnteredUnit = userEnteredUnit.name,
        photoFilePath = photoFilePath,
        manualEntry = manualEntry,
        status = status.name,
        criticalAnomaly = criticalAnomaly,
        largeJumpSuspected = largeJumpSuspected,
        trustedReferenceMileageEntryId = trustedReferenceEntryId,
        trustScore = trustScore,
        anomalyFlagsCsv = anomalyTypes.joinToString(",") { it.name },
    )
}

internal fun MileageEntryEntity.toSummary(): MileageEntrySummary {
    return MileageEntrySummary(
        id = mileageEntryId,
        vehicleId = vehicleId,
        timestampEpochMillis = timestampEpochMillis,
        reading = OdometerReading(
            km = odometerKm,
            mi = odometerMi,
        ),
        status = enumValueOf<MileageStatus>(status),
        origin = enumValueOf<MileageEntryOrigin>(origin),
        userEnteredValue = userEnteredValue,
        userEnteredUnit = enumValueOf<DistanceUnit>(userEnteredUnit),
        photoFilePath = photoFilePath,
        manualEntry = manualEntry,
        criticalAnomaly = criticalAnomaly,
        largeJumpSuspected = largeJumpSuspected,
        trustedReferenceEntryId = trustedReferenceMileageEntryId,
        trustScore = trustScore,
        anomalyTypes = anomalyFlagsCsv
            .split(',')
            .filter { it.isNotBlank() }
            .map { enumValueOf<MileageAnomalyType>(it) },
    )
}
