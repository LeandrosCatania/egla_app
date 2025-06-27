package com.example.eglatracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eglatracker.data.LogLevel
import com.example.eglatracker.data.LogTag
import com.example.eglatracker.utils.LogFilters
import com.example.eglatracker.utils.LogStats
import com.example.eglatracker.utils.LoggingManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the log viewer screen
 */
class LogViewerViewModel : ViewModel() {
    
    private val loggingManager = LoggingManager.getInstance()
    
    private val _uiState = MutableStateFlow(LogViewerUiState())
    val uiState: StateFlow<LogViewerUiState> = _uiState.asStateFlow()
    
    val filteredLogs = loggingManager.filteredLogs
    val activeFilters = loggingManager.activeFilters
    
    init {
        // Observe logs and update stats
        viewModelScope.launch {
            loggingManager.logs.collect { logs ->
                val stats = loggingManager.getLogStats()
                _uiState.value = _uiState.value.copy(
                    stats = mapOf(
                        "Total" to stats.total,
                        "Errors" to (stats.byLevel[LogLevel.ERROR] ?: 0) + (stats.byLevel[LogLevel.CRITICAL] ?: 0),
                        "Warnings" to (stats.byLevel[LogLevel.WARNING] ?: 0),
                        "Last Hour" to stats.lastHour
                    )
                )
            }
        }
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        val currentFilters = activeFilters.value
        loggingManager.updateFilters(
            currentFilters.copy(searchQuery = query)
        )
    }
    
    /**
     * Toggle level filter
     */
    fun toggleLevelFilter(level: LogLevel) {
        val currentFilters = activeFilters.value
        val newLevels = if (currentFilters.levels.contains(level)) {
            currentFilters.levels - level
        } else {
            currentFilters.levels + level
        }
        
        loggingManager.updateFilters(
            currentFilters.copy(levels = newLevels)
        )
    }
    
    /**
     * Toggle tag filter
     */
    fun toggleTagFilter(tag: LogTag) {
        val currentFilters = activeFilters.value
        val newTags = if (currentFilters.tags.contains(tag)) {
            currentFilters.tags - tag
        } else {
            currentFilters.tags + tag
        }
        
        loggingManager.updateFilters(
            currentFilters.copy(tags = newTags)
        )
    }
    
    /**
     * Clear all filters
     */
    fun clearFilters() {
        loggingManager.updateFilters(LogFilters())
    }
    
    /**
     * Toggle log expansion
     */
    fun toggleLogExpansion(logId: String) {
        val currentExpanded = _uiState.value.expandedLogIds
        val newExpanded = if (currentExpanded.contains(logId)) {
            currentExpanded - logId
        } else {
            currentExpanded + logId
        }
        
        _uiState.value = _uiState.value.copy(expandedLogIds = newExpanded)
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs() {
        loggingManager.clearLogs()
        _uiState.value = _uiState.value.copy(expandedLogIds = emptySet())
    }
    
    /**
     * Refresh logs (placeholder for future functionality)
     */
    fun refresh() {
        // Could trigger log refresh from external sources if needed
    }
}

/**
 * UI state for log viewer
 */
data class LogViewerUiState(
    val stats: Map<String, Int> = emptyMap(),
    val expandedLogIds: Set<String> = emptySet(),
    val isLoading: Boolean = false
) 