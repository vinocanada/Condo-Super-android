package com.condosuper.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.condosuper.app.managers.FirebaseManager
import com.condosuper.app.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val firebaseManager = FirebaseManager.getInstance()
    val currentCompany by firebaseManager.currentCompany.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (currentCompany == null) "splash" else "main"
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToAuth = { navController.navigate("auth") },
                onNavigateToMain = { navController.navigate("main") { popUpTo("splash") { inclusive = true } } }
            )
        }
        
        composable("auth") {
            AuthWelcomeScreen(
                onNavigateToSignup = { navController.navigate("signup") },
                onNavigateToCompanyLogin = { navController.navigate("company_login") },
                onNavigateToEmployeeLogin = { navController.navigate("employee_login") },
                onNavigateToMain = { navController.navigate("main") { popUpTo("auth") { inclusive = true } } }
            )
        }
        
        composable("signup") {
            CompanySetupScreen(
                onComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("company_login") {
            CompanyLoginScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("employee_login") {
            EmployeeLoginScreen(
                onSuccess = { navController.navigate("main") { popUpTo("auth") { inclusive = true } } },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("main") {
            MainTabScreen()
        }
    }
}


