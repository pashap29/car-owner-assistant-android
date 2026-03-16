package com.carownerassistant.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.carownerassistant.core.model.AppStartMode
import com.carownerassistant.core.model.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class AppPreferences(
    val unitSystem: String = "metric",
    val currencyCode: String = "USD",
    val remindersEnabled: Boolean = true,
    val appStartMode: AppStartMode = AppStartMode.ACTIVE_CAR,
)

interface AppPreferencesStore {
    fun observe(): Flow<AppPreferences>
    suspend fun setUnitSystem(unitSystem: String)
    suspend fun setCurrencyCode(currencyCode: String)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setAppStartMode(mode: AppStartMode)
}

class DataStoreAppPreferencesStore(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesStore {

    override fun observe(): Flow<AppPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppPreferences(
                unitSystem = preferences[Keys.UNIT_SYSTEM] ?: "metric",
                currencyCode = preferences[Keys.CURRENCY_CODE] ?: "USD",
                remindersEnabled = preferences[Keys.REMINDERS_ENABLED] ?: true,
                appStartMode = preferences[Keys.APP_START_MODE]
                    ?.let { value -> AppStartMode.entries.firstOrNull { it.name == value } }
                    ?: AppStartMode.ACTIVE_CAR,
            )
        }

    override suspend fun setUnitSystem(unitSystem: String) {
        dataStore.edit { it[Keys.UNIT_SYSTEM] = unitSystem }
    }

    override suspend fun setCurrencyCode(currencyCode: String) {
        dataStore.edit { it[Keys.CURRENCY_CODE] = currencyCode }
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }

    override suspend fun setAppStartMode(mode: AppStartMode) {
        dataStore.edit { it[Keys.APP_START_MODE] = mode.name }
    }

    private object Keys {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val APP_START_MODE = stringPreferencesKey("app_start_mode")
    }
}

class DataStoreSettingsRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : SettingsRepository {
    override fun remindersEnabled(): Flow<Boolean> {
        return appPreferencesStore.observe().map { it.remindersEnabled }
    }

    override fun appStartMode(): Flow<AppStartMode> {
        return appPreferencesStore.observe().map { it.appStartMode }
    }

    override suspend fun setAppStartMode(mode: AppStartMode) {
        appPreferencesStore.setAppStartMode(mode)
    }
}

object AppPreferencesDataStoreFactory {
    fun create(context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("app_preferences.preferences_pb") },
        )
    }
}

object AppPreferencesStoreFactory {
    fun create(context: Context): AppPreferencesStore {
        return DataStoreAppPreferencesStore(
            dataStore = AppPreferencesDataStoreFactory.create(context),
        )
    }
}
