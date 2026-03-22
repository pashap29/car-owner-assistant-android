package com.carownerassistant.core.database.repository

import androidx.room.withTransaction
import com.carownerassistant.core.database.AppDatabase
import com.carownerassistant.core.database.entity.FuelEntryEntity
import com.carownerassistant.core.model.FuelEntryDraft
import com.carownerassistant.core.model.FuelEntryMethod
import com.carownerassistant.core.model.FuelEntrySummary
import com.carownerassistant.core.model.FuelRules
import com.carownerassistant.core.model.FuelType
import com.carownerassistant.core.model.MileageUnitConverter
import com.carownerassistant.core.model.OdometerReading
import com.carownerassistant.core.model.repository.FuelRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFuelRepository(
    private val database: AppDatabase,
) : FuelRepository {

    private val fuelDao = database.fuelDao()
    private val mileageDao = database.mileageDao()

    override fun observeFuel(vehicleId: String): Flow<List<FuelEntrySummary>> {
        return fuelDao.observeForVehicle(vehicleId).map { entities ->
            entities
                .map { it.toSummary() }
                .sortedByDescending { it.timestampEpochMillis }
        }
    }

    override suspend fun createFuelEntry(draft: FuelEntryDraft): String {
        val fuelEntryId = UUID.randomUUID().toString()

        database.withTransaction {
            val existingMileage = mileageDao.getForVehicle(draft.vehicleId)
            val previousMileageKm = existingMileage
                .lastOrNull { it.timestampEpochMillis <= draft.timestampEpochMillis }
                ?.odometerKm

            val reading = draft.odometerValue?.let { value ->
                val unit = requireNotNull(draft.odometerUnit) {
                    "Fuel mileage unit is required when odometer value is provided."
                }
                MileageUnitConverter.toReading(
                    value = value,
                    inputUnit = unit,
                )
            }

            val mileageDeltaKm = reading?.let {
                FuelRules.computeMileageDeltaKm(
                    previousMileageKm = previousMileageKm,
                    newReading = it,
                )
            }

            fuelDao.insert(
                FuelEntryEntity(
                    fuelEntryId = fuelEntryId,
                    vehicleId = draft.vehicleId,
                    timestampEpochMillis = draft.timestampEpochMillis,
                    liters = draft.liters,
                    totalAmount = draft.totalAmount,
                    isFullTank = draft.isFullTank,
                    fuelType = draft.fuelType.name,
                    entryMethod = draft.entryMethod.name,
                    odometerKm = reading?.km,
                    odometerMi = reading?.mi,
                    mileageDeltaKm = mileageDeltaKm,
                    qrPayloadRaw = draft.qrPayloadRaw?.takeIf { it.isNotBlank() },
                ),
            )

            if (draft.odometerValue != null && draft.odometerUnit != null) {
                persistMileageEntry(
                    database = database,
                    draft = FuelRules.buildLinkedMileageDraft(draft),
                    entryId = UUID.randomUUID().toString(),
                )
            }
        }

        return fuelEntryId
    }

    private fun FuelEntryEntity.toSummary(): FuelEntrySummary {
        return FuelEntrySummary(
            id = fuelEntryId,
            vehicleId = vehicleId,
            timestampEpochMillis = timestampEpochMillis,
            liters = liters,
            totalAmount = totalAmount,
            isFullTank = isFullTank,
            fuelType = enumValueOf<FuelType>(fuelType),
            entryMethod = enumValueOf<FuelEntryMethod>(entryMethod),
            odometerReading = if (odometerKm != null && odometerMi != null) {
                OdometerReading(
                    km = odometerKm,
                    mi = odometerMi,
                )
            } else {
                null
            },
            mileageDeltaKm = mileageDeltaKm,
            qrPayloadRaw = qrPayloadRaw,
        )
    }
}
