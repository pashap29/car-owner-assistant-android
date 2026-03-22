package com.carownerassistant.feature.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import com.carownerassistant.core.model.DistanceUnit
import com.carownerassistant.core.model.FuelDraftValidationResult
import com.carownerassistant.core.model.FuelEntryDraft
import com.carownerassistant.core.model.FuelEntryMethod
import com.carownerassistant.core.model.FuelEntrySummary
import com.carownerassistant.core.model.FuelQrParser
import com.carownerassistant.core.model.FuelRules
import com.carownerassistant.core.model.FuelType
import com.carownerassistant.core.model.MileageEntrySummary
import com.carownerassistant.core.model.MileageUnitConverter
import com.carownerassistant.core.model.repository.FuelRepository
import com.carownerassistant.core.model.repository.MileageRepository
import com.carownerassistant.core.model.repository.VehicleRepository
import java.text.DateFormat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val SystemFuelTypes = FuelType.entries

data class FuelEditorState(
    val liters: String = "",
    val totalAmount: String = "",
    val fuelType: FuelType = FuelType.GASOLINE_95,
    val isFullTank: Boolean = true,
    val odometerValue: String = "",
    val odometerUnit: DistanceUnit = DistanceUnit.KM,
    val timestampEpochMillis: Long = System.currentTimeMillis(),
    val entryMethod: FuelEntryMethod = FuelEntryMethod.MANUAL,
    val qrPayloadRaw: String = "",
)

