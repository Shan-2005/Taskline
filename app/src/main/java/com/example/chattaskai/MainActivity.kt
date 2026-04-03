package com.example.chattaskai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.chattaskai.util.PermissionChecker
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import com.example.chattaskai.data.database.AppDatabase
import com.example.chattaskai.data.repository.TaskRepository
import com.example.chattaskai.ui.TaskViewModel
import com.example.chattaskai.ui.TaskViewModelFactory
import com.example.chattaskai.ui.screens.DashboardScreen
import com.example.chattaskai.ui.theme.TasklineTheme
import com.example.chattaskai.ui.theme.buildTypography
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.chattaskai.ui.screens.SettingsScreen
import com.example.chattaskai.ui.screens.TaskDetailScreen
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        requestNotificationPermission()
        
        // Final Resilient Typography Initialization
        val typography = buildTypography(this)
        
        val dao = AppDatabase.getDatabase(applicationContext).taskDao()
        val repository = TaskRepository(dao)

        setContent {
            val viewModel: TaskViewModel = viewModel(
                factory = TaskViewModelFactory(repository)
            )
            
            val themeHue by viewModel.themeHue.collectAsState()
            
            LaunchedEffect(Unit) {
                viewModel.loadSettings(applicationContext)
            }

            TasklineTheme(
                themeHue = themeHue,
                typography = typography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onTaskClick = { taskId -> navController.navigate("taskDetail/$taskId") },
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "taskDetail/{taskId}",
                            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
                            TaskDetailScreen(
                                taskId = taskId,
                                viewModel = viewModel,
                                onBack = { 
                                    if (navController.currentDestination?.route?.startsWith("taskDetail") == true) {
                                        navController.popBackStack() 
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionChecker.hasNotificationPermission(this)) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
