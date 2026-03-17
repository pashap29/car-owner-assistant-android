package com.carownerassistant.feature.mileage

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carownerassistant.core.files.AppFileStore
import com.carownerassistant.core.files.FileBucket
import com.carownerassistant.core.model.DistanceUnit
import com.carownerassistant.core.model.MileageEntryDraft
import com.carownerassistant.core.model.MileageEntryOrigin
import com.carownerassistant.core.model.MileageEntrySummary
import com.carownerassistant.core.model.MileageLedgerSummary
import com.carownerassistant.core.model.MileageStatus
import com.carownerassistant.core.model.repository.MileageRepository
import com.carownerassistant.core.model.repository.VehicleRepository
import java.io.File
import java.text.DateFormat
import java.util.UUID
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val EmptyMileageLedger = MileageLedgerSummary(
    entries = emptyList(),
    anomalyLog = emptyList(),
    overallTrustScore = 0,
)

data class MileageEditorState(
    val value: String = "",
    val inputUnit: DistanceUnit = DistanceUnit.KM,
    val photoFilePath: String? = null,
)

@Composable
fun MileageRoute(
    vehicleRepository: VehicleRepository,
    mileageRepository: MileageRepository,
    appFileStore: AppFileStore,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val vehicles by vehicleRepository.observeVehicles().collectAsState(initial = emptyList())
    val activeVehicleId by vehicleRepository.activeVehicleId().collectAsState(initial = null)

    var ledger by remember { mutableStateOf(EmptyMileageLedger) }
    var editorState by remember { mutableStateOf<MileageEditorState?>(null) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeVehicleId) {
        val vehicleId = activeVehicleId
        if (vehicleId == null) {
            ledger = EmptyMileageLedger
            return@LaunchedEffect
        }

        mileageRepository.observeMileageLedger(vehicleId).collectLatest { summary ->
            ledger = summary
        }
    }

    val activeVehicleName = vehicles.firstOrNull { it.isActive }?.displayName ?: "No active car"

    MileageScreen(
        activeVehicleName = activeVehicleName,
        ledger = ledger,
        validationMessage = validationMessage,
        canCreateEntry = activeVehicleId != null,
        onBack = onBack,
        onAddEntry = {
            editorState = MileageEditorState()
            validationMessage = null
        },
    )

    editorState?.let { state ->
        MileageEntryDialog(
            state = state,
            appFileStore = appFileStore,
            onDismiss = {
                editorState = null
                validationMessage = null
            },
            onStateChange = { editorState = it },
            onConfirm = {
                val vehicleId = activeVehicleId
                val mileageValue = state.value.toDoubleOrNull()
                if (vehicleId == null) {
                    validationMessage = "Create or select an active car before logging mileage."
                } else if (mileageValue == null || mileageValue <= 0.0) {
                    validationMessage = "Enter a valid manual mileage value."
                } else {
                    scope.launch {
                        mileageRepository.addMileageEntry(
                            MileageEntryDraft(
                                vehicleId = vehicleId,
                                timestampEpochMillis = System.currentTimeMillis(),
                                value = mileageValue,
                                inputUnit = state.inputUnit,
                                photoFilePath = state.photoFilePath,
                                origin = MileageEntryOrigin.DEDICATED_MILEAGE,
                                manualEntry = true,
                            ),
                        )
                        editorState = null
                        validationMessage = null
                    }
                }
            },
            onPickPhoto = { filePath ->
                editorState = state.copy(photoFilePath = filePath)
            },
            context = context,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MileageScreen(
    activeVehicleName: String,
    ledger: MileageLedgerSummary,
    validationMessage: String?,
    canCreateEntry: Boolean,
    onBack: () -> Unit,
    onAddEntry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Mileage Ledger") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            if (canCreateEntry) {
                FloatingActionButton(onClick = onAddEntry) {
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
                        Text(text = "Trust score: ${ledger.overallTrustScore}/100")
                        Text(text = "History size: ${ledger.entries.size}")
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

            item {
                SectionCard(title = "Anomaly Log") {
                    if (ledger.anomalyLog.isEmpty()) {
                        Text(text = "No anomalies detected yet.")
                    } else {
                        ledger.anomalyLog.forEach { anomaly ->
                            Text(text = "- ${formatTimestamp(anomaly.timestampEpochMillis)} - ${anomaly.message}")
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Mileage History") {
                    if (ledger.entries.isEmpty()) {
                        Text(text = "No mileage entries yet. Add your first odometer reading.")
                    }
                }
            }

            items(ledger.entries, key = { it.id }) { entry ->
                MileageHistoryCard(entry = entry)
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun MileageHistoryCard(entry: MileageEntrySummary) {
    val statusLabel = when (entry.status) {
        MileageStatus.VERIFIED -> "Verified"
        MileageStatus.UNVERIFIED -> "Unverified"
        MileageStatus.CONFLICTED -> "Conflicted"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "${entry.reading.km} km / ${entry.reading.mi} mi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = formatTimestamp(entry.timestampEpochMillis))
            Text(text = "Status: $statusLabel")
            Text(text = "Trust score: ${entry.trustScore}/100")
            Text(text = "Photo attached: ${if (entry.photoFilePath != null) "Yes" else "No"}")
            if (entry.largeJumpSuspected) {
                Text(
                    text = "Large jump suspected",
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (entry.criticalAnomaly) {
                Text(
                    text = "Critical anomaly detected",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MileageEntryDialog(
    state: MileageEditorState,
    appFileStore: AppFileStore,
    onDismiss: () -> Unit,
    onStateChange: (MileageEditorState) -> Unit,
    onConfirm: () -> Unit,
    onPickPhoto: (String?) -> Unit,
    context: Context,
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val copiedFilePath = uri?.let {
            copyMileagePhotoToAppStorage(
                context = context,
                appFileStore = appFileStore,
                sourceUri = it,
            )
        }
        onPickPhoto(copiedFilePath)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Mileage Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.value,
                    onValueChange = { onStateChange(state.copy(value = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Manual odometer value") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onStateChange(state.copy(inputUnit = DistanceUnit.KM)) }) {
                        Text(text = if (state.inputUnit == DistanceUnit.KM) "KM selected" else "Use KM")
                    }
                    Button(onClick = { onStateChange(state.copy(inputUnit = DistanceUnit.MI)) }) {
                        Text(text = if (state.inputUnit == DistanceUnit.MI) "MI selected" else "Use MI")
                    }
                }
                Text(text = "Photo flow is optional for save, but required for verified mileage.")
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) {
                    Text(text = if (state.photoFilePath == null) "Attach odometer photo" else "Replace odometer photo")
                }
                Text(
                    text = state.photoFilePath?.let { "Photo attached" } ?: "No photo attached",
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

private fun copyMileagePhotoToAppStorage(
    context: Context,
    appFileStore: AppFileStore,
    sourceUri: Uri,
): String? {
    val targetDirectory = appFileStore.bucket(FileBucket.MEDIA_MILEAGE)
    val targetFile = File(targetDirectory, "${UUID.randomUUID()}.jpg")

    return runCatching {
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            targetFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: return null

        targetFile.absolutePath
    }.getOrNull()
}

private fun formatTimestamp(timestampEpochMillis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(timestampEpochMillis)
}
