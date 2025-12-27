package com.condosuper.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.condosuper.app.data.models.*
import com.condosuper.app.managers.FirebaseManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManageView() {
    val firebaseManager = FirebaseManager.getInstance()
    val employees by firebaseManager.employees.collectAsState()
    val jobSites by firebaseManager.jobSites.collectAsState()
    val timeEntries by firebaseManager.timeEntries.collectAsState()
    val currentCompany by firebaseManager.currentCompany.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = Employees, 1 = Sites, 2 = Map
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredEmployees = remember(employees, searchQuery) {
        if (searchQuery.isBlank()) {
            employees
        } else {
            employees.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val filteredSites = remember(jobSites, searchQuery) {
        if (searchQuery.isBlank()) {
            jobSites
        } else {
            jobSites.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val totalHours = remember(timeEntries) {
        timeEntries.sumOf { entry ->
            val clockOut = entry.clockOutTime ?: System.currentTimeMillis()
            val workTime = clockOut - entry.clockInTime
            val breakTime = entry.breaks
                .filter { it.endTime != null }
                .sumOf { it.endTime!! - it.startTime }
            (workTime - breakTime) / (1000.0 * 60 * 60)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage") },
                actions = {
                    IconButton(onClick = { 
                        // Navigate to add employee/site
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Employees",
                    value = employees.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Job Sites",
                    value = jobSites.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Hours",
                    value = String.format("%.1f", totalHours),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Employees") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Sites") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Map") }
                )
            }
            
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            // Content
            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredEmployees) { employee ->
                            EmployeeManageCard(employee)
                        }
                    }
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSites) { site ->
                            SiteManageCard(site)
                        }
                    }
                }
                2 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map View - Coming Soon")
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeManageCard(employee: Employee) {
    val firebaseManager = FirebaseManager.getInstance()
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* Navigate to edit employee */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (employee.isManager) "Manager" else "Employee",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "ID: ${employee.id.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            IconButton(
                onClick = {
                    scope.launch {
                        firebaseManager.deleteEmployee(employee)
                    }
                }
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun SiteManageCard(site: JobSite) {
    val firebaseManager = FirebaseManager.getInstance()
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* Navigate to edit site */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${site.latitude}, ${site.longitude}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Radius: ${site.radius.toInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            IconButton(
                onClick = {
                    scope.launch {
                        firebaseManager.deleteJobSite(site)
                    }
                }
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

