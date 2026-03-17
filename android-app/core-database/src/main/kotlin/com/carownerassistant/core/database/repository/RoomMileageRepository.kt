package com.carownerassistant.core.database.repository

import androidx.room.withTransaction
import com.carownerassistant.core.database.AppDatabase
import com.carownerassistant.core.database.entity.MileageEntryEntity
import com.carownerassistant.core.model.DistanceUnit
import com.carownerassistant.core.model.MileageAnomalyType
import com.carownerassistant.core.model.MileageEntryDraft
import com.carownerassistant.core.model.MileageEntryOrigin
import com.carownerassistant.core.model.MileageEntrySummary
import com.carownerassistant.core.model.MileageLedgerEvaluator
import com.carownerassistant.core.model.MileageLedgerSummary
import com.carownerassistant.core.model.MileageStatus
import com.carownerassistant.core.model.MileageUnitConverter
import com.carownerassistant.core.model.RawMileageEntry
import com.carownerassistant.core.model.repository.MileageRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMileageRepository(
    private val database: AppDatabase,
) : MileageRepository {

    private val mileageDao = database.mileageDao()

    override fun observeMileage(vehicleId: String): Flow<List<MileageEntrySummary>> {
        return mileageDao.observeForVehicle(vehicleId).map { entries ->
            entries.map { it.toSummary() }
        }
    }

    override fun observeMileageLedger(vehicleId: String): Flow<MileageLedgerSummary> {
        return observeMileage(vehicleId).map(MileageLedgerEvaluator::toLedger)
    }

    override suspend fun addMileageEntry(draft: MileageEntryDraft): String {
        val entryId = UUID.randomUUID().toString()
        val reading = MileageUnitConverter.toReading(
            value = draft.value,
            inputUnit = draft.inputUnit,
        )

        database.withTransaction {
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
        }

        return entryId
    }

    private fun MileageEntryEntity.toRawEntry(): RawMileageEntry {
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

    private fun MileageEntrySummary.toEntity(): MileageEntryEntity {
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

    private fun MileageEntryEntity.toSummary(): MileageEntrySummary {
        return MileageEntrySummary(
            id = mileageEntryId,
            vehicleId = vehicleId,
            timestampEpochMillis = timestampEpochMillis,
            reading = com.carownerassistant.core.model.OdometerReading(
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
}
