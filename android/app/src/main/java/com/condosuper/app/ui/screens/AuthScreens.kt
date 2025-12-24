package com.condosuper.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.condosuper.app.managers.BiometricManager
import com.condosuper.app.managers.FirebaseManager
import kotlinx.coroutines.launch

@Composable
fun CompanyLoginScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var companyId by remember { mutableStateOf("") }
    var ownerEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val firebaseManager = FirebaseManager.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organization Login") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Login to Organization",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Text(
                text = "Enter your company ID and owner email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = companyId,
                onValueChange = { companyId = it; errorMessage = null },
                label = { Text("Company ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            
            OutlinedTextField(
                value = ownerEmail,
                onValueChange = { ownerEmail = it; errorMessage = null },
                label = { Text("Owner Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !isLoading
            )
            
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (companyId.isBlank()) {
                        errorMessage = "Company ID is required"
                        return@Button
                    }
                    if (ownerEmail.isBlank() || !ownerEmail.contains("@")) {
                        errorMessage = "Valid email is required"
                        return@Button
                    }
                    
                    isLoading = true
                    errorMessage = null
                    
                    scope.launch {
                        val company = firebaseManager.fetchCompany(companyId)
                        if (company != null && company.ownerEmail.equals(ownerEmail, ignoreCase = true)) {
                            firebaseManager.setCompany(company)
                            onSuccess()
                        } else {
                            errorMessage = "Invalid company ID or email"
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && companyId.isNotBlank() && ownerEmail.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }
        }
    }
}

@Composable
fun EmployeeLoginScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var companyId by remember { mutableStateOf("") }
    var employeeIdentifier by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBiometricOption by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val firebaseManager = FirebaseManager.getInstance()
    val biometricManager = remember { BiometricManager.getInstance(context) }
    
    LaunchedEffect(Unit) {
        showBiometricOption = biometricManager.isBiometricAvailable
        // Check for saved credentials
        val credentials = biometricManager.retrieveCredentials()
        if (credentials != null && showBiometricOption) {
            // Auto-fill
            companyId = credentials.first
            employeeIdentifier = credentials.second
            pin = credentials.third
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employee Login") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Employee Login",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Text(
                text = "Enter your company ID, identifier, and PIN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = companyId,
                onValueChange = { companyId = it; errorMessage = null },
                label = { Text("Company ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            
            OutlinedTextField(
                value = employeeIdentifier,
                onValueChange = { employeeIdentifier = it; errorMessage = null },
                label = { Text("Employee ID or Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            
            OutlinedTextField(
                value = pin,
                onValueChange = { 
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        pin = it
                        errorMessage = null
                    }
                },
                label = { Text("PIN (4 digits)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                enabled = !isLoading
            )
            
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (showBiometricOption && companyId.isNotBlank() && employeeIdentifier.isNotBlank() && pin.length == 4) {
                val activity = context as? FragmentActivity
                if (activity != null) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val authenticated = biometricManager.authenticate(activity, "Log in to your account")
                                if (authenticated) {
                                    // Use saved credentials
                                    val credentials = biometricManager.retrieveCredentials()
                                    if (credentials != null) {
                                        companyId = credentials.first
                                        employeeIdentifier = credentials.second
                                        pin = credentials.third
                                        // Proceed with login
                                        performLogin()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use ${biometricManager.biometricType.value.displayName}")
                    }
                }
            }
            
            Button(
                onClick = { performLogin() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && companyId.isNotBlank() && employeeIdentifier.isNotBlank() && pin.length == 4
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }
        }
    }
    
    fun performLogin() {
        if (companyId.isBlank() || employeeIdentifier.isBlank() || pin.length != 4) {
            errorMessage = "Please fill all fields correctly"
            return
        }
        
        isLoading = true
        errorMessage = null
        
        scope.launch {
            // First, fetch company
            val company = firebaseManager.fetchCompany(companyId)
            if (company == null) {
                errorMessage = "Company not found"
                isLoading = false
                return@launch
            }
            
            firebaseManager.setCompany(company)
            
            // Then find employee
            val employees = firebaseManager.employees.value
            val employee = employees.find { 
                it.id == employeeIdentifier || 
                it.name.equals(employeeIdentifier, ignoreCase = true)
            }
            
            if (employee == null || employee.pin != pin) {
                errorMessage = "Invalid employee ID or PIN"
                isLoading = false
                return@launch
            }
            
            // Save credentials for biometric login
            if (showBiometricOption) {
                biometricManager.storeCredentials(companyId, employeeIdentifier, pin)
                biometricManager.enableBiometric()
            }
            
            // Set current employee (you may need to add this to FirebaseManager)
            onSuccess()
        }
    }
}

