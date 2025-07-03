@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.eglatracker.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eglatracker.data.LogEntry
import com.example.eglatracker.data.LogLevel
import com.example.eglatracker.data.LogTag
import com.example.eglatracker.viewmodel.LogViewerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    viewModel: LogViewerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val logs by viewModel.filteredLogs.collectAsState()
    val filters by viewModel.activeFilters.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with stats and controls
        LogViewerHeader(
            stats = uiState.stats,
            onClearLogs = viewModel::clearLogs,
            onRefresh = viewModel::refresh
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search bar
        SearchBar(
            searchQuery = filters.searchQuery,
            onSearchChange = viewModel::updateSearchQuery
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Filter chips
        FilterChips(
            selectedLevels = filters.levels,
            selectedTags = filters.tags,
            onLevelToggle = viewModel::toggleLevelFilter,
            onTagToggle = viewModel::toggleTagFilter,
            onClearFilters = viewModel::clearFilters
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Log entries
        if (logs.isEmpty()) {
            EmptyLogsState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs) { logEntry ->
                    LogEntryCard(
                        entry = logEntry,
                        onClick = { viewModel.toggleLogExpansion(logEntry.id) },
                        isExpanded = uiState.expandedLogIds.contains(logEntry.id)
                    )
                }
            }
        }
    }
}

@Composable
private fun LogViewerHeader(
    stats: Map<String, Int>,
    onClearLogs: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 System Logs",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh logs")
                    }
                    IconButton(onClick = onClearLogs) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stats chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stats.entries.toList()) { (key, count) ->
                    AssistChip(
                        onClick = { },
                        label = { Text("$key: $count") }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        label = { Text("Search logs...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun FilterChips(
    selectedLevels: Set<LogLevel>,
    selectedTags: Set<LogTag>,
    onLevelToggle: (LogLevel) -> Unit,
    onTagToggle: (LogTag) -> Unit,
    onClearFilters: () -> Unit
) {
    Column {
        // Level filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Levels",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (selectedLevels.isNotEmpty() || selectedTags.isNotEmpty()) {
                TextButton(onClick = onClearFilters) {
                    Text("Clear Filters")
                }
            }
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LogLevel.values()) { level ->
                FilterChip(
                    selected = selectedLevels.contains(level),
                    onClick = { onLevelToggle(level) },
                    label = { Text("${level.emoji} ${level.displayName}") }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Tag filters
        Text(
            text = "Categories",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LogTag.values()) { tag ->
                FilterChip(
                    selected = selectedTags.contains(tag),
                    onClick = { onTagToggle(tag) },
                    label = { Text("${tag.emoji} ${tag.displayName}") }
                )
            }
        }
    }
}

@Composable
private fun LogEntryCard(
    entry: LogEntry,
    onClick: () -> Unit,
    isExpanded: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when (entry.level) {
                LogLevel.ERROR, LogLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                LogLevel.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                LogLevel.SUCCESS -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Compact header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Smaller level indicator
                    Text(
                        text = entry.level.emoji,
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    // Compact tag
                    Text(
                        text = "${entry.tag.emoji} ${entry.tag.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(android.graphics.Color.parseColor(entry.tag.color))
                    )
                }
                
                Text(
                    text = entry.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Compact message
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Expandable details
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entry.details?.let { details ->
                        Divider()
                        Text(
                            text = "Details:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(6.dp)
                        )
                    }
                    
                    entry.data?.let { data ->
                        if (entry.details == null) {
                            Divider()
                        }
                        Text(
                            text = "Data:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        data.forEach { (key, value) ->
                            Text(
                                text = "$key: $value",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Text(
                        text = "ID: ${entry.id}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLogsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📝",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "No logs to display",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start using the app to see system logs appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 