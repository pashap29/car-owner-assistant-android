package com.carownerassistant.core.database.repository

import androidx.room.withTransaction
import com.carownerassistant.core.database.AppDatabase
import com.carownerassistant.core.database.dao.ServiceEntryAggregate
import com.carownerassistant.core.database.entity.ServiceEntryEntity
import com.carownerassistant.core.database.entity.ServicePartItemEntity
import com.carownerassistant.core.database.entity.ServiceWorkItemEntity
import com.carownerassistant.core.model.MileageUnitConverter
import com.carownerassistant.core.model.OdometerReading
import com.carownerassistant.core.model.ServiceEntryDraft
import com.carownerassistant.core.model.ServiceEntrySummary
import com.carownerassistant.core.model.ServicePartItem
import com.carownerassistant.core.model.ServiceRules
import com.carownerassistant.core.model.ServiceWorkItem
import com.carownerassistant.core.model.repository.ServiceRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomServiceRepository(
    private val database: AppDatabase,
) : ServiceRepository {

    private val serviceDao = database.serviceDao()

    override fun observeServices(vehicleId: String): Flow<List<ServiceEntrySummary>> {
        return serviceDao.observeForVehicle(vehicleId).map { aggregates ->
            aggregates.map { it.toSummary() }
        }
    }

    override suspend fun createServiceEntry(draft: ServiceEntryDraft): String {
        val serviceEntryId = UUID.randomUUID().toString()

        database.withTransaction {
            val linkedMileageEntryId = ServiceRules.buildLinkedMileageDraft(draft)?.let { mileageDraft ->
                val mileageEntryId = UUID.randomUUID().toString()
                persistMileageEntry(
                    database = database,
                    draft = mileageDraft,
                    entryId = mileageEntryId,
                )
                mileageEntryId
            }

            val reading = draft.toReading()
            serviceDao.insertEntry(
                ServiceEntryEntity(
                    serviceEntryId = serviceEntryId,
                    vehicleId = draft.vehicleId,
                    timestampEpochMillis = draft.timestampEpochMillis,
                    title = draft.title.trim(),
                    notes = draft.notes.trim(),
                    totalAmount = ServiceRules.totalAmount(draft.workItems, draft.partItems),
                    address = draft.address?.trim()?.takeIf { it.isNotBlank() },
                    phone = draft.phone?.trim()?.takeIf { it.isNotBlank() },
                    contact = draft.contact?.trim()?.takeIf { it.isNotBlank() },
                    odometerKm = reading?.km,
                    odometerMi = reading?.mi,
                    linkedMileageEntryId = linkedMileageEntryId,
                ),
            )
            serviceDao.insertWorkItems(
                draft.workItems.map { workItem ->
                    ServiceWorkItemEntity(
                        serviceWorkItemId = UUID.randomUUID().toString(),
                        serviceEntryId = serviceEntryId,
                        title = workItem.title.trim(),
                        totalAmount = workItem.totalAmount,
                    )
                },
            )
            serviceDao.insertPartItems(
                draft.partItems.map { partItem ->
                    ServicePartItemEntity(
                        servicePartItemId = UUID.randomUUID().toString(),
                        serviceEntryId = serviceEntryId,
                        title = partItem.title.trim(),
                        quantity = partItem.quantity,
                        totalAmount = partItem.totalAmount,
                    )
                },
            )
        }

        return serviceEntryId
    }

    override suspend fun updateServiceEntry(
        serviceEntryId: String,
        draft: ServiceEntryDraft,
    ) {
        val existing = serviceDao.getById(serviceEntryId) ?: return

        database.withTransaction {
            val linkedMileageDraft = ServiceRules.buildLinkedMileageDraft(draft)
            val linkedMileageEntryId = when {
                linkedMileageDraft != null && existing.entry.linkedMileageEntryId != null -> {
                    persistMileageEntry(
                        database = database,
                        draft = linkedMileageDraft,
                        entryId = existing.entry.linkedMileageEntryId,
                    )
                    existing.entry.linkedMileageEntryId
                }

                linkedMileageDraft != null -> {
                    val newMileageEntryId = UUID.randomUUID().toString()
                    persistMileageEntry(
                        database = database,
                        draft = linkedMileageDraft,
                        entryId = newMileageEntryId,
                    )
                    newMileageEntryId
                }

                existing.entry.linkedMileageEntryId != null -> {
                    deleteMileageEntry(
                        database = database,
                        vehicleId = existing.entry.vehicleId,
                        mileageEntryId = existing.entry.linkedMileageEntryId,
                    )
                    null
                }

                else -> null
            }

            val reading = draft.toReading()

            serviceDao.updateEntry(
                existing.entry.copy(
                    vehicleId = draft.vehicleId,
                    timestampEpochMillis = draft.timestampEpochMillis,
                    title = draft.title.trim(),
                    notes = draft.notes.trim(),
                    totalAmount = ServiceRules.totalAmount(draft.workItems, draft.partItems),
                    address = draft.address?.trim()?.takeIf { it.isNotBlank() },
                    phone = draft.phone?.trim()?.takeIf { it.isNotBlank() },
                    contact = draft.contact?.trim()?.takeIf { it.isNotBlank() },
                    odometerKm = reading?.km,
                    odometerMi = reading?.mi,
                    linkedMileageEntryId = linkedMileageEntryId,
                ),
            )
            serviceDao.deleteWorkItemsForService(serviceEntryId)
            serviceDao.deletePartItemsForService(serviceEntryId)
            serviceDao.insertWorkItems(
                draft.workItems.map { workItem ->
                    ServiceWorkItemEntity(
                        serviceWorkItemId = UUID.randomUUID().toString(),
                        serviceEntryId = serviceEntryId,
                        title = workItem.title.trim(),
                        totalAmount = workItem.totalAmount,
                    )
                },
            )
            serviceDao.insertPartItems(
                draft.partItems.map { partItem ->
                    ServicePartItemEntity(
                        servicePartItemId = UUID.randomUUID().toString(),
                        serviceEntryId = serviceEntryId,
                        title = partItem.title.trim(),
                        quantity = partItem.quantity,
                        totalAmount = partItem.totalAmount,
                    )
                },
            )
        }
    }

    private fun ServiceEntryDraft.toReading(): OdometerReading? {
        val value = mileageValue ?: return null
        val unit = mileageUnit ?: return null
        return MileageUnitConverter.toReading(
            value = value,
            inputUnit = unit,
        )
    }

    private fun ServiceEntryAggregate.toSummary(): ServiceEntrySummary {
        return ServiceEntrySummary(
            id = entry.serviceEntryId,
            vehicleId = entry.vehicleId,
            timestampEpochMillis = entry.timestampEpochMillis,
            title = entry.title,
            notes = entry.notes,
            totalAmount = entry.totalAmount,
            address = entry.address,
            phone = entry.phone,
            contact = entry.contact,
            workItems = workItems.map { workItem ->
                ServiceWorkItem(
                    title = workItem.title,
                    totalAmount = workItem.totalAmount,
                )
            },
            partItems = partItems.map { partItem ->
                ServicePartItem(
                    title = partItem.title,
                    quantity = partItem.quantity,
                    totalAmount = partItem.totalAmount,
                )
            },
            mileageReading = if (entry.odometerKm != null && entry.odometerMi != null) {
                OdometerReading(
                    km = entry.odometerKm,
                    mi = entry.odometerMi,
                )
            } else {
                null
            },
        )
    }
}
