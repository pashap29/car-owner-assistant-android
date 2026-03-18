package com.carownerassistant.feature.expense

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carownerassistant.core.files.AppFileStore
import com.carownerassistant.core.files.FileBucket
import com.carownerassistant.core.files.compressImageToJpeg
import com.carownerassistant.core.model.DistanceUnit
import com.carownerassistant.core.model.ExpenseCategory
import com.carownerassistant.core.model.ExpenseDraftValidationResult
import com.carownerassistant.core.model.ExpenseEntryDraft
import com.carownerassistant.core.model.ExpenseEntrySummary
import com.carownerassistant.core.model.ExpenseFilter
import com.carownerassistant.core.model.ExpenseRules
import com.carownerassistant.core.model.repository.ExpenseRepository
import com.carownerassistant.core.model.repository.VehicleRepository
import java.io.File
import java.text.DateFormat
import java.util.UUID
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val SystemExpenseCategories = ExpenseCategory.entries

data class ExpenseEditorState(
    val expenseId: String? = null,
    val amount: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val note: String = "",
    val mileageValue: String = "",
    val mileageUnit: DistanceUnit = DistanceUnit.KM,
    val attachmentPath: String? = null,
) {
    companion object {
        fun from(entry: ExpenseEntrySummary): ExpenseEditorState {
            return ExpenseEditorState(
                expenseId = entry.id,
                amount = entry.totalAmount.toString(),
                category = entry.category,
                note = entry.note,
                mileageValue = entry.mileageReading?.km?.toString().orEmpty(),
                mileageUnit = DistanceUnit.KM,
                attachmentPath = entry.attachmentPath,
            )
        }
    }
}

@Composable
fun ExpenseTabScreen(
    vehicleRepository: VehicleRepository,
    expenseRepository: ExpenseRepository,
    appFileStore: AppFileStore,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val vehicles by vehicleRepository.observeVehicles().collectAsState(initial = emptyList())
    val activeVehicleId by vehicleRepository.activeVehicleId().collectAsState(initial = null)

    var allExpenses by remember { mutableStateOf<List<ExpenseEntrySummary>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var editorState by remember { mutableStateOf<ExpenseEditorState?>(null) }
    var pendingDelete by remember { mutableStateOf<ExpenseEntrySummary?>(null) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeVehicleId) {
        val vehicleId = activeVehicleId
        if (vehicleId == null) {
            allExpenses = emptyList()
            return@LaunchedEffect
        }

        expenseRepository.observeExpenses(vehicleId).collectLatest { entries ->
            allExpenses = entries
        }
    }

    val filteredExpenses = ExpenseRules.applyFilter(
        entries = allExpenses,
        filter = ExpenseFilter(
            query = searchQuery,
            category = selectedCategory,
        ),
    )

    val activeVehicleName = vehicles.firstOrNull { it.isActive }?.displayName ?: "No active car"

    ExpenseScreen(
        activeVehicleName = activeVehicleName,
        expenses = filteredExpenses,
        selectedCategory = selectedCategory,
        searchQuery = searchQuery,
        validationMessage = validationMessage,
        onSearchQueryChange = { searchQuery = it },
        onCategorySelected = { category ->
            selectedCategory = if (selectedCategory == category) null else category
        },
        onAddExpense = {
            editorState = ExpenseEditorState()
            validationMessage = null
        },
        onEditExpense = { editorState = ExpenseEditorState.from(it) },
        onDeleteExpense = { pendingDelete = it },
    )

    editorState?.let { state ->
        ExpenseEditorDialog(
            state = state,
            appFileStore = appFileStore,
            context = context,
            onDismiss = {
                editorState = null
                validationMessage = null
            },
            onStateChange = { editorState = it },
            onConfirm = {
                val activeVehicle = activeVehicleId
                val amount = state.amount.toDoubleOrNull()
                val mileageValue = state.mileageValue.toDoubleOrNull()
                val mileageUnit = if (state.mileageValue.isBlank()) null else state.mileageUnit

                when (val validation = ExpenseRules.validateDraft(amount, mileageValue, mileageUnit)) {
                    is ExpenseDraftValidationResult.Invalid -> validationMessage = validation.message
                    ExpenseDraftValidationResult.Valid -> {
                        if (activeVehicle == null) {
                            validationMessage = "Create or select an active car before adding expenses."
                        } else {
                            val draft = ExpenseEntryDraft(
                                vehicleId = activeVehicle,
                                timestampEpochMillis = System.currentTimeMillis(),
                                category = state.category,
                                totalAmount = amount!!,
                                note = state.note,
                                attachmentPath = state.attachmentPath,
                                mileageValue = mileageValue,
                                mileageUnit = mileageUnit,
                            )
                            scope.launch {
                                if (state.expenseId == null) {
                                    expenseRepository.createExpense(draft)
                                } else {
                                    expenseRepository.updateExpense(state.expenseId, draft)
                                }
                                editorState = null
                                validationMessage = null
                            }
                        }
                    }
                }
            },
        )
    }

    pendingDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(text = "Delete expense") },
            text = { Text(text = "Delete ${expense.category.displayLabel()} expense?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            expenseRepository.deleteExpense(expense.id)
                        }
                        pendingDelete = null
                    },
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseScreen(
    activeVehicleName: String,
    expenses: List<ExpenseEntrySummary>,
    selectedCategory: ExpenseCategory?,
    searchQuery: String,
    validationMessage: String?,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (ExpenseCategory) -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (ExpenseEntrySummary) -> Unit,
    onDeleteExpense: (ExpenseEntrySummary) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Expenses") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = activeVehicleName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = "Search active car expenses") },
                            singleLine = true,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SystemExpenseCategories) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { onCategorySelected(category) },
                                    label = { Text(text = category.displayLabel()) },
                                )
                            }
                        }
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

            if (expenses.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No expenses match the current active-car filters yet.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            items(expenses, key = { it.id }) { expense ->
                ExpenseCard(
                    expense = expense,
                    onEdit = { onEditExpense(expense) },
                    onDelete = { onDeleteExpense(expense) },
                )
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: ExpenseEntrySummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = expense.category.displayLabel(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = "Amount: ${expense.totalAmount}")
            Text(text = formatExpenseTimestamp(expense.timestampEpochMillis))
            if (expense.note.isNotBlank()) {
                Text(text = expense.note)
            }
            Text(text = "Attachment: ${if (expense.attachmentPath != null) "Yes" else "No"}")
            Text(text = "Mileage linked: ${if (expense.mileageReading != null) "Yes" else "No"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text(text = "Edit")
                }
                TextButton(onClick = onDelete) {
                    Text(text = "Delete")
                }
            }
        }
    }
}

