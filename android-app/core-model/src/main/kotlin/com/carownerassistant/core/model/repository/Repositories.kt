package com.carownerassistant.core.model.repository

import com.carownerassistant.core.model.AppStartMode
import com.carownerassistant.core.model.ExpenseEntryDraft
import com.carownerassistant.core.model.ExpenseFilter
import com.carownerassistant.core.model.FuelEntryDraft
import com.carownerassistant.core.model.MileageEntryDraft
import com.carownerassistant.core.model.ExpenseEntrySummary
import com.carownerassistant.core.model.FuelEntrySummary
import com.carownerassistant.core.model.MileageLedgerSummary
import com.carownerassistant.core.model.MileageEntrySummary
import com.carownerassistant.core.model.VehicleDraft
import com.carownerassistant.core.model.VehicleSummary
import kotlinx.coroutines.flow.Flow

interface ActiveVehicleRepository {
    fun activeVehicleId(): Flow<String?>
}

interface VehicleRepository : ActiveVehicleRepository {
    fun observeVehicles(): Flow<List<VehicleSummary>>
    suspend fun createVehicle(draft: VehicleDraft): String
    suspend fun updateVehicle(vehicleId: String, draft: VehicleDraft)
    suspend fun deleteVehicle(vehicleId: String)
    suspend fun setActiveVehicle(vehicleId: String)
}

interface MileageRepository {
    fun observeMileage(vehicleId: String): Flow<List<MileageEntrySummary>>
    fun observeMileageLedger(vehicleId: String): Flow<MileageLedgerSummary>
    suspend fun addMileageEntry(draft: MileageEntryDraft): String
}

interface FuelRepository {
    fun observeFuel(vehicleId: String): Flow<List<FuelEntrySummary>>
    suspend fun createFuelEntry(draft: FuelEntryDraft): String
}

interface ExpenseRepository {
    fun observeExpenses(vehicleId: String): Flow<List<ExpenseEntrySummary>>
    suspend fun createExpense(draft: ExpenseEntryDraft): String
    suspend fun updateExpense(expenseId: String, draft: ExpenseEntryDraft)
    suspend fun deleteExpense(expenseId: String)
    suspend fun searchExpenses(
        vehicleId: String,
        filter: ExpenseFilter,
    ): List<ExpenseEntrySummary>
}

interface ServiceRepository {
    fun observeServiceCount(vehicleId: String): Flow<Int>
}

interface SettingsRepository {
    fun remindersEnabled(): Flow<Boolean>
    fun appStartMode(): Flow<AppStartMode>
    suspend fun setAppStartMode(mode: AppStartMode)
}

interface BackupRepository {
    fun latestBackupSlot(): Flow<String?>
}
