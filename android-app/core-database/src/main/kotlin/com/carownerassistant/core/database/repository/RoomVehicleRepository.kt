package com.carownerassistant.core.database.repository

import androidx.room.withTransaction
import com.carownerassistant.core.database.AppDatabase
import com.carownerassistant.core.database.entity.VehicleEntity
import com.carownerassistant.core.model.GarageRules
import com.carownerassistant.core.model.VehicleDraft
import com.carownerassistant.core.model.VehicleSummary
import com.carownerassistant.core.model.repository.VehicleRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomVehicleRepository(
    private val database: AppDatabase,
) : VehicleRepository {

    private val vehicleDao = database.vehicleDao()

    override fun observeVehicles(): Flow<List<VehicleSummary>> {
        return vehicleDao.observeVehicles().map { vehicles -> vehicles.map { it.toSummary() } }
    }

    override fun activeVehicleId(): Flow<String?> = vehicleDao.observeActiveVehicleId()

    override suspend fun createVehicle(draft: VehicleDraft): String {
        val vehicleId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        database.withTransaction {
            val shouldBeActive = GarageRules.shouldNewVehicleBeActive(
                existingVehicles = vehicleDao.observeVehiclesOnce().map { it.toSummary() },
                makeActiveRequested = draft.makeActive,
            )
            if (shouldBeActive) {
                vehicleDao.clearActive()
            }
            vehicleDao.insert(
                VehicleEntity(
                    vehicleId = vehicleId,
                    displayName = draft.displayName.trim(),
                    make = draft.make.trim(),
                    model = draft.model.trim(),
                    year = draft.year,
                    plateNumber = draft.plateNumber.trim(),
                    isActive = shouldBeActive,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
        }

        return vehicleId
    }

    override suspend fun updateVehicle(vehicleId: String, draft: VehicleDraft) {
        val existing = vehicleDao.getVehicle(vehicleId) ?: return
        val now = System.currentTimeMillis()

        database.withTransaction {
            val shouldBeActive = GarageRules.shouldUpdatedVehicleRemainActive(
                currentlyActive = existing.isActive,
                makeActiveRequested = draft.makeActive,
            )
            if (shouldBeActive) {
                vehicleDao.clearActive()
            }
            vehicleDao.update(
                existing.copy(
                    displayName = draft.displayName.trim(),
                    make = draft.make.trim(),
                    model = draft.model.trim(),
                    year = draft.year,
                    plateNumber = draft.plateNumber.trim(),
                    isActive = shouldBeActive,
                    updatedAtEpochMillis = now,
                ),
            )
        }
    }

    override suspend fun deleteVehicle(vehicleId: String) {
        database.withTransaction {
            val existing = vehicleDao.getVehicle(vehicleId) ?: return@withTransaction
            val allVehicles = vehicleDao.observeVehiclesOnce()
            vehicleDao.deleteById(vehicleId)

            if (existing.isActive) {
                val fallbackVehicleId = GarageRules.nextActiveVehicleIdAfterDelete(
                    existingVehicles = allVehicles.map { it.toSummary() },
                    deletedVehicleId = vehicleId,
                )
                if (fallbackVehicleId != null) {
                    vehicleDao.setActive(
                        vehicleId = fallbackVehicleId,
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    )
                }
            }
        }
    }

    override suspend fun setActiveVehicle(vehicleId: String) {
        val existing = vehicleDao.getVehicle(vehicleId) ?: return
        if (existing.isActive) return

        database.withTransaction {
            vehicleDao.clearActive()
            vehicleDao.setActive(
                vehicleId = vehicleId,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        }
    }

    private fun VehicleEntity.toSummary(): VehicleSummary {
        return VehicleSummary(
            id = vehicleId,
            displayName = displayName,
            make = make,
            model = model,
            year = year,
            plateNumber = plateNumber,
            isActive = isActive,
        )
    }
}
