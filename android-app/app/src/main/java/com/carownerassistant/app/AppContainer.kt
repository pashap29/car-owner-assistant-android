package com.carownerassistant.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.carownerassistant.app.places.SamplePlacesGateway
import com.carownerassistant.core.database.DatabaseFactory
import com.carownerassistant.core.database.repository.RoomExpenseRepository
import com.carownerassistant.core.database.repository.RoomFuelRepository
import com.carownerassistant.core.database.repository.RoomHandbookRepository
import com.carownerassistant.core.database.repository.RoomMileageRepository
import com.carownerassistant.core.database.repository.RoomPlacesRepository
import com.carownerassistant.core.database.repository.RoomServiceRepository
import com.carownerassistant.core.database.repository.RoomVehicleRepository
import com.carownerassistant.core.datastore.AppPreferencesStoreFactory
import com.carownerassistant.core.datastore.DataStoreSettingsRepository
import com.carownerassistant.core.flags.FeatureFlag
import com.carownerassistant.core.flags.FeatureFlagRepository
import com.carownerassistant.core.flags.InMemoryFeatureFlagRepository
import com.carownerassistant.core.files.AndroidAppFileStore
import com.carownerassistant.core.files.AppFileStore
import com.carownerassistant.core.model.repository.ExpenseRepository
import com.carownerassistant.core.model.repository.FuelRepository
import com.carownerassistant.core.model.repository.HandbookRepository
import com.carownerassistant.core.model.repository.MileageRepository
import com.carownerassistant.core.model.repository.PlacesRepository
import com.carownerassistant.core.model.repository.ServiceRepository
import com.carownerassistant.core.model.repository.SettingsRepository
import com.carownerassistant.core.model.repository.VehicleRepository

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val database = DatabaseFactory.create(appContext)
    private val appPreferencesStore = AppPreferencesStoreFactory.create(appContext)
    private val samplePlacesGateway = SamplePlacesGateway()

    val fileStore: AppFileStore = AndroidAppFileStore(appContext)
    val featureFlagRepository: FeatureFlagRepository = InMemoryFeatureFlagRepository().apply {
        setLocalOverride(FeatureFlag.MAP_PLACES_ENABLED, true)
    }
    val vehicleRepository: VehicleRepository = RoomVehicleRepository(database)
    val mileageRepository: MileageRepository = RoomMileageRepository(database)
    val fuelRepository: FuelRepository = RoomFuelRepository(database)
    val handbookRepository: HandbookRepository = RoomHandbookRepository(database)
    val expenseRepository: ExpenseRepository = RoomExpenseRepository(database)
    val serviceRepository: ServiceRepository = RoomServiceRepository(database)
    val placesRepository: PlacesRepository = RoomPlacesRepository(
        database = database,
        placeSearchGateway = samplePlacesGateway,
        placeDetailsGateway = samplePlacesGateway,
    )
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(appPreferencesStore)
}

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current.applicationContext
    return remember(context) { AppContainer(context) }
}
