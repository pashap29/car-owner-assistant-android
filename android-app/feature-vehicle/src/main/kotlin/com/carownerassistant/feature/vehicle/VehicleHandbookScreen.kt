package com.carownerassistant.feature.vehicle

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.core.content.FileProvider
import com.carownerassistant.core.files.AppFileStore
import com.carownerassistant.core.files.FileBucket
import com.carownerassistant.core.files.copyContentUriToFile
import com.carownerassistant.core.model.HandbookSectionId
import com.carownerassistant.core.model.HandbookRules
import com.carownerassistant.core.model.HandbookSectionSummary
import com.carownerassistant.core.model.VehicleDocumentSummary
import com.carownerassistant.core.model.VehicleHandbookSummary
import com.carownerassistant.core.model.repository.HandbookRepository
import com.carownerassistant.core.model.repository.VehicleRepository
import java.io.File
import java.text.DateFormat
import java.util.UUID
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val EmptyHandbook = VehicleHandbookSummary(
    vehicleId = "",
    vin = "",
    sections = HandbookRules.defaultSections(),
    documents = emptyList(),
)

@Composable
fun VehicleHandbookRoute(
    vehicleRepository: VehicleRepository,
    handbookRepository: HandbookRepository,
    appFileStore: AppFileStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vehicles by vehicleRepository.observeVehicles().collectAsState(initial = emptyList())
    val activeVehicleId by vehicleRepository.activeVehicleId().collectAsState(initial = null)

    var handbook by remember { mutableStateOf(EmptyHandbook) }
    var vinInput by remember { mutableStateOf("") }
    var sectionDrafts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(activeVehicleId) {
        val vehicleId = activeVehicleId
        if (vehicleId == null) {
            handbook = EmptyHandbook
            vinInput = ""
            sectionDrafts = emptyMap()
            return@LaunchedEffect
        }

        handbookRepository.observeHandbook(vehicleId).collectLatest { summary ->
            handbook = summary
            vinInput = summary.vin
            sectionDrafts = summary.sections.associate { it.id.name to it.content }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val vehicleId = activeVehicleId ?: return@rememberLauncherForActivityResult
        val sourceUri = uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val targetDirectory = File(appFileStore.bucket(FileBucket.MEDIA_DOCUMENTS), vehicleId).apply { mkdirs() }
            val displayName = context.resolveDisplayName(sourceUri)
            val targetFile = File(targetDirectory, "document_${UUID.randomUUID()}_${System.currentTimeMillis()}.pdf")
            val copiedPath = copyContentUriToFile(
                context = context,
                sourceUri = sourceUri,
                destinationFile = targetFile,
            )
            if (copiedPath != null) {
                handbookRepository.addDocument(
                    vehicleId = vehicleId,
                    displayName = displayName,
                    mimeType = "application/pdf",
                    filePath = copiedPath,
                )
            }
        }
    }

    VehicleHandbookScreen(
        activeVehicleName = vehicles.firstOrNull { it.isActive }?.displayName ?: "No active car",
        handbook = handbook,
        vinInput = vinInput,
        sectionDrafts = sectionDrafts,
        onBack = onBack,
        onVinChange = { vinInput = it },
        onSaveVin = {
            val vehicleId = activeVehicleId
            if (vehicleId != null) {
                scope.launch {
                    handbookRepository.updateVin(vehicleId, vinInput)
                }
            }
        },
        onSectionChange = { sectionId, content ->
            sectionDrafts = sectionDrafts + (sectionId.name to content)
        },
        onSaveSection = { section ->
            val vehicleId = activeVehicleId
            if (vehicleId != null) {
                scope.launch {
                    handbookRepository.updateSection(
                        vehicleId = vehicleId,
                        sectionId = section.id,
                        content = sectionDrafts[section.id.name].orEmpty(),
                    )
                }
            }
        },
        onAttachPdf = {
            pdfLauncher.launch(arrayOf("application/pdf"))
        },
        onOpenPdf = { document ->
            val file = File(document.filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, document.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleHandbookScreen(
    activeVehicleName: String,
    handbook: VehicleHandbookSummary,
    vinInput: String,
    sectionDrafts: Map<String, String>,
    onBack: () -> Unit,
    onVinChange: (String) -> Unit,
    onSaveVin: () -> Unit,
    onSectionChange: (HandbookSectionId, String) -> Unit,
    onSaveSection: (HandbookSectionSummary) -> Unit,
    onAttachPdf: () -> Unit,
    onOpenPdf: (VehicleDocumentSummary) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Vehicle Handbook") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = "Back")
                    }
                },
            )
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
                        OutlinedTextField(
                            value = vinInput,
                            onValueChange = onVinChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = "VIN (manual input only)") },
                            singleLine = true,
                        )
                        Button(onClick = onSaveVin) {
                            Text(text = "Save VIN")
                        }
                    }
                }
            }

            items(handbook.sections, key = { it.id.name }) { section ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = sectionDrafts[section.id.name].orEmpty(),
                            onValueChange = { onSectionChange(section.id, it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = "Content") },
                            minLines = 3,
                        )
                        Button(onClick = { onSaveSection(section) }) {
                            Text(text = "Save Section")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Documents",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(onClick = onAttachPdf) {
                            Text(text = "Attach PDF")
                        }
                    }
                }
            }

            if (handbook.documents.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No PDF documents attached yet.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            items(handbook.documents, key = { it.id }) { document ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = document.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(text = formatDocumentTime(document.createdAtEpochMillis))
                        Button(onClick = { onOpenPdf(document) }) {
                            Text(text = "Open PDF")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDocumentTime(createdAtEpochMillis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(createdAtEpochMillis)
}

private fun android.content.Context.resolveDisplayName(uri: Uri): String {
    val fallback = "document_${System.currentTimeMillis()}.pdf"
    val cursor: Cursor = contentResolver.query(
        uri,
        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )
        ?: return fallback
    cursor.use {
        if (!it.moveToFirst()) return fallback
        val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        return if (index >= 0) it.getString(index) ?: fallback else fallback
    }
}
