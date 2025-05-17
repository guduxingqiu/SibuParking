package sibu.parking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sibu.parking.model.Report
import sibu.parking.model.ReportType
import sibu.parking.model.ReportStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    reports: List<Report>,
    onCreateReport: (
        type: ReportType,
        title: String,
        description: String,
        parkingArea: String,
        parkingLotNumber: String
    ) -> Unit,
    onUpdateStatus: ((String, ReportStatus) -> Unit)? = null,
    onBackClick: () -> Unit
) {
    var showNewReportDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<Report?>(null) }
    var showStatusDialog by remember { mutableStateOf(false) }
    
    val isStaff = onUpdateStatus != null
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewReportDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Report")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (reports.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No reports yet")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(reports) { report ->
                        ReportCard(
                            report = report,
                            onStatusClick = { 
                                if (onUpdateStatus != null) {
                                    selectedReport = report
                                    showStatusDialog = true
                                }
                            },
                            canUpdateStatus = onUpdateStatus != null
                        )
                    }
                }
            }
        }
    }
    
    if (showNewReportDialog) {
        NewReportDialog(
            onDismiss = { showNewReportDialog = false },
            onSubmit = { type, title, description, parkingArea, parkingLotNumber ->
                onCreateReport(type, title, description, parkingArea, parkingLotNumber)
                showNewReportDialog = false
            }
        )
    }
    
    if (showStatusDialog && selectedReport != null && onUpdateStatus != null) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update Status") },
            text = {
                Column {
                    ReportStatus.values().forEach { status ->
                        ListItem(
                            headlineContent = { Text(status.name.replace("_", " ")) },
                            leadingContent = {
                                RadioButton(
                                    selected = selectedReport?.status == status,
                                    onClick = {
                                        onUpdateStatus(selectedReport!!.id, status)
                                        showStatusDialog = false
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                onUpdateStatus(selectedReport!!.id, status)
                                showStatusDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReportCard(
    report: Report,
    onStatusClick: () -> Unit = {},
    canUpdateStatus: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = canUpdateStatus) { onStatusClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    color = when (report.status) {
                        ReportStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                        ReportStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
                        ReportStatus.RESOLVED -> MaterialTheme.colorScheme.secondaryContainer
                        ReportStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = report.status.name.replace("_", " "),
                        style = MaterialTheme.typography.labelMedium,
                        color = when (report.status) {
                            ReportStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
                            ReportStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onPrimaryContainer
                            ReportStatus.RESOLVED -> MaterialTheme.colorScheme.onSecondaryContainer
                            ReportStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = report.type.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (report.type == ReportType.PARKING_ISSUE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Location: ${report.parkingArea} - Lot ${report.parkingLotNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date(report.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (canUpdateStatus) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Click to update status",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        type: ReportType,
        title: String,
        description: String,
        parkingArea: String,
        parkingLotNumber: String
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(ReportType.PARKING_ISSUE) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var parkingArea by remember { mutableStateOf("") }
    var parkingLotNumber by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Report") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Report Type Selection
                Text("Report Type", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name.replace("_", " ")) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                // Parking Location (only for parking issues)
                if (selectedType == ReportType.PARKING_ISSUE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = parkingArea,
                        onValueChange = { parkingArea = it },
                        label = { Text("Parking Area") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = parkingLotNumber,
                        onValueChange = { parkingLotNumber = it },
                        label = { Text("Parking Lot Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onSubmit(selectedType, title, description, parkingArea, parkingLotNumber)
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 