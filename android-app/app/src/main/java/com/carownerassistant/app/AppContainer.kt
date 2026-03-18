package com.carownerassistant.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.carownerassistant.core.database.DatabaseFactory
import com.carownerassistant.core.database.repository.RoomExpenseRepository
import com.carownerassistant.core.database.repository.RoomMileageRepository
import com.carownerassistant.core.database.repository.RoomVehicleRepository
import com.carownerassistant.core.datastore.AppPreferencesStoreFactory
import com.carownerassistant.core.datastore.DataStoreSettingsRepository
import com.carownerassistant.core.files.AndroidAppFileStore
import com.carownerassistant.core.files.AppFileStore
import com.carownerassistant.core.model.repository.ExpenseRepository
import com.carownerassistant.core.model.repository.MileageRepository
import com.carownerassistant.core.model.repository.SettingsRepository
import com.carownerassistant.core.model.repository.VehicleRepository

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val database = DatabaseFactory.create(appContext)
    private val appPreferencesStore = AppPreferencesStoreFactory.create(appContext)

    val fileStore: AppFileStore = AndroidAppFileStore(appContext)
    val vehicleRepository: VehicleRepository = RoomVehicleRepository(database)
    val mileageRepository: MileageRepository = RoomMileageRepository(database)
    val expenseRepository: ExpenseRepository = RoomExpenseRepository(database)
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(appPreferencesStore)
}

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current.applicationContext
    return remember(context) { AppContainer(context) }
}
