package com.carownerassistant.feature.service

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import com.carownerassistant.core.model.DistanceUnit
import com.carownerassistant.core.model.ServiceDraftValidationResult
import com.carownerassistant.core.model.ServiceEntryDraft
import com.carownerassistant.core.model.ServiceEntrySummary
import com.carownerassistant.core.model.ServicePartItem
import com.carownerassistant.core.model.ServiceRules
import com.carownerassistant.core.model.ServiceWorkItem
import com.carownerassistant.core.model.repository.ServiceRepository
import com.carownerassistant.core.model.repository.VehicleRepository
import java.text.DateFormat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class EditableServiceWorkItem(
    val title: String = "",
    val totalAmount: String = "",
)

data class EditableServicePartItem(
    val title: String = "",
    val quantity: String = "1",
    val totalAmount: String = "",
)

data class ServiceEditorState(
    val serviceId: String? = null,
    val title: String = "",
    val notes: String = "",
    val address: String = "",
    val phone: String = "",
    val contact: String = "",
    val mileageValue: String = "",
    val mileageUnit: DistanceUnit = DistanceUnit.KM,
    val workItems: List<EditableServiceWorkItem> = emptyList(),
    val partItems: List<EditableServicePartItem> = emptyList(),
) {
    companion object {
        fun from(entry: ServiceEntrySummary): ServiceEditorState {
            return ServiceEditorState(
                serviceId = entry.id,
                title = entry.title,
                notes = entry.notes,
                address = entry.address.orEmpty(),
                phone = entry.phone.orEmpty(),
                contact = entry.contact.orEmpty(),
                mileageValue = entry.mileageReading?.km?.toString().orEmpty(),
                mileageUnit = DistanceUnit.KM,
                workItems = entry.workItems.map {
                    EditableServiceWorkItem(
                        title = it.title,
                        totalAmount = it.totalAmount.toString(),
                    )
                },
                partItems = entry.partItems.map {
                    EditableServicePartItem(
                        title = it.title,
                        quantity = it.quantity.toString(),
                        totalAmount = it.totalAmount.toString(),
                    )
                },
            )
        }
    }
}

