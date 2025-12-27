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
import androidx.compose.ui.unit.sp
import com.condosuper.app.data.models.*
import com.condosuper.app.managers.FirebaseManager
import com.condosuper.app.managers.LocationManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TimeClockView() {
    val context = LocalContext.current
    val firebaseManager = FirebaseManager.getInstance()
    val locationManager = remember { LocationManager(context) }
    val scope = rememberCoroutineScope()
    
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        } else {
            locationManager.startUpdating()
        }
    }
    
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            locationManager.startUpdating()
        } else {
            locationManager.stopUpdating()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            locationManager.stopUpdating()
        }
    }
    
    val currentEmployee by firebaseManager.currentEmployee.collectAsState()
    val currentCompany by firebaseManager.currentCompany.collectAsState()
    val jobSites by firebaseManager.jobSites.collectAsState()
    val timeEntries by firebaseManager.timeEntries.collectAsState()
    val currentLocation by locationManager.location.collectAsState()
    
    var selectedSiteId by remember { mutableStateOf<String?>(null) }
    var isClockedIn by remember { mutableStateOf(false) }
    var currentTimeEntry by remember { mutableStateOf<TimeEntry?>(null) }
    var isOnBreak by remember { mutableStateOf(false) }
    var currentBreak by remember { mutableStateOf<BreakEntry?>(null) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var breakTime by remember { mutableStateOf(0L) }
    
    // Find current active time entry
    LaunchedEffect(timeEntries, currentEmployee?.id) {
        val employeeId = currentEmployee?.id ?: return@LaunchedEffect
        val activeEntry = timeEntries
            .filter { it.employeeId == employeeId && it.clockOutTime == null }
            .maxByOrNull { it.clockInTime }
        
        currentTimeEntry = activeEntry
        isClockedIn = activeEntry != null
        
        if (activeEntry != null) {
            selectedSiteId = activeEntry.siteId
            val activeBreak = activeEntry.breaks.lastOrNull { it.endTime == null }
            isOnBreak = activeBreak != null
            currentBreak = activeBreak
        }
    }
    
    // Update elapsed time
    LaunchedEffect(isClockedIn, currentTimeEntry, isOnBreak) {
        while (isClockedIn && currentTimeEntry != null) {
            val now = System.currentTimeMillis()
            val clockInTime = currentTimeEntry!!.clockInTime
            val totalBreakTime = currentTimeEntry!!.breaks
                .filter { it.endTime != null }
                .sumOf { it.endTime!! - it.startTime }
            val currentBreakTime = if (isOnBreak && currentBreak != null) {
                now - currentBreak!!.startTime
            } else 0L
            
            elapsedTime = now - clockInTime - totalBreakTime - currentBreakTime
            breakTime = totalBreakTime + currentBreakTime
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Time Clock") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isClockedIn) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isClockedIn) "Clocked In" else "Clocked Out",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isClockedIn && currentTimeEntry != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = formatDuration(elapsedTime),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total Time",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (breakTime > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Break: ${formatDuration(breakTime)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            
            // Site Selection (only when not clocked in)
            if (!isClockedIn) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Job Site",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (jobSites.isEmpty()) {
                            Text(
                                text = "No job sites available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            jobSites.forEach { site ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedSiteId == site.id,
                                        onClick = { selectedSiteId = site.id }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = site.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Radius: ${site.radius.toInt()}m",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Show current site
                currentTimeEntry?.let { entry ->
                    val site = jobSites.find { it.id == entry.siteId }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = site?.name ?: "Unknown Site",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                                        .format(Date(entry.clockInTime)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Location Status
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (currentLocation != null) Icons.Default.MyLocation else Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = if (currentLocation != null) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (currentLocation != null) "Location Available" else "Location Unavailable",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (currentLocation != null) {
                            Text(
                                text = "Accuracy: ${currentLocation!!.accuracy.toInt()}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            // Action Buttons
            Spacer(modifier = Modifier.height(8.dp))
            
            if (!isClockedIn) {
                Button(
                    onClick = {
                        val employee = currentEmployee ?: return@Button
                        val company = currentCompany ?: return@Button
                        val siteId = selectedSiteId ?: return@Button
                        val location = currentLocation ?: return@Button
                        
                        scope.launch {
                            val now = System.currentTimeMillis()
                            val entry = TimeEntry(
                                companyId = company.id,
                                employeeId = employee.id,
                                siteId = siteId,
                                clockInTime = now,
                                clockInLat = location.latitude,
                                clockInLon = location.longitude
                            )
                            firebaseManager.saveTimeEntry(entry)
                            
                            // Save initial location point
                            val locationPoint = LocationPoint(
                                companyId = company.id,
                                employeeId = employee.id,
                                timeEntryId = entry.id,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                timestamp = now,
                                accuracy = location.accuracy
                            )
                            firebaseManager.saveLocationPoint(locationPoint)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedSiteId != null && currentLocation != null && currentEmployee != null
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clock In")
                }
            } else {
                // Break button
                Button(
                    onClick = {
                        val entry = currentTimeEntry ?: return@Button
                        scope.launch {
                            val now = System.currentTimeMillis()
                            val updatedBreaks = if (isOnBreak) {
                                // End break
                                entry.breaks.map { break ->
                                    if (break.id == currentBreak?.id) {
                                        break.copy(endTime = now)
                                    } else break
                                }
                            } else {
                                // Start break
                                entry.breaks + BreakEntry(startTime = now)
                            }
                            val updatedEntry = entry.copy(breaks = updatedBreaks)
                            firebaseManager.saveTimeEntry(updatedEntry)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOnBreak) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        if (isOnBreak) Icons.Default.Stop else Icons.Default.Pause,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isOnBreak) "End Break" else "Start Break")
                }
                
                // Clock Out button
                Button(
                    onClick = {
                        val entry = currentTimeEntry ?: return@Button
                        val location = currentLocation
                        scope.launch {
                            val now = System.currentTimeMillis()
                            val updatedBreaks = entry.breaks.map { break ->
                                if (break.endTime == null) {
                                    break.copy(endTime = now)
                                } else break
                            }
                            val updatedEntry = entry.copy(
                                clockOutTime = now,
                                clockOutLat = location?.latitude,
                                clockOutLon = location?.longitude,
                                breaks = updatedBreaks
                            )
                            firebaseManager.saveTimeEntry(updatedEntry)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clock Out")
                }
            }
        }
    }
}

fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

