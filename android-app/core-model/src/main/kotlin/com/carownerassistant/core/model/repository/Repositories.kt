package com.carownerassistant.core.model.repository

import com.carownerassistant.core.model.AppStartMode
import com.carownerassistant.core.model.AppLanguage
import com.carownerassistant.core.model.AppSettingsSnapshot
import com.carownerassistant.core.model.DistanceUnit
import com.carownerassistant.core.model.ExpenseEntryDraft
import com.carownerassistant.core.model.ExpenseEntrySummary
import com.carownerassistant.core.model.ExpenseFilter
import com.carownerassistant.core.model.FuelVolumeUnit
import com.carownerassistant.core.model.FuelEntryDraft
import com.carownerassistant.core.model.FuelEntrySummary
import com.carownerassistant.core.model.HandbookSectionId
import com.carownerassistant.core.model.MileageEntryDraft
import com.carownerassistant.core.model.MileageLedgerSummary
import com.carownerassistant.core.model.MileageEntrySummary
import com.carownerassistant.core.model.PlaceInfo
import com.carownerassistant.core.model.PlaceRef
import com.carownerassistant.core.model.PlaceSearchCriteria
import com.carownerassistant.core.model.ServiceEntryDraft
import com.carownerassistant.core.model.ServiceEntrySummary
import com.carownerassistant.core.model.UserProfile
import com.carownerassistant.core.model.VehicleHandbookSummary
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
    fun observeServices(vehicleId: String): Flow<List<ServiceEntrySummary>>
    suspend fun createServiceEntry(draft: ServiceEntryDraft): String
    suspend fun updateServiceEntry(serviceEntryId: String, draft: ServiceEntryDraft)
}

interface SettingsRepository {
    fun profile(): Flow<UserProfile>
    fun settings(): Flow<AppSettingsSnapshot>
    fun remindersEnabled(): Flow<Boolean>
    fun appStartMode(): Flow<AppStartMode>
    suspend fun updateProfile(profile: UserProfile)
    suspend fun setAppStartMode(mode: AppStartMode)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setMileageDisplayUnit(unit: DistanceUnit)
    suspend fun setFuelVolumeUnit(unit: FuelVolumeUnit)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    suspend fun setBackupIncludeMedia(enabled: Boolean)
}

interface BackupRepository {
    fun latestBackupSlot(): Flow<String?>
}

interface HandbookRepository {
    fun observeHandbook(vehicleId: String): Flow<VehicleHandbookSummary>
    suspend fun updateVin(vehicleId: String, vin: String)
    suspend fun updateSection(
        vehicleId: String,
        sectionId: HandbookSectionId,
        content: String,
    )
    suspend fun addDocument(
        vehicleId: String,
        displayName: String,
        mimeType: String,
        filePath: String,
    ): String
}

sealed interface PlaceGatewayResult<out T> {
    data class Success<T>(val value: T) : PlaceGatewayResult<T>
    data object NoNetwork : PlaceGatewayResult<Nothing>
    data object PermissionDenied : PlaceGatewayResult<Nothing>
    data object ProviderUnavailable : PlaceGatewayResult<Nothing>
    data class InvalidRequest(val reason: String) : PlaceGatewayResult<Nothing>
}

interface PlaceSearchGateway {
    suspend fun search(criteria: PlaceSearchCriteria): PlaceGatewayResult<List<PlaceInfo>>
}

interface PlaceDetailsGateway {
    suspend fun getDetails(placeRef: PlaceRef): PlaceGatewayResult<PlaceInfo>
}

interface PlacesRepository {
    fun observeFavorites(): Flow<List<PlaceInfo>>
    suspend fun searchPlaces(criteria: PlaceSearchCriteria): PlaceGatewayResult<List<PlaceInfo>>
    suspend fun getPlaceDetails(placeRef: PlaceRef): PlaceGatewayResult<PlaceInfo>
    suspend fun addFavorite(place: PlaceInfo)
    suspend fun removeFavorite(normalizedPlaceId: String)
}
