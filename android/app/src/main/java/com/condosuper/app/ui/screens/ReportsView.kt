package com.condosuper.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.condosuper.app.data.models.*
import com.condosuper.app.managers.FirebaseManager
import com.condosuper.app.managers.ReportGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsView() {
    val context = LocalContext.current
    val firebaseManager = FirebaseManager.getInstance()
    val scope = rememberCoroutineScope()
    
    val employees by firebaseManager.employees.collectAsState()
    val jobSites by firebaseManager.jobSites.collectAsState()
    val photos by firebaseManager.photos.collectAsState()
    val timeEntries by firebaseManager.timeEntries.collectAsState()
    val currentCompany by firebaseManager.currentCompany.collectAsState()
    
    var reportType by remember { mutableStateOf(0) } // 0 = Photo, 1 = Payroll
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedEmployees by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Report Type Selector
            Text(
                text = "Report Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = reportType == 0,
                    onClick = { reportType = 0 },
                    label = { Text("Photo Report") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = reportType == 1,
                    onClick = { reportType = 1 },
                    label = { Text("Payroll Report") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Date Range
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        startDate = calendar.timeInMillis
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = startDate?.let { dateFormat.format(Date(it)) } ?: "Start Date"
                    )
                }
                OutlinedButton(
                    onClick = {
                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        endDate = calendar.timeInMillis
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = endDate?.let { dateFormat.format(Date(it)) } ?: "End Date"
                    )
                }
            }
            
            // Filters
            if (reportType == 0) {
                // Photo Report Filters
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val allTags = photos.flatMap { it.tags }.distinct()
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTags.forEach { tag ->
                        FilterChip(
                            selected = selectedTags.contains(tag),
                            onClick = {
                                selectedTags = if (selectedTags.contains(tag)) {
                                    selectedTags - tag
                                } else {
                                    selectedTags + tag
                                }
                            },
                            label = { Text(tag) }
                        )
                    }
                }
            } else {
                // Payroll Report Filters
                Text(
                    text = "Employees",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    employees.forEach { employee ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedEmployees.contains(employee.id),
                                onCheckedChange = {
                                    selectedEmployees = if (selectedEmployees.contains(employee.id)) {
                                        selectedEmployees - employee.id
                                    } else {
                                        selectedEmployees + employee.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(employee.name)
                        }
                    }
                }
            }
            
            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Generate Button
            Button(
                onClick = {
                    if (startDate == null || endDate == null) {
                        errorMessage = "Please select a date range"
                        return@Button
                    }
                    
                    isGenerating = true
                    errorMessage = null
                    
                    scope.launch {
                        try {
                            val outputDir = File(context.getExternalFilesDir(null), "reports")
                            outputDir.mkdirs()
                            
                            val dateRangeStr = "${dateFormat.format(Date(startDate!!))} - ${dateFormat.format(Date(endDate!!))}"
                            
                            if (reportType == 0) {
                                // Photo Report
                                val filteredPhotos = photos.filter { photo ->
                                    photo.date >= startDate!! && photo.date <= endDate!! &&
                                    (selectedTags.isEmpty() || photo.tags.any { selectedTags.contains(it) })
                                }
                                
                                val outputFile = File(
                                    outputDir,
                                    "photo_report_${System.currentTimeMillis()}.pdf"
                                )
                                
                                ReportGenerator.generatePhotoReport(
                                    photos = filteredPhotos,
                                    employees = employees,
                                    sites = jobSites,
                                    title = "Photo Report",
                                    dateRange = dateRangeStr,
                                    companyName = currentCompany?.name ?: "Company",
                                    outputFile = outputFile
                                )
                                
                                // Share the file
                                // TODO: Implement ShareSheet
                            } else {
                                // Payroll Report
                                val filteredEntries = timeEntries.filter { entry ->
                                    entry.clockInTime >= startDate!! && 
                                    entry.clockInTime <= endDate!! &&
                                    (selectedEmployees.isEmpty() || selectedEmployees.contains(entry.employeeId))
                                }
                                
                                val outputFile = File(
                                    outputDir,
                                    "payroll_report_${System.currentTimeMillis()}.pdf"
                                )
                                
                                ReportGenerator.generatePayrollReport(
                                    timeEntries = filteredEntries,
                                    employees = employees,
                                    sites = jobSites,
                                    title = "Payroll Report",
                                    dateRange = dateRangeStr,
                                    outputFile = outputFile
                                )
                                
                                // Share the file
                                // TODO: Implement ShareSheet
                            }
                            
                            errorMessage = "Report generated successfully!"
                        } catch (e: Exception) {
                            errorMessage = "Error generating report: ${e.message}"
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating && startDate != null && endDate != null
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Report")
                }
            }
        }
    }
}

