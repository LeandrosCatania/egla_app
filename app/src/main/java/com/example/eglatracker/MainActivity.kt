package com.example.eglatracker

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.eglatracker.ui.AnalysisScreen
import com.example.eglatracker.ui.LogViewerScreen
import com.example.eglatracker.ui.theme.EGLATrackerTheme
import com.egla.core.DirectionCalculator
import com.example.eglatracker.viewmodel.LocationTrackingViewModelV2
import com.example.eglatracker.viewmodel.LocationTrackingUiState
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (!allPermissionsGranted) {
            // Handle permission denial
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestPermissions()
        
        setContent {
            EGLATrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EGLATrackerApp()
                }
            }
        }
    }
    
    private fun requestPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EGLATrackerApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Tracking") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Data") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Logs") }
            )
        }
        
        when (selectedTab) {
            0 -> TrackingScreen()
            1 -> AnalysisScreen()
            2 -> LogViewerScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    viewModel: LocationTrackingViewModelV2 = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    
    // Get database stats for status monitoring
    var databaseStats by remember { mutableStateOf<com.example.eglatracker.utils.DatabaseLogger.DatabaseStats?>(null) }
    var isLoadingStats by remember { mutableStateOf(false) }
    
    // Only load stats when tracking starts, not on every recomposition
    LaunchedEffect(isTracking) {
        if (isTracking && databaseStats == null) {
            isLoadingStats = true
            try {
                databaseStats = viewModel.getDatabaseStats()
            } catch (e: Exception) {
                // Handle stats loading error gracefully
            } finally {
                isLoadingStats = false
            }
        } else if (!isTracking) {
            // Reset stats when tracking stops
            databaseStats = null
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Display errors as snackbars for better UX
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EGLA Location Tracker") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main control card with status
            item {
                MainControlCard(
                    isTracking = isTracking,
                    onStartTracking = { viewModel.startTracking() },
                    onStopTracking = { viewModel.stopTracking() },
                    databaseStats = databaseStats,
                    isLoadingStats = isLoadingStats
                )
            }
            
            // Current location and movement in one card
            item {
                LocationMovementCard(uiState = uiState)
            }
            
            // Performance metrics combined
            item {
                PerformanceCard(uiState = uiState)
            }
            
            // EGLA settings
            item {
                SettingsCard(
                    currentMode = uiState.eglaMode,
                    onModeChange = { mode ->
                        when (mode) {
                            "Balanced" -> viewModel.changeToBalancedMode()
                            "High Accuracy" -> viewModel.changeToHighAccuracyMode()
                            "Ultra High" -> viewModel.changeToUltraHighMode()
                        }
                    }
                )
            }
            
            /* Error messages are now shown via SnackbarHost */
        }
    }
}

@Composable
fun MainControlCard(
    isTracking: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    databaseStats: com.example.eglatracker.utils.DatabaseLogger.DatabaseStats?,
    isLoadingStats: Boolean
) {
    var isProcessing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTracking) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = if (isTracking) "🟢 Active" else "⚫ Stopped",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = {
                        isProcessing = true
                        if (isTracking) onStopTracking() else onStartTracking()
                        // Reset processing after a short delay using proper scope
                        coroutineScope.launch {
                            delay(1000)
                            isProcessing = false
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isTracking) "Stop" else "Start")
                    }
                }
            }
            
            // Database status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Database",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (isLoadingStats) {
                        Text("Loading...")
                    } else {
                        Text(
                            text = if (databaseStats?.isConnected == true) "✅ Connected" else "❌ Disconnected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Column {
                    Text(
                        text = "Records",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${databaseStats?.totalRecords ?: 0}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (isTracking && databaseStats?.currentSession != null) {
                    Column {
                        Text(
                            text = "Session",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = databaseStats.currentSession.takeLast(8), // Show 8 chars for better debugging
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationMovementCard(uiState: LocationTrackingUiState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Location & Movement",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Location coordinates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Coordinates",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${String.format("%.5f", uiState.latitude)}, ${String.format("%.5f", uiState.longitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Text(
                        text = "Environment",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = uiState.environment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Divider()
            
            // Movement info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Direction",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = uiState.direction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.isStationary) Color.Gray else MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = "Speed",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${String.format("%.1f", DirectionCalculator.msToKmh(uiState.speed))} km/h",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Text(
                        text = "Bearing",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${String.format("%.0f", uiState.bearing)}°",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PerformanceCard(uiState: LocationTrackingUiState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "EGLA Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Accuracy comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Original GPS",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${String.format("%.1f", uiState.originalAccuracy)}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red
                    )
                }
                Column {
                    Text(
                        text = "EGLA Enhanced",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${String.format("%.1f", uiState.accuracy)}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "Improvement",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${String.format("%.0f", uiState.accuracyImprovement)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.accuracyImprovement > 0) Color.Green else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Performance bar
            LinearProgressIndicator(
                progress = (uiState.accuracyImprovement / 100f).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    uiState.accuracyImprovement > 50 -> Color.Green
                    uiState.accuracyImprovement > 25 -> Color(0xFFFF9800)
                    uiState.accuracyImprovement > 0 -> Color(0xFFFFC107)
                    else -> Color.Gray
                }
            )
            
            // Additional metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Confidence",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${String.format("%.0f", uiState.confidence * 100)}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Text(
                        text = "Processing",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${uiState.processingTime}ms",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Text(
                        text = "System",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = uiState.systemStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.systemStatus == "ACTIVE") Color.Green else Color.Gray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCard(
    currentMode: String,
    onModeChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf("Balanced", "High Accuracy", "Ultra High")
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "EGLA Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Operating Mode",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = currentMode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.menuAnchor()
                    ) {
                        Text("Change")
                    }
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        modes.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode) },
                                onClick = {
                                    onModeChange(mode)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
} 