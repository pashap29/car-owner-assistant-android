package com.carownerassistant.feature.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carownerassistant.core.model.AppStartMode
import com.carownerassistant.core.model.VehicleSummary
import com.carownerassistant.core.model.repository.SettingsRepository
import com.carownerassistant.core.model.repository.VehicleRepository
import kotlinx.coroutines.launch

data class VehicleEditorState(
    val vehicleId: String? = null,
    val displayName: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val plateNumber: String = "",
    val makeActive: Boolean = false,
) {
    companion object {
        fun from(vehicle: VehicleSummary): VehicleEditorState {
            return VehicleEditorState(
                vehicleId = vehicle.id,
                displayName = vehicle.displayName,
                make = vehicle.make,
                model = vehicle.model,
                year = vehicle.year?.toString().orEmpty(),
                plateNumber = vehicle.plateNumber,
                makeActive = vehicle.isActive,
            )
        }
    }
}

@Composable
fun GarageRoute(
    vehicleRepository: VehicleRepository,
    settingsRepository: SettingsRepository,
    launchedAsStartDestination: Boolean,
    onContinueToMain: () -> Unit,
    onBack: () -> Unit,
) {
    val vehicles by vehicleRepository.observeVehicles().collectAsState(initial = emptyList())
    val appStartMode by settingsRepository.appStartMode().collectAsState(initial = AppStartMode.ACTIVE_CAR)
    val scope = rememberCoroutineScope()

    var editorState by remember { mutableStateOf<VehicleEditorState?>(null) }
    var vehiclePendingDelete by remember { mutableStateOf<VehicleSummary?>(null) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    GarageScreen(
        vehicles = vehicles,
        appStartMode = appStartMode,
        launchedAsStartDestination = launchedAsStartDestination,
        validationMessage = validationMessage,
        onAddVehicle = { editorState = VehicleEditorState(makeActive = vehicles.isEmpty()) },
        onEditVehicle = { vehicle -> editorState = VehicleEditorState.from(vehicle) },
        onActivateVehicle = { vehicle ->
            scope.launch { vehicleRepository.setActiveVehicle(vehicle.id) }
        },
        onDeleteVehicle = { vehicle -> vehiclePendingDelete = vehicle },
        onChangeAppStartMode = { mode ->
            scope.launch { settingsRepository.setAppStartMode(mode) }
        },
        onContinueToMain = onContinueToMain,
        onBack = onBack,
    )

    editorState?.let { state ->
        VehicleEditorDialog(
            state = state,
            onDismiss = {
                editorState = null
                validationMessage = null
            },
            onStateChange = { editorState = it },
            onConfirm = {
                when (val validation = VehicleDraftValidator.validate(state.displayName, state.year)) {
                    is VehicleDraftValidationResult.Invalid -> validationMessage = validation.message
                    VehicleDraftValidationResult.Valid -> {
                        val draft = VehicleDraftValidator.toDraft(state)
                        scope.launch {
                            if (state.vehicleId == null) {
                                vehicleRepository.createVehicle(draft)
                            } else {
                                vehicleRepository.updateVehicle(state.vehicleId, draft)
                            }
                            validationMessage = null
                            editorState = null
                        }
                    }
                }
            },
        )
    }

    vehiclePendingDelete?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { vehiclePendingDelete = null },
            title = { Text(text = "Delete car") },
            text = { Text(text = "Delete ${vehicle.displayName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { vehicleRepository.deleteVehicle(vehicle.id) }
                        vehiclePendingDelete = null
                    },
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { vehiclePendingDelete = null }) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GarageScreen(
    vehicles: List<VehicleSummary>,
    appStartMode: AppStartMode,
    launchedAsStartDestination: Boolean,
    validationMessage: String?,
    onAddVehicle: () -> Unit,
    onEditVehicle: (VehicleSummary) -> Unit,
    onActivateVehicle: (VehicleSummary) -> Unit,
    onDeleteVehicle: (VehicleSummary) -> Unit,
    onChangeAppStartMode: (AppStartMode) -> Unit,
    onContinueToMain: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Garage") },
                navigationIcon = {
                    if (!launchedAsStartDestination) {
                        TextButton(onClick = onBack) {
                            Text(text = "Back")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVehicle) {
                Text(text = "+")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StartupModeCard(
                    selectedMode = appStartMode,
                    onModeSelected = onChangeAppStartMode,
                    launchedAsStartDestination = launchedAsStartDestination,
                    canContinue = vehicles.isNotEmpty(),
                    onContinueToMain = onContinueToMain,
                )
            }

            validationMessage?.let { message ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (vehicles.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No cars yet. Add your first car to continue.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            items(vehicles, key = { it.id }) { vehicle ->
                VehicleCard(
                    vehicle = vehicle,
                    onEditVehicle = { onEditVehicle(vehicle) },
                    onActivateVehicle = { onActivateVehicle(vehicle) },
                    onDeleteVehicle = { onDeleteVehicle(vehicle) },
                )
            }
        }
    }
}

@Composable
private fun StartupModeCard(
    selectedMode: AppStartMode,
    onModeSelected: (AppStartMode) -> Unit,
    launchedAsStartDestination: Boolean,
    canContinue: Boolean,
    onContinueToMain: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "App start mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedMode == AppStartMode.ACTIVE_CAR,
                    onClick = { onModeSelected(AppStartMode.ACTIVE_CAR) },
                    label = { Text(text = "Open active car") },
                )
                FilterChip(
                    selected = selectedMode == AppStartMode.GARAGE,
                    onClick = { onModeSelected(AppStartMode.GARAGE) },
                    label = { Text(text = "Open garage") },
                )
            }
            if (launchedAsStartDestination) {
                Button(
                    onClick = onContinueToMain,
                    enabled = canContinue,
                ) {
                    Text(text = "Continue to app")
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: VehicleSummary,
    onEditVehicle: () -> Unit,
    onActivateVehicle: () -> Unit,
    onDeleteVehicle: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = vehicle.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val details = listOfNotNull(
                        vehicle.make.takeIf { it.isNotBlank() },
                        vehicle.model.takeIf { it.isNotBlank() },
                        vehicle.year?.toString(),
                        vehicle.plateNumber.takeIf { it.isNotBlank() },
                    ).joinToString(" • ")

                    if (details.isNotBlank()) {
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (vehicle.isActive) {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text(text = "Active") },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!vehicle.isActive) {
                    Button(onClick = onActivateVehicle) {
                        Text(text = "Make active")
                    }
                }
                TextButton(onClick = onEditVehicle) {
                    Text(text = "Edit")
                }
                TextButton(onClick = onDeleteVehicle) {
                    Text(text = "Delete")
                }
            }
        }
    }
}

@Composable
private fun VehicleEditorDialog(
    state: VehicleEditorState,
    onDismiss: () -> Unit,
    onStateChange: (VehicleEditorState) -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (state.vehicleId == null) "Add car" else "Edit car")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = { onStateChange(state.copy(displayName = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Display name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.make,
                    onValueChange = { onStateChange(state.copy(make = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Make") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.model,
                    onValueChange = { onStateChange(state.copy(model = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Model") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.year,
                    onValueChange = { onStateChange(state.copy(year = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Year") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.plateNumber,
                    onValueChange = { onStateChange(state.copy(plateNumber = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Plate number") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = state.makeActive,
                        onCheckedChange = { onStateChange(state.copy(makeActive = it)) },
                    )
                    Text(text = "Set as active car")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}