@Composable
fun ServiceTabScreen(
    vehicleRepository: VehicleRepository,
    serviceRepository: ServiceRepository,
    onOpenMileage: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vehicles by vehicleRepository.observeVehicles().collectAsState(initial = emptyList())
    val activeVehicleId by vehicleRepository.activeVehicleId().collectAsState(initial = null)

    var serviceEntries by remember { mutableStateOf<List<ServiceEntrySummary>>(emptyList()) }
    var editorState by remember { mutableStateOf<ServiceEditorState?>(null) }
    var selectedEntry by remember { mutableStateOf<ServiceEntrySummary?>(null) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeVehicleId) {
        val vehicleId = activeVehicleId
        if (vehicleId == null) {
            serviceEntries = emptyList()
            return@LaunchedEffect
        }

        serviceRepository.observeServices(vehicleId).collectLatest { entries ->
            serviceEntries = entries
        }
    }

    val activeVehicleName = vehicles.firstOrNull { it.isActive }?.displayName ?: "No active car"

    ServiceScreen(
        activeVehicleName = activeVehicleName,
        serviceEntries = serviceEntries,
        validationMessage = validationMessage,
        canCreate = activeVehicleId != null,
        onOpenMileage = onOpenMileage,
        onAddService = {
            editorState = ServiceEditorState(
                workItems = listOf(EditableServiceWorkItem()),
            )
            validationMessage = null
        },
        onEditService = {
            editorState = ServiceEditorState.from(it)
            validationMessage = null
        },
        onViewService = { selectedEntry = it },
    )

    editorState?.let { state ->
        ServiceEditorDialog(
            state = state,
            validationMessage = validationMessage,
            onDismiss = {
                editorState = null
                validationMessage = null
            },
            onStateChange = { editorState = it },
            onConfirm = {
                val activeVehicle = activeVehicleId
                val workItems = state.workItems.mapNotNull { item ->
                    val amount = item.totalAmount.toDoubleOrNull()
                    if (item.title.isBlank() && item.totalAmount.isBlank()) {
                        null
                    } else {
                        ServiceWorkItem(
                            title = item.title,
                            totalAmount = amount ?: Double.NaN,
                        )
                    }
                }
                val partItems = state.partItems.mapNotNull { item ->
                    val quantity = item.quantity.toIntOrNull()
                    val amount = item.totalAmount.toDoubleOrNull()
                    if (item.title.isBlank() && item.quantity.isBlank() && item.totalAmount.isBlank()) {
                        null
                    } else {
                        ServicePartItem(
                            title = item.title,
                            quantity = quantity ?: -1,
                            totalAmount = amount ?: Double.NaN,
                        )
                    }
                }
                val mileageValue = state.mileageValue.toDoubleOrNull()

                val validation = ServiceRules.validateDraft(
                    title = state.title,
                    workItems = workItems,
                    partItems = partItems,
                    mileageValue = if (state.mileageValue.isBlank()) null else mileageValue,
                    mileageUnit = if (state.mileageValue.isBlank()) null else state.mileageUnit,
                )

                when {
                    activeVehicle == null -> {
                        validationMessage = "Create or select an active car before logging service."
                    }

                    validation is ServiceDraftValidationResult.Invalid -> {
                        validationMessage = validation.message
                    }

                    else -> {
                        val draft = ServiceEntryDraft(
                            vehicleId = activeVehicle,
                            timestampEpochMillis = System.currentTimeMillis(),
                            title = state.title,
                            notes = state.notes,
                            address = state.address,
                            phone = state.phone,
                            contact = state.contact,
                            workItems = workItems,
                            partItems = partItems,
                            mileageValue = if (state.mileageValue.isBlank()) null else mileageValue,
                            mileageUnit = if (state.mileageValue.isBlank()) null else state.mileageUnit,
                        )
                        scope.launch {
                            if (state.serviceId == null) {
                                serviceRepository.createServiceEntry(draft)
                            } else {
                                serviceRepository.updateServiceEntry(state.serviceId, draft)
                            }
                            editorState = null
                            validationMessage = null
                        }
                    }
                }
            },
        )
    }

    selectedEntry?.let { entry ->
        ServiceDetailDialog(
            entry = entry,
            onDismiss = { selectedEntry = null },
            onOpenDialer = {
                ServiceRules.dialUri(entry.phone)?.let { dialUri ->
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(dialUri)))
                }
            },
            onOpenMaps = {
                ServiceRules.mapsUri(entry.address)?.let { mapsUri ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUri)))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceScreen(
    activeVehicleName: String,
    serviceEntries: List<ServiceEntrySummary>,
    validationMessage: String?,
    canCreate: Boolean,
    onOpenMileage: () -> Unit,
    onAddService: () -> Unit,
    onEditService: (ServiceEntrySummary) -> Unit,
    onViewService: (ServiceEntrySummary) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Service") },
                actions = {
                    TextButton(onClick = onOpenMileage) {
                        Text(text = "Mileage")
                    }
                },
            )
        },
        floatingActionButton = {
            if (canCreate) {
                FloatingActionButton(onClick = onAddService) {
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
                        Text(text = "Completed service records only in this flow.")
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

            if (serviceEntries.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No completed service records yet.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            items(serviceEntries, key = { it.id }) { entry ->
                ServiceEntryCard(
                    entry = entry,
                    onEdit = { onEditService(entry) },
                    onView = { onViewService(entry) },
                )
            }
        }
    }
}

@Composable
private fun ServiceEntryCard(
    entry: ServiceEntrySummary,
    onEdit: () -> Unit,
    onView: () -> Unit,
) {
    val mileageReading = entry.mileageReading
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = formatTimestamp(entry.timestampEpochMillis))
            Text(text = "Total: ${entry.totalAmount}")
            Text(text = "Works: ${entry.workItems.size} • Parts: ${entry.partItems.size}")
            Text(
                text = if (mileageReading != null) {
                    "Mileage linked: ${mileageReading.km} km / ${mileageReading.mi} mi"
                } else {
                    "Mileage linked: No"
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onView) {
                    Text(text = "View")
                }
                TextButton(onClick = onEdit) {
                    Text(text = "Edit")
                }
            }
        }
    }
}

