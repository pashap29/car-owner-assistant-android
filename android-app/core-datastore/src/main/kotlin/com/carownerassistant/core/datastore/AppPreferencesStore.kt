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
import com.carownerassistant.core.model.AppLanguage
import com.carownerassistant.core.model.AppSettingsSnapshot
import com.carownerassistant.core.model.BackupSettings
import com.carownerassistant.core.model.DistanceUnit
import com.carownerassistant.core.model.FuelVolumeUnit
import com.carownerassistant.core.model.UserProfile
import com.carownerassistant.core.model.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class AppPreferences(
    val profile: UserProfile = UserProfile(),
    val remindersEnabled: Boolean = true,
    val appStartMode: AppStartMode = AppStartMode.ACTIVE_CAR,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val mileageDisplayUnit: DistanceUnit = DistanceUnit.KM,
    val fuelVolumeUnit: FuelVolumeUnit = FuelVolumeUnit.LITER,
    val backupSettings: BackupSettings = BackupSettings(),
)

interface AppPreferencesStore {
    fun observe(): Flow<AppPreferences>
    suspend fun updateProfile(profile: UserProfile)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setAppStartMode(mode: AppStartMode)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setMileageDisplayUnit(unit: DistanceUnit)
    suspend fun setFuelVolumeUnit(unit: FuelVolumeUnit)
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    suspend fun setBackupIncludeMedia(enabled: Boolean)
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
                profile = UserProfile(
                    displayName = preferences[Keys.PROFILE_DISPLAY_NAME].orEmpty(),
                    email = preferences[Keys.PROFILE_EMAIL].orEmpty(),
                    phone = preferences[Keys.PROFILE_PHONE].orEmpty(),
                ),
                remindersEnabled = preferences[Keys.REMINDERS_ENABLED] ?: true,
                appStartMode = preferences[Keys.APP_START_MODE]
                    ?.let { value -> AppStartMode.entries.firstOrNull { it.name == value } }
                    ?: AppStartMode.ACTIVE_CAR,
                language = preferences[Keys.LANGUAGE]
                    ?.let { value -> AppLanguage.entries.firstOrNull { it.name == value } }
                    ?: AppLanguage.SYSTEM,
                mileageDisplayUnit = preferences[Keys.MILEAGE_DISPLAY_UNIT]
                    ?.let { value -> DistanceUnit.entries.firstOrNull { it.name == value } }
                    ?: DistanceUnit.KM,
                fuelVolumeUnit = preferences[Keys.FUEL_VOLUME_UNIT]
                    ?.let { value -> FuelVolumeUnit.entries.firstOrNull { it.name == value } }
                    ?: FuelVolumeUnit.LITER,
                backupSettings = BackupSettings(
                    autoBackupEnabled = preferences[Keys.AUTO_BACKUP_ENABLED] ?: false,
                    includeMediaInBackup = preferences[Keys.BACKUP_INCLUDE_MEDIA] ?: true,
                ),
            )
        }

    override suspend fun updateProfile(profile: UserProfile) {
        dataStore.edit {
            it[Keys.PROFILE_DISPLAY_NAME] = profile.displayName
            it[Keys.PROFILE_EMAIL] = profile.email
            it[Keys.PROFILE_PHONE] = profile.phone
        }
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }

    override suspend fun setAppStartMode(mode: AppStartMode) {
        dataStore.edit { it[Keys.APP_START_MODE] = mode.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    override suspend fun setMileageDisplayUnit(unit: DistanceUnit) {
        dataStore.edit { it[Keys.MILEAGE_DISPLAY_UNIT] = unit.name }
    }

    override suspend fun setFuelVolumeUnit(unit: FuelVolumeUnit) {
        dataStore.edit { it[Keys.FUEL_VOLUME_UNIT] = unit.name }
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    override suspend fun setBackupIncludeMedia(enabled: Boolean) {
        dataStore.edit { it[Keys.BACKUP_INCLUDE_MEDIA] = enabled }
    }

    private object Keys {
        val PROFILE_DISPLAY_NAME = stringPreferencesKey("profile_display_name")
        val PROFILE_EMAIL = stringPreferencesKey("profile_email")
        val PROFILE_PHONE = stringPreferencesKey("profile_phone")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val APP_START_MODE = stringPreferencesKey("app_start_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val MILEAGE_DISPLAY_UNIT = stringPreferencesKey("mileage_display_unit")
        val FUEL_VOLUME_UNIT = stringPreferencesKey("fuel_volume_unit")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_INCLUDE_MEDIA = booleanPreferencesKey("backup_include_media")
    }
}

class DataStoreSettingsRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : SettingsRepository {
    override fun profile(): Flow<UserProfile> {
        return appPreferencesStore.observe().map { it.profile }
    }

    override fun settings(): Flow<AppSettingsSnapshot> {
        return appPreferencesStore.observe().map { preferences ->
            AppSettingsSnapshot(
                appStartMode = preferences.appStartMode,
                language = preferences.language,
                mileageDisplayUnit = preferences.mileageDisplayUnit,
                fuelVolumeUnit = preferences.fuelVolumeUnit,
                remindersEnabled = preferences.remindersEnabled,
                backupSettings = preferences.backupSettings,
            )
        }
    }

    override fun remindersEnabled(): Flow<Boolean> {
        return appPreferencesStore.observe().map { it.remindersEnabled }
    }

    override fun appStartMode(): Flow<AppStartMode> {
        return appPreferencesStore.observe().map { it.appStartMode }
    }

    override suspend fun updateProfile(profile: UserProfile) {
        appPreferencesStore.updateProfile(profile)
    }

    override suspend fun setAppStartMode(mode: AppStartMode) {
        appPreferencesStore.setAppStartMode(mode)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        appPreferencesStore.setLanguage(language)
    }

    override suspend fun setMileageDisplayUnit(unit: DistanceUnit) {
        appPreferencesStore.setMileageDisplayUnit(unit)
    }

    override suspend fun setFuelVolumeUnit(unit: FuelVolumeUnit) {
        appPreferencesStore.setFuelVolumeUnit(unit)
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        appPreferencesStore.setRemindersEnabled(enabled)
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        appPreferencesStore.setAutoBackupEnabled(enabled)
    }

    override suspend fun setBackupIncludeMedia(enabled: Boolean) {
        appPreferencesStore.setBackupIncludeMedia(enabled)
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
