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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.condosuper.app.managers.FirebaseManager
import com.condosuper.app.managers.ThemeManager

@Composable
fun ProfileView() {
    val firebaseManager = FirebaseManager.getInstance()
    val currentEmployee by firebaseManager.currentEmployee.collectAsState()
    val currentCompany by firebaseManager.currentCompany.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentEmployee?.name?.take(1)?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = currentEmployee?.name ?: "Unknown",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentEmployee?.isManager == true) "Manager" else "Employee",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Appearance Settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ListItem(
                        headlineContent = { Text("Theme") },
                        supportingContent = { 
                            Text(ThemeManager.currentTheme.value.name)
                        },
                        leadingContent = {
                            Icon(Icons.Default.Palette, contentDescription = null)
                        },
                        trailingContent = {
                            IconButton(onClick = { /* Navigate to theme picker */ }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    )
                    
                    Divider()
                    
                    ListItem(
                        headlineContent = { Text("Video Quality") },
                        supportingContent = { Text("High") },
                        leadingContent = {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null)
                        },
                        trailingContent = {
                            IconButton(onClick = { /* Navigate to quality picker */ }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // General Settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "General",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var biometricEnabled by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text("Biometric Login") },
                        supportingContent = { Text("Use fingerprint or face ID") },
                        leadingContent = {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { biometricEnabled = it }
                            )
                        }
                    )
                    
                    Divider()
                    
                    var notificationsEnabled by remember { mutableStateOf(true) }
                    ListItem(
                        headlineContent = { Text("Notifications") },
                        supportingContent = { Text("Receive push notifications") },
                        leadingContent = {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it }
                            )
                        }
                    )
                    
                    Divider()
                    
                    var geofencingEnabled by remember { mutableStateOf(true) }
                    ListItem(
                        headlineContent = { Text("Geofencing") },
                        supportingContent = { Text("Auto clock in/out at job sites") },
                        leadingContent = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        trailingContent = {
                            Switch(
                                checked = geofencingEnabled,
                                onCheckedChange = { geofencingEnabled = it }
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Company Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Company",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ListItem(
                        headlineContent = { Text("Company ID") },
                        supportingContent = { 
                            Text(currentCompany?.id ?: "N/A")
                        },
                        leadingContent = {
                            Icon(Icons.Default.Business, contentDescription = null)
                        },
                        trailingContent = {
                            IconButton(onClick = { /* Show company ID */ }) {
                                Icon(Icons.Default.Copy, contentDescription = "Copy")
                            }
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text("Company Name") },
                        supportingContent = { 
                            Text(currentCompany?.name ?: "N/A")
                        },
                        leadingContent = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Logout Button
            Button(
                onClick = { /* Logout */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

