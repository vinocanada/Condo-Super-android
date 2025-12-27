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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardView() {
    val firebaseManager = FirebaseManager.getInstance()
    val employees by firebaseManager.employees.collectAsState()
    val timeEntries by firebaseManager.timeEntries.collectAsState()
    val currentCompany by firebaseManager.currentCompany.collectAsState()
    
    var selectedPeriod by remember { mutableStateOf(0) } // 0 = Daily, 1 = Weekly, 2 = Monthly
    
    val periodStart = remember(selectedPeriod) {
        val calendar = Calendar.getInstance()
        when (selectedPeriod) {
            0 -> { // Daily
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            1 -> { // Weekly
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            else -> { // Monthly
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
        }
        calendar.timeInMillis
    }
    
    val filteredEntries = remember(timeEntries, periodStart) {
        timeEntries.filter { it.clockInTime >= periodStart }
    }
    
    val employeeStats = remember(employees, filteredEntries) {
        employees.map { employee ->
            val employeeEntries = filteredEntries.filter { it.employeeId == employee.id }
            val totalHours = employeeEntries.sumOf { entry ->
                val clockOut = entry.clockOutTime ?: System.currentTimeMillis()
                val workTime = clockOut - entry.clockInTime
                val breakTime = entry.breaks
                    .filter { it.endTime != null }
                    .sumOf { it.endTime!! - it.startTime }
                (workTime - breakTime) / (1000.0 * 60 * 60)
            }
            val totalShifts = employeeEntries.size
            EmployeeStats(employee, totalHours, totalShifts, employeeEntries)
        }.sortedByDescending { it.totalHours }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Period Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPeriod == 0,
                    onClick = { selectedPeriod = 0 },
                    label = { Text("Daily") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPeriod == 1,
                    onClick = { selectedPeriod = 1 },
                    label = { Text("Weekly") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPeriod == 2,
                    onClick = { selectedPeriod = 2 },
                    label = { Text("Monthly") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Summary Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Total Employees",
                    value = employees.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Hours",
                    value = String.format("%.1f", employeeStats.sumOf { it.totalHours }),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Employee List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(employeeStats) { stats ->
                    EmployeeCard(stats)
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmployeeCard(stats: EmployeeStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* Navigate to detail */ }
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
                    text = stats.employee.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stats.totalShifts} shifts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                text = String.format("%.1f hrs", stats.totalHours),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class EmployeeStats(
    val employee: Employee,
    val totalHours: Double,
    val totalShifts: Int,
    val entries: List<TimeEntry>
)

