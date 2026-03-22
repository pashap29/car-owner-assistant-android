package com.carownerassistant.core.model

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    RUSSIAN,
}

enum class FuelVolumeUnit {
    LITER,
    GALLON,
}

data class UserProfile(
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
)

data class BackupSettings(
    val autoBackupEnabled: Boolean = false,
    val includeMediaInBackup: Boolean = true,
)

data class AppSettingsSnapshot(
    val appStartMode: AppStartMode = AppStartMode.ACTIVE_CAR,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val mileageDisplayUnit: DistanceUnit = DistanceUnit.KM,
    val fuelVolumeUnit: FuelVolumeUnit = FuelVolumeUnit.LITER,
    val remindersEnabled: Boolean = true,
    val backupSettings: BackupSettings = BackupSettings(),
)
