package com.example.securityapp.ui.screens.battery

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securityapp.data.repository.AppBatteryUsage
import com.example.securityapp.data.repository.BatteryRepository
import com.example.securityapp.data.repository.BatteryStateSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BatteryUiState {
    object Loading : BatteryUiState()
    object PermissionRequired : BatteryUiState()
    data class Success(
        val snapshot: BatteryStateSnapshot,
        val appUsageList: List<AppBatteryUsage>,
        val currentTimeframe: TimeframeFilter
    ) : BatteryUiState()
    data class Error(val message: String) : BatteryUiState()
}

enum class TimeframeFilter(val label: String, val ms: Long) {
    HOUR_1("Last 1 Hour", 1 * 60 * 60 * 1000L),
    HOURS_6("Last 6 Hours", 6 * 60 * 60 * 1000L),
    HOURS_24("Last 24 Hours", 24 * 60 * 60 * 1000L),
    DAYS_7("Last 7 Days", 7 * 24 * 60 * 60 * 1000L)
}

@HiltViewModel
class BatteryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BatteryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BatteryUiState>(BatteryUiState.Loading)
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    private var currentFilter = TimeframeFilter.HOURS_24
    
    // Store original list to support sorting locally
    private var allAppsUsage = listOf<AppBatteryUsage>()

    init {
        checkPermissionAndLoadData()
    }

    fun checkPermissionAndLoadData() {
        if (!hasUsageStatsPermission(context)) {
            _uiState.value = BatteryUiState.PermissionRequired
            return
        }
        
        loadBatteryData(currentFilter)
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsagePermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun setTimeframeFilter(filter: TimeframeFilter) {
        if (currentFilter != filter) {
            currentFilter = filter
            loadBatteryData(filter)
        }
    }

    fun refreshData() {
        loadBatteryData(currentFilter)
    }
    
    fun sortApps(byName: Boolean = false) {
        val currentState = _uiState.value
        if (currentState is BatteryUiState.Success) {
            val sortedList = if (byName) {
                allAppsUsage.sortedBy { it.packageName }
            } else {
                allAppsUsage.sortedByDescending { it.batteryPercentageConsumed }
            }
            _uiState.value = currentState.copy(appUsageList = sortedList)
        }
    }

    private fun loadBatteryData(filter: TimeframeFilter) {
        _uiState.value = BatteryUiState.Loading
        viewModelScope.launch {
            try {
                // Save a snapshot to history periodically when loading dashboard
                repository.saveCurrentSnapshot()
                
                val currentSnapshot = repository.getCurrentBatteryState()
                
                // Fetch stats based on filter
                allAppsUsage = repository.getUsageStatsForTimeframe(filter.ms)
                
                _uiState.value = BatteryUiState.Success(
                    snapshot = currentSnapshot,
                    appUsageList = allAppsUsage,
                    currentTimeframe = filter
                )
            } catch (e: Exception) {
                _uiState.value = BatteryUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}