@Composable
private fun ServiceEditorDialog(
    state: ServiceEditorState,
    validationMessage: String?,
    onDismiss: () -> Unit,
    onStateChange: (ServiceEditorState) -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (state.serviceId == null) "Add Service Record" else "Edit Service Record") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        validationMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        OutlinedTextField(
                            value = state.title,
                            onValueChange = { onStateChange(state.copy(title = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = "Service title") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = { onStateChange(state.copy(notes = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = "Notes") },
                        )
                        OutlinedTextField(
                            value = state.address,
                            onValueChange = { onStateChange(state.copy(address = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = "Address (optional)") },
                        )
                        OutlinedTextField(
                            value = state.phone,
                            onValueChange = { onStateChange(state.copy(phone = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = "Phone (optional)") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.contact,
                            onValueChange = { onStateChange(state.copy(contact = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = "Contact (optional)") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.mileageValue,
                            onValueChange = { onStateChange(state.copy(mileageValue = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = "Service mileage (optional)") },
                            singleLine = true,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onStateChange(state.copy(mileageUnit = DistanceUnit.KM)) }) {
                                Text(text = if (state.mileageUnit == DistanceUnit.KM) "KM selected" else "Use KM")
                            }
                            TextButton(onClick = { onStateChange(state.copy(mileageUnit = DistanceUnit.MI)) }) {
                                Text(text = if (state.mileageUnit == DistanceUnit.MI) "MI selected" else "Use MI")
                            }
                        }
                        Text(
                            text = "Work items",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                items(state.workItems.indices.toList()) { index ->
                    val item = state.workItems[index]
                    ServiceWorkItemEditor(
                        item = item,
                        onItemChange = { updated ->
                            onStateChange(state.copy(workItems = state.workItems.updated(index, updated)))
                        },
                        onRemove = {
                            onStateChange(state.copy(workItems = state.workItems.removeIndex(index)))
                        },
                    )
                }

                item {
                    TextButton(
                        onClick = {
                            onStateChange(state.copy(workItems = state.workItems + EditableServiceWorkItem()))
                        },
                    ) {
                        Text(text = "Add work item")
                    }
                    Text(
                        text = "Part items",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                items(state.partItems.indices.toList()) { index ->
                    val item = state.partItems[index]
                    ServicePartItemEditor(
                        item = item,
                        onItemChange = { updated ->
                            onStateChange(state.copy(partItems = state.partItems.updated(index, updated)))
                        },
                        onRemove = {
                            onStateChange(state.copy(partItems = state.partItems.removeIndex(index)))
                        },
                    )
                }

                item {
                    TextButton(
                        onClick = {
                            onStateChange(state.copy(partItems = state.partItems + EditableServicePartItem()))
                        },
                    ) {
                        Text(text = "Add part item")
                    }
                    val total = state.workItems.sumOf { it.totalAmount.toDoubleOrNull() ?: 0.0 } +
                        state.partItems.sumOf { it.totalAmount.toDoubleOrNull() ?: 0.0 }
                    Text(text = "Computed total: $total")
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

@Composable
private fun ServiceWorkItemEditor(
    item: EditableServiceWorkItem,
    onItemChange: (EditableServiceWorkItem) -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = item.title,
                onValueChange = { onItemChange(item.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Work item") },
                singleLine = true,
            )
            OutlinedTextField(
                value = item.totalAmount,
                onValueChange = { onItemChange(item.copy(totalAmount = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Work cost") },
                singleLine = true,
            )
            TextButton(onClick = onRemove) {
                Text(text = "Remove work item")
            }
        }
    }
}

@Composable
private fun ServicePartItemEditor(
    item: EditableServicePartItem,
    onItemChange: (EditableServicePartItem) -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = item.title,
                onValueChange = { onItemChange(item.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Part item") },
                singleLine = true,
            )
            OutlinedTextField(
                value = item.quantity,
                onValueChange = { onItemChange(item.copy(quantity = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Quantity") },
                singleLine = true,
            )
            OutlinedTextField(
                value = item.totalAmount,
                onValueChange = { onItemChange(item.copy(totalAmount = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Part cost") },
                singleLine = true,
            )
            TextButton(onClick = onRemove) {
                Text(text = "Remove part item")
            }
        }
    }
}

@Composable
private fun ServiceDetailDialog(
    entry: ServiceEntrySummary,
    onDismiss: () -> Unit,
    onOpenDialer: () -> Unit,
    onOpenMaps: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = entry.title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(text = formatTimestamp(entry.timestampEpochMillis)) }
                item { Text(text = "Total: ${entry.totalAmount}") }
                entry.mileageReading?.let {
                    item { Text(text = "Mileage: ${it.km} km / ${it.mi} mi") }
                }
                if (!entry.notes.isBlank()) {
                    item { Text(text = entry.notes) }
                }
                entry.contact?.takeIf { it.isNotBlank() }?.let {
                    item { Text(text = "Contact: $it") }
                }
                entry.phone?.takeIf { it.isNotBlank() }?.let {
                    item {
                        TextButton(onClick = onOpenDialer) {
                            Text(text = "Call $it")
                        }
                    }
                }
                entry.address?.takeIf { it.isNotBlank() }?.let {
                    item {
                        TextButton(onClick = onOpenMaps) {
                            Text(text = "Open maps")
                        }
                    }
                }
                item {
                    Text(
                        text = "Work items",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(entry.workItems) { item ->
                    Text(text = "- ${item.title}: ${item.totalAmount}")
                }
                item {
                    Text(
                        text = "Part items",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(entry.partItems) { item ->
                    Text(text = "- ${item.title} x${item.quantity}: ${item.totalAmount}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        },
    )
}

private fun formatTimestamp(timestampEpochMillis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(timestampEpochMillis)
}

private fun <T> List<T>.updated(index: Int, item: T): List<T> {
    return toMutableList().also { it[index] = item }
}

private fun <T> List<T>.removeIndex(index: Int): List<T> {
    return toMutableList().also { it.removeAt(index) }
}