@Composable
fun FuelTabScreen(
    vehicleRepository: VehicleRepository,
    fuelRepository: FuelRepository,
    mileageRepository: MileageRepository,
    onOpenMileage: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicles by vehicleRepository.observeVehicles().collectAsState(initial = emptyList())
    val activeVehicleId by vehicleRepository.activeVehicleId().collectAsState(initial = null)

    var fuelEntries by remember { mutableStateOf<List<FuelEntrySummary>>(emptyList()) }
    var mileageEntries by remember { mutableStateOf<List<MileageEntrySummary>>(emptyList()) }
    var editorState by remember { mutableStateOf<FuelEditorState?>(null) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeVehicleId) {
        val vehicleId = activeVehicleId
        if (vehicleId == null) {
            fuelEntries = emptyList()
            return@LaunchedEffect
        }

        fuelRepository.observeFuel(vehicleId).collectLatest { entries ->
            fuelEntries = entries
        }
    }

    LaunchedEffect(activeVehicleId) {
        val vehicleId = activeVehicleId
        if (vehicleId == null) {
            mileageEntries = emptyList()
            return@LaunchedEffect
        }

        mileageRepository.observeMileage(vehicleId).collectLatest { entries ->
            mileageEntries = entries
        }
    }

    val activeVehicleName = vehicles.firstOrNull { it.isActive }?.displayName ?: "No active car"
    val suggestedFuelType = remember(fuelEntries) { FuelRules.suggestFuelType(fuelEntries) }

    FuelScreen(
        activeVehicleName = activeVehicleName,
        suggestedFuelType = suggestedFuelType,
        fuelEntries = fuelEntries,
        validationMessage = validationMessage,
        canCreateFuel = activeVehicleId != null,
        onAddFuel = {
            editorState = FuelEditorState(
                fuelType = suggestedFuelType ?: FuelType.GASOLINE_95,
            )
            validationMessage = null
        },
        onOpenMileage = onOpenMileage,
    )

    editorState?.let { state ->
        val mileageDeltaKm = remember(state.odometerValue, state.odometerUnit, state.timestampEpochMillis, mileageEntries) {
            state.odometerValue.toDoubleOrNull()?.let { value ->
                val previousMileageKm = mileageEntries
                    .lastOrNull { it.timestampEpochMillis <= state.timestampEpochMillis }
                    ?.reading
                    ?.km
                FuelRules.computeMileageDeltaKm(
                    previousMileageKm = previousMileageKm,
                    newReading = MileageUnitConverter.toReading(
                        value = value,
                        inputUnit = state.odometerUnit,
                    ),
                )
            }
        }

        FuelEntryDialog(
            state = state,
            suggestedFuelType = suggestedFuelType,
            mileageDeltaKm = mileageDeltaKm,
            onDismiss = {
                editorState = null
                validationMessage = null
            },
            onStateChange = { editorState = it },
            onApplyQrPrefill = {
                val prefill = FuelQrParser.parse(state.qrPayloadRaw)
                if (prefill.timestampEpochMillis == null && prefill.totalAmount == null) {
                    validationMessage = "QR assist could not extract date or total amount from this payload."
                } else {
                    editorState = state.copy(
                        timestampEpochMillis = prefill.timestampEpochMillis ?: state.timestampEpochMillis,
                        totalAmount = prefill.totalAmount?.toString() ?: state.totalAmount,
                        entryMethod = FuelEntryMethod.QR_ASSISTED,
                    )
                    validationMessage = null
                }
            },
            onConfirm = {
                val vehicleId = activeVehicleId
                val liters = state.liters.toDoubleOrNull()
                val totalAmount = state.totalAmount.toDoubleOrNull()
                val odometerValue = state.odometerValue.toDoubleOrNull()
                val validation = FuelRules.validateDraft(
                    liters = liters,
                    totalAmount = totalAmount,
                    odometerValue = odometerValue,
                    odometerUnit = state.odometerUnit,
                )

                when {
                    vehicleId == null -> {
                        validationMessage = "Create or select an active car before logging fuel."
                    }

                    validation is FuelDraftValidationResult.Invalid -> {
                        validationMessage = validation.message
                    }

                    else -> {
                        scope.launch {
                            fuelRepository.createFuelEntry(
                                FuelEntryDraft(
                                    vehicleId = vehicleId,
                                    timestampEpochMillis = state.timestampEpochMillis,
                                    liters = liters!!,
                                    totalAmount = totalAmount!!,
                                    fuelType = state.fuelType,
                                    isFullTank = state.isFullTank,
                                    odometerValue = odometerValue,
                                    odometerUnit = state.odometerUnit,
                                    entryMethod = state.entryMethod,
                                    qrPayloadRaw = state.qrPayloadRaw.ifBlank { null },
                                ),
                            )
                            editorState = null
                            validationMessage = null
                        }
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelScreen(
    activeVehicleName: String,
    suggestedFuelType: FuelType?,
    fuelEntries: List<FuelEntrySummary>,
    validationMessage: String?,
    canCreateFuel: Boolean,
    onAddFuel: () -> Unit,
    onOpenMileage: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Fuel") },
                actions = {
                    TextButton(onClick = onOpenMileage) {
                        Text(text = "Mileage")
                    }
                },
            )
        },
        floatingActionButton = {
            if (canCreateFuel) {
                FloatingActionButton(onClick = onAddFuel) {
                    Text(text = "+")
                }
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = activeVehicleName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = suggestedFuelType?.let {
                                "Suggested fuel type from history: ${it.displayLabel()}"
                            } ?: "No prior fuel history yet. Your first entry will seed future suggestions.",
                        )
                        Text(
                            text = "QR assist in MVP pre-fills only date and total amount. Odometer stays manual.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
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

            if (fuelEntries.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No fuel entries yet. Add the first fueling for the active car.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            items(fuelEntries, key = { it.id }) { entry ->
                FuelEntryCard(entry = entry)
            }
        }
    }
}

@Composable
private fun FuelEntryCard(
    entry: FuelEntrySummary,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "${entry.liters} L • ${entry.totalAmount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = formatTimestamp(entry.timestampEpochMillis))
            Text(text = "Fuel type: ${entry.fuelType.displayLabel()}")
            Text(text = "Entry method: ${entry.entryMethod.displayLabel()}")
            Text(text = if (entry.isFullTank) "Full tank" else "Partial fuel-up")
            Text(
                text = entry.odometerReading?.let {
                    "Mileage linked: ${it.km} km / ${it.mi} mi"
                } ?: "Mileage linked: No",
            )
            Text(
                text = entry.mileageDeltaKm?.let { "Mileage delta: $it km" } ?: "Mileage delta: first linked reading",
            )
            Text(
                text = if (entry.qrPayloadRaw != null) "QR assist used" else "Manual entry",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FuelEntryDialog(
    state: FuelEditorState,
    suggestedFuelType: FuelType?,
    mileageDeltaKm: Double?,
    onDismiss: () -> Unit,
    onStateChange: (FuelEditorState) -> Unit,
    onApplyQrPrefill: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Fuel Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestedFuelType?.let {
                    Text(
                        text = "Auto-suggested from prior records: ${it.displayLabel()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedTextField(
                    value = state.liters,
                    onValueChange = { onStateChange(state.copy(liters = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Liters") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.totalAmount,
                    onValueChange = { onStateChange(state.copy(totalAmount = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Total amount") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.odometerValue,
                    onValueChange = { onStateChange(state.copy(odometerValue = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "New odometer value") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onStateChange(state.copy(odometerUnit = DistanceUnit.KM)) }) {
                        Text(text = if (state.odometerUnit == DistanceUnit.KM) "KM selected" else "Use KM")
                    }
                    TextButton(onClick = { onStateChange(state.copy(odometerUnit = DistanceUnit.MI)) }) {
                        Text(text = if (state.odometerUnit == DistanceUnit.MI) "MI selected" else "Use MI")
                    }
                }
                Text(
                    text = mileageDeltaKm?.let { "Computed mileage delta: $it km" }
                        ?: "Mileage delta will be computed from the latest prior mileage when possible.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(
                    onClick = { onStateChange(state.copy(isFullTank = !state.isFullTank)) },
                ) {
                    Text(text = if (state.isFullTank) "Full tank selected" else "Mark as full tank")
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SystemFuelTypes) { fuelType ->
                        FilterChip(
                            selected = state.fuelType == fuelType,
                            onClick = { onStateChange(state.copy(fuelType = fuelType)) },
                            label = { Text(text = fuelType.displayLabel()) },
                        )
                    }
                }
                OutlinedTextField(
                    value = state.qrPayloadRaw,
                    onValueChange = { onStateChange(state.copy(qrPayloadRaw = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "QR payload (optional)") },
                    supportingText = {
                        Text(text = "Supported MVP prefill fields: date and total amount.")
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onApplyQrPrefill) {
                        Text(text = "Apply QR prefill")
                    }
                    TextButton(
                        onClick = {
                            onStateChange(
                                state.copy(
                                    timestampEpochMillis = System.currentTimeMillis(),
                                    entryMethod = FuelEntryMethod.MANUAL,
                                ),
                            )
                        },
                    ) {
                        Text(text = "Use current time")
                    }
                }
                Text(
                    text = "Entry date: ${formatTimestamp(state.timestampEpochMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                )
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

private fun FuelType.displayLabel(): String {
    return when (this) {
        FuelType.GASOLINE_92 -> "AI-92"
        FuelType.GASOLINE_95 -> "AI-95"
        FuelType.GASOLINE_98 -> "AI-98"
        FuelType.DIESEL -> "Diesel"
        FuelType.LPG -> "LPG"
        FuelType.CNG -> "CNG"
        FuelType.OTHER -> "Other"
    }
}

private fun FuelEntryMethod.displayLabel(): String {
    return when (this) {
        FuelEntryMethod.MANUAL -> "Manual"
        FuelEntryMethod.QR_ASSISTED -> "QR assisted"
    }
}

private fun formatTimestamp(timestampEpochMillis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(timestampEpochMillis)
}
