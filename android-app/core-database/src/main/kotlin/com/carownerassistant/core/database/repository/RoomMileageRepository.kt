package com.carownerassistant.core.database.repository

import androidx.room.withTransaction
import com.carownerassistant.core.database.AppDatabase
import com.carownerassistant.core.model.MileageEntryDraft
import com.carownerassistant.core.model.MileageEntrySummary
import com.carownerassistant.core.model.MileageLedgerEvaluator
import com.carownerassistant.core.model.MileageLedgerSummary
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

        database.withTransaction {
            persistMileageEntry(
                database = database,
                draft = draft,
                entryId = entryId,
            )
        }

        return entryId
    }
}
