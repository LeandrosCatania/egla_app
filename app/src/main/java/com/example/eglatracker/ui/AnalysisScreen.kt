package com.example.eglatracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.eglatracker.viewmodel.AnalysisViewModel
import com.example.eglatracker.viewmodel.AnalysisUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isTestingConnection by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Management") },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshData() }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Server Configuration
            item {
                ServerConfigCard(
                    currentServerUrl = uiState.currentServerUrl,
                    isConnected = uiState.isConnectedToDatabase,
                    connectionStatus = uiState.serverConnectionStatus,
                    isTestingConnection = isTestingConnection,
                    onServerUrlUpdate = viewModel::updateServerUrl,
                    onTestConnection = {
                        isTestingConnection = true
                        viewModel.testDatabaseConnection()
                        // Reset testing state after delay using proper scope
                        coroutineScope.launch {
                            delay(2000)
                            isTestingConnection = false
                        }
                    }
                )
            }
            
            // Database Status
            item {
                DatabaseStatusCard(
                    isConnected = uiState.isConnectedToDatabase,
                    connectionStatus = uiState.serverConnectionStatus
                )
            }
            
            // Quick Actions
            item {
                QuickActionsCard()
            }
            
            // Help Card
            item {
                HelpCard()
            }
            
            uiState.errorMessage?.let { errorMessage ->
                item {
                    ErrorCard(
                        error = errorMessage,
                        onDismiss = viewModel::clearError
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerConfigCard(
    currentServerUrl: String,
    isConnected: Boolean,
    connectionStatus: String,
    isTestingConnection: Boolean,
    onServerUrlUpdate: (String) -> Unit,
    onTestConnection: () -> Unit
) {
    var serverUrlInput by remember { mutableStateOf(currentServerUrl) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Database Server",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = serverUrlInput,
                onValueChange = { serverUrlInput = it },
                label = { Text("Server Address") },
                placeholder = { Text("192.168.1.100:3000") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { 
                    Text("Use 10.0.2.2:3000 for emulator or your PC's IP for device")
                },
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onServerUrlUpdate(serverUrlInput) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Update")
                }
                
                Button(
                    onClick = onTestConnection,
                    modifier = Modifier.weight(1f),
                    enabled = !isTestingConnection,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    } else {
                        Text("Test")
                    }
                }
            }
        }
    }
    
    // Update local state when currentServerUrl changes
    LaunchedEffect(currentServerUrl) {
        serverUrlInput = currentServerUrl
    }
}

@Composable
private fun DatabaseStatusCard(
    isConnected: Boolean,
    connectionStatus: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Connection Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = connectionStatus,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = if (isConnected) "🟢" else "🔴",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            if (isConnected) {
                Text(
                    text = "Your location data is being saved to the database in real-time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = "Make sure your database server is running and accessible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun QuickActionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: View data */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Data")
                }
                
                OutlinedButton(
                    onClick = { /* TODO: Export */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export")
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Clear data */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Data")
                }
                
                OutlinedButton(
                    onClick = { /* TODO: Settings */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Settings")
                }
            }
        }
    }
}

@Composable
private fun HelpCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "How It Works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "1. Start your database server on your computer",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "2. Enter your computer's IP address above",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "3. Test the connection to make sure it works",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "4. Go to Tracking tab and start location tracking",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Divider()
            
            Text(
                text = "Endpoints",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "GET /health - Check server status",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "POST /api/location - Send location data",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "GET /api/location/recent - Get recent data",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Dismiss")
            }
        }
    }
}
