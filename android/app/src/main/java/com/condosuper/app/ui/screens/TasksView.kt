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
fun TasksView() {
    val firebaseManager = FirebaseManager.getInstance()
    val tasks by firebaseManager.tasks.collectAsState()
    val currentEmployee by firebaseManager.currentEmployee.collectAsState()
    
    var selectedStatus by remember { mutableStateOf<TaskTicket.TaskStatus?>(null) }
    var showMyTasksOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredTasks = remember(tasks, selectedStatus, showMyTasksOnly, searchQuery, currentEmployee) {
        tasks.filter { task ->
            val matchesStatus = selectedStatus == null || task.status == selectedStatus
            val matchesMyTasks = !showMyTasksOnly || task.assignedTo == currentEmployee?.id
            val matchesSearch = searchQuery.isBlank() || 
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)
            
            matchesStatus && matchesMyTasks && matchesSearch
        }.sortedByDescending { it.createdAt }
    }
    
    val taskStats = remember(tasks, currentEmployee) {
        val employeeId = currentEmployee?.id
        val myTasks = if (employeeId != null) {
            tasks.filter { it.assignedTo == employeeId }
        } else {
            emptyList()
        }
        TaskStats(
            total = tasks.size,
            myTasks = myTasks.size,
            pending = tasks.count { it.status == TaskTicket.TaskStatus.OPEN },
            inProgress = tasks.count { it.status == TaskTicket.TaskStatus.IN_PROGRESS },
            completed = tasks.count { it.status == TaskTicket.TaskStatus.COMPLETED }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(onClick = { /* Navigate to create task */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task")
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
                    title = "Total",
                    value = taskStats.total.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "My Tasks",
                    value = taskStats.myTasks.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pending",
                    value = taskStats.pending.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search tasks...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                
                // Status Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = selectedStatus == TaskTicket.TaskStatus.OPEN,
                        onClick = { 
                            selectedStatus = if (selectedStatus == TaskTicket.TaskStatus.OPEN) null 
                            else TaskTicket.TaskStatus.OPEN
                        },
                        label = { Text("Open") }
                    )
                    FilterChip(
                        selected = selectedStatus == TaskTicket.TaskStatus.IN_PROGRESS,
                        onClick = { 
                            selectedStatus = if (selectedStatus == TaskTicket.TaskStatus.IN_PROGRESS) null 
                            else TaskTicket.TaskStatus.IN_PROGRESS
                        },
                        label = { Text("In Progress") }
                    )
                    FilterChip(
                        selected = selectedStatus == TaskTicket.TaskStatus.COMPLETED,
                        onClick = { 
                            selectedStatus = if (selectedStatus == TaskTicket.TaskStatus.COMPLETED) null 
                            else TaskTicket.TaskStatus.COMPLETED
                        },
                        label = { Text("Completed") }
                    )
                }
                
                // My Tasks Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showMyTasksOnly,
                        onCheckedChange = { showMyTasksOnly = it }
                    )
                    Text("My Tasks Only")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Task List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTasks) { task ->
                    TaskCard(task)
                }
            }
        }
    }
}

@Composable
fun TaskCard(task: TaskTicket) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val statusColor = when (task.status) {
        TaskTicket.TaskStatus.OPEN -> MaterialTheme.colorScheme.tertiary
        TaskTicket.TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        TaskTicket.TaskStatus.PENDING_REVIEW -> MaterialTheme.colorScheme.secondary
        TaskTicket.TaskStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        TaskTicket.TaskStatus.CLOSED -> MaterialTheme.colorScheme.error
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* Navigate to task detail */ },
        colors = CardDefaults.cardColors(
            containerColor = if (task.priority == TaskTicket.TaskPriority.HIGH) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
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
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Surface(
                    color = statusColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = task.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (task.assignedToName != null) {
                        Text(
                            text = "Assigned to: ${task.assignedToName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "Created: ${dateFormat.format(Date(task.createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                if (task.priority == TaskTicket.TaskPriority.HIGH) {
                    Icon(
                        Icons.Default.PriorityHigh,
                        contentDescription = "High Priority",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

data class TaskStats(
    val total: Int,
    val myTasks: Int,
    val pending: Int,
    val inProgress: Int,
    val completed: Int
)