@Composable
private fun ExpenseEditorDialog(
    state: ExpenseEditorState,
    appFileStore: AppFileStore,
    context: Context,
    onDismiss: () -> Unit,
    onStateChange: (ExpenseEditorState) -> Unit,
    onConfirm: () -> Unit,
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val attachmentPath = uri?.let {
            copyCompressedReceipt(
                context = context,
                appFileStore = appFileStore,
                sourceUri = it,
            )
        }
        onStateChange(state.copy(attachmentPath = attachmentPath))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (state.expenseId == null) "Add Expense" else "Edit Expense")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { onStateChange(state.copy(amount = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Amount") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.note,
                    onValueChange = { onStateChange(state.copy(note = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Note") },
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SystemExpenseCategories) { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = { onStateChange(state.copy(category = category)) },
                            label = { Text(text = category.displayLabel()) },
                        )
                    }
                }
                OutlinedTextField(
                    value = state.mileageValue,
                    onValueChange = { onStateChange(state.copy(mileageValue = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Optional mileage") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onStateChange(state.copy(mileageUnit = DistanceUnit.KM)) }) {
                        Text(text = if (state.mileageUnit == DistanceUnit.KM) "KM selected" else "Use KM")
                    }
                    Button(onClick = { onStateChange(state.copy(mileageUnit = DistanceUnit.MI)) }) {
                        Text(text = if (state.mileageUnit == DistanceUnit.MI) "MI selected" else "Use MI")
                    }
                }
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) {
                    Text(text = if (state.attachmentPath == null) "Attach receipt image" else "Replace receipt image")
                }
                Text(text = state.attachmentPath?.let { "Attachment ready" } ?: "No attachment")
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

private fun copyCompressedReceipt(
    context: Context,
    appFileStore: AppFileStore,
    sourceUri: Uri,
): String? {
    val targetDirectory = appFileStore.bucket(FileBucket.MEDIA_RECEIPTS)
    val targetFile = File(targetDirectory, "${UUID.randomUUID()}.jpg")
    return compressImageToJpeg(
        context = context,
        sourceUri = sourceUri,
        destinationFile = targetFile,
    )
}

private fun ExpenseCategory.displayLabel(): String {
    return when (this) {
        ExpenseCategory.MAINTENANCE -> "Maintenance"
        ExpenseCategory.REPAIR -> "Repair"
        ExpenseCategory.TIRES -> "Tires"
        ExpenseCategory.INSURANCE -> "Insurance"
        ExpenseCategory.TAX -> "Tax"
        ExpenseCategory.FINES -> "Fines"
        ExpenseCategory.PARKING -> "Parking"
        ExpenseCategory.WASHING -> "Washing"
        ExpenseCategory.OTHER -> "Other"
    }
}

private fun formatExpenseTimestamp(timestampEpochMillis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(timestampEpochMillis)
}
