package com.carownerassistant.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carownerassistant.core.flags.FeatureFlag
import com.carownerassistant.core.flags.FeatureFlagRepository
import com.carownerassistant.core.model.AppLanguage
import com.carownerassistant.core.model.AppSettingsSnapshot
import com.carownerassistant.core.model.AppStartMode
import com.carownerassistant.core.model.DistanceUnit
import com.carownerassistant.core.model.FuelVolumeUnit
import com.carownerassistant.core.model.UserProfile
import com.carownerassistant.core.model.repository.SettingsRepository
import kotlinx.coroutines.launch

private enum class SettingsSection(val title: String) {
    OVERVIEW("Settings"),
    PROFILE("Profile"),
    APP("App Settings"),
    FUTURE("Future Features"),
}

@Composable
fun SettingsTabScreen(
    settingsRepository: SettingsRepository,
    featureFlagRepository: FeatureFlagRepository,
    onOpenGarage: () -> Unit,
    onOpenHandbook: () -> Unit,
    onOpenPlaces: () -> Unit,
) {
    val profile by settingsRepository.profile().collectAsState(initial = UserProfile())
    val settings by settingsRepository.settings().collectAsState(initial = AppSettingsSnapshot())
    val featureFlags by featureFlagRepository.flags.collectAsState()
    val scope = rememberCoroutineScope()

    var currentSection by remember { mutableStateOf(SettingsSection.OVERVIEW) }
    var profileName by remember { mutableStateOf("") }
    var profileEmail by remember { mutableStateOf("") }
    var profilePhone by remember { mutableStateOf("") }

    LaunchedEffect(profile) {
        profileName = profile.displayName
        profileEmail = profile.email
        profilePhone = profile.phone
    }

    SettingsScaffold(
        currentSection = currentSection,
        onBackToOverview = { currentSection = SettingsSection.OVERVIEW },
    ) { innerPadding ->
        when (currentSection) {
            SettingsSection.OVERVIEW -> SettingsOverviewSection(
                modifier = innerPadding,
                onOpenProfile = { currentSection = SettingsSection.PROFILE },
                onOpenAppSettings = { currentSection = SettingsSection.APP },
                onOpenFutureFeatures = { currentSection = SettingsSection.FUTURE },
                onOpenGarage = onOpenGarage,
                onOpenHandbook = onOpenHandbook,
                onOpenPlaces = onOpenPlaces,
            )

            SettingsSection.PROFILE -> ProfileSettingsSection(
                modifier = innerPadding,
                name = profileName,
                email = profileEmail,
                phone = profilePhone,
                onNameChange = { profileName = it },
                onEmailChange = { profileEmail = it },
                onPhoneChange = { profilePhone = it },
                onSave = {
                    scope.launch {
                        settingsRepository.updateProfile(
                            UserProfile(
                                displayName = profileName.trim(),
                                email = profileEmail.trim(),
                                phone = profilePhone.trim(),
                            ),
                        )
                    }
                },
            )

            SettingsSection.APP -> AppSettingsSection(
                modifier = innerPadding,
                settings = settings,
                onAppStartModeChange = { mode ->
                    scope.launch { settingsRepository.setAppStartMode(mode) }
                },
                onLanguageChange = { language ->
                    scope.launch { settingsRepository.setLanguage(language) }
                },
                onMileageUnitChange = { unit ->
                    scope.launch { settingsRepository.setMileageDisplayUnit(unit) }
                },
                onFuelVolumeUnitChange = { unit ->
                    scope.launch { settingsRepository.setFuelVolumeUnit(unit) }
                },
                onRemindersChange = { enabled ->
                    scope.launch { settingsRepository.setRemindersEnabled(enabled) }
                },
                onAutoBackupChange = { enabled ->
                    scope.launch { settingsRepository.setAutoBackupEnabled(enabled) }
                },
                onIncludeMediaChange = { enabled ->
                    scope.launch { settingsRepository.setBackupIncludeMedia(enabled) }
                },
            )

            SettingsSection.FUTURE -> FutureFeaturesSection(
                modifier = innerPadding,
                featureFlags = featureFlags,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    currentSection: SettingsSection,
    onBackToOverview: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = currentSection.title) },
                navigationIcon = {
                    if (currentSection != SettingsSection.OVERVIEW) {
                        TextButton(onClick = onBackToOverview) {
                            Text(text = "Back")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}

@Composable
private fun SettingsOverviewSection(
    modifier: Modifier,
    onOpenProfile: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenFutureFeatures: () -> Unit,
    onOpenGarage: () -> Unit,
    onOpenHandbook: () -> Unit,
    onOpenPlaces: () -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionActionCard(
                title = "Profile",
                body = "Edit local profile fields stored only on this device.",
                actionLabel = "Open profile",
                onAction = onOpenProfile,
            )
        }
        item {
            SectionActionCard(
                title = "App Settings",
                body = "Configure startup mode, language, units, reminders, and backup behavior.",
                actionLabel = "Open app settings",
                onAction = onOpenAppSettings,
            )
        }
        item {
            SectionActionCard(
                title = "Future Features",
                body = "Review structured placeholders for premium, cloud sync, family access, and auth.",
                actionLabel = "Open future features",
                onAction = onOpenFutureFeatures,
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SectionTitle(text = "Vehicle Modules")
                    Button(
                        onClick = onOpenGarage,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Open Garage")
                    }
                    Button(
                        onClick = onOpenHandbook,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Open Vehicle Handbook")
                    }
                    Button(
                        onClick = onOpenPlaces,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Open Map and Places")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSettingsSection(
    modifier: Modifier,
    name: String,
    email: String,
    phone: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle(text = "Local Profile")
                    Text(
                        text = "These fields stay on the device and prepare the app for future account features.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Display name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Email") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = onPhoneChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Phone") },
                        singleLine = true,
                    )
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Save profile")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSettingsSection(
    modifier: Modifier,
    settings: AppSettingsSnapshot,
    onAppStartModeChange: (AppStartMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onMileageUnitChange: (DistanceUnit) -> Unit,
    onFuelVolumeUnitChange: (FuelVolumeUnit) -> Unit,
    onRemindersChange: (Boolean) -> Unit,
    onAutoBackupChange: (Boolean) -> Unit,
    onIncludeMediaChange: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ChoiceCard(
                title = "Startup Mode",
                options = AppStartMode.entries,
                selected = settings.appStartMode,
                labelFor = { mode ->
                    when (mode) {
                        AppStartMode.ACTIVE_CAR -> "Open active car"
                        AppStartMode.GARAGE -> "Open garage"
                    }
                },
                onSelect = onAppStartModeChange,
            )
        }
        item {
            ChoiceCard(
                title = "Language",
                options = AppLanguage.entries,
                selected = settings.language,
                labelFor = { language ->
                    when (language) {
                        AppLanguage.SYSTEM -> "System"
                        AppLanguage.ENGLISH -> "English"
                        AppLanguage.RUSSIAN -> "Russian"
                    }
                },
                onSelect = onLanguageChange,
            )
        }
        item {
            ChoiceCard(
                title = "Mileage Units",
                options = DistanceUnit.entries,
                selected = settings.mileageDisplayUnit,
                labelFor = { unit ->
                    when (unit) {
                        DistanceUnit.KM -> "Kilometers"
                        DistanceUnit.MI -> "Miles"
                    }
                },
                onSelect = onMileageUnitChange,
            )
        }
        item {
            ChoiceCard(
                title = "Fuel Units",
                options = FuelVolumeUnit.entries,
                selected = settings.fuelVolumeUnit,
                labelFor = { unit ->
                    when (unit) {
                        FuelVolumeUnit.LITER -> "Liters"
                        FuelVolumeUnit.GALLON -> "Gallons"
                    }
                },
                onSelect = onFuelVolumeUnitChange,
            )
        }
        item {
            ToggleCard(
                title = "Mileage reminders",
                body = "Turn inactivity reminders on or off without affecting history.",
                checked = settings.remindersEnabled,
                onCheckedChange = onRemindersChange,
            )
        }
        item {
            ToggleCard(
                title = "Automatic backup",
                body = "Prepare local backup behavior for the future backup flow.",
                checked = settings.backupSettings.autoBackupEnabled,
                onCheckedChange = onAutoBackupChange,
            )
        }
        item {
            ToggleCard(
                title = "Include media in backup",
                body = "Keep handbook PDFs and future media artifacts inside backup scope.",
                checked = settings.backupSettings.includeMediaInBackup,
                onCheckedChange = onIncludeMediaChange,
            )
        }
    }
}

@Composable
private fun FutureFeaturesSection(
    modifier: Modifier,
    featureFlags: Map<FeatureFlag, Boolean>,
) {
    val placeholderRows = listOf(
        PlaceholderFeature(
            title = "Premium",
            body = "Placeholder only. Subscription logic stays disabled in MVP.",
            flag = FeatureFlag.FUTURE_PREMIUM_CONTRACT_ENABLED,
        ),
        PlaceholderFeature(
            title = "Cloud Sync",
            body = "Placeholder only. Sync contracts stay visible without any real network behavior.",
            flag = FeatureFlag.FUTURE_CLOUD_SYNC_CONTRACT_ENABLED,
        ),
        PlaceholderFeature(
            title = "Family Access",
            body = "Placeholder only. Shared household and multi-user controls remain future-ready.",
            flag = FeatureFlag.FUTURE_FAMILY_CONTRACT_ENABLED,
        ),
        PlaceholderFeature(
            title = "Auth Methods",
            body = "Placeholder only. Sign-in methods stay non-functional until a later milestone.",
            flag = FeatureFlag.FUTURE_AUTH_CONTRACT_ENABLED,
        ),
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(placeholderRows) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SectionTitle(text = item.title)
                    Text(text = item.body)
                    Text(
                        text = "Placeholder flag: ${if (featureFlags[item.flag] == true) "Enabled" else "Disabled"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionActionCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(text = title)
            Text(text = body)
            Button(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SectionTitle(text = title)
                Text(text = body)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun <T> ChoiceCard(
    title: String,
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(text = title)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        label = { Text(text = labelFor(option)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

private data class PlaceholderFeature(
    val title: String,
    val body: String,
    val flag: FeatureFlag,
)
