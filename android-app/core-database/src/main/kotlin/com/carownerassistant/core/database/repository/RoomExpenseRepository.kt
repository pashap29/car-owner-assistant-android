package com.carownerassistant.core.database.repository

import com.carownerassistant.core.database.AppDatabase
import com.carownerassistant.core.database.entity.ExpenseEntryEntity
import com.carownerassistant.core.model.DistanceUnit
import com.carownerassistant.core.model.ExpenseCategory
import com.carownerassistant.core.model.ExpenseEntryDraft
import com.carownerassistant.core.model.ExpenseEntrySummary
import com.carownerassistant.core.model.ExpenseFilter
import com.carownerassistant.core.model.ExpenseRules
import com.carownerassistant.core.model.MileageUnitConverter
import com.carownerassistant.core.model.OdometerReading
import com.carownerassistant.core.model.repository.ExpenseRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomExpenseRepository(
    database: AppDatabase,
) : ExpenseRepository {

    private val expenseDao = database.expenseDao()

    override fun observeExpenses(vehicleId: String): Flow<List<ExpenseEntrySummary>> {
        return expenseDao.observeForVehicle(vehicleId).map { entities ->
            entities.map { it.toSummary() }
        }
    }

    override suspend fun createExpense(draft: ExpenseEntryDraft): String {
        val expenseId = UUID.randomUUID().toString()
        expenseDao.insert(
            ExpenseEntryEntity(
                expenseEntryId = expenseId,
                vehicleId = draft.vehicleId,
                timestampEpochMillis = draft.timestampEpochMillis,
                category = draft.category.name,
                totalAmount = draft.totalAmount,
                note = draft.note.trim(),
                attachmentPath = draft.attachmentPath,
                odometerKm = draft.toReading()?.km,
                odometerMi = draft.toReading()?.mi,
            ),
        )
        return expenseId
    }

    override suspend fun updateExpense(
        expenseId: String,
        draft: ExpenseEntryDraft,
    ) {
        val existing = expenseDao.getById(expenseId) ?: return
        val reading = draft.toReading()
        expenseDao.update(
            existing.copy(
                vehicleId = draft.vehicleId,
                timestampEpochMillis = draft.timestampEpochMillis,
                category = draft.category.name,
                totalAmount = draft.totalAmount,
                note = draft.note.trim(),
                attachmentPath = draft.attachmentPath,
                odometerKm = reading?.km,
                odometerMi = reading?.mi,
            ),
        )
    }

    override suspend fun deleteExpense(expenseId: String) {
        expenseDao.deleteById(expenseId)
    }

    override suspend fun searchExpenses(
        vehicleId: String,
        filter: ExpenseFilter,
    ): List<ExpenseEntrySummary> {
        return ExpenseRules.applyFilter(
            entries = expenseDao.getForVehicle(vehicleId).map { it.toSummary() },
            filter = filter,
        )
    }

    private fun ExpenseEntryEntity.toSummary(): ExpenseEntrySummary {
        return ExpenseEntrySummary(
            id = expenseEntryId,
            vehicleId = vehicleId,
            timestampEpochMillis = timestampEpochMillis,
            category = enumValueOf<ExpenseCategory>(category),
            totalAmount = totalAmount,
            note = note,
            attachmentPath = attachmentPath,
            mileageReading = if (odometerKm != null && odometerMi != null) {
                OdometerReading(
                    km = odometerKm,
                    mi = odometerMi,
                )
            } else {
                null
            },
        )
    }

    private fun ExpenseEntryDraft.toReading(): OdometerReading? {
        val value = mileageValue ?: return null
        val unit = mileageUnit ?: return null
        return MileageUnitConverter.toReading(
            value = value,
            inputUnit = unit,
        )
    }
}
