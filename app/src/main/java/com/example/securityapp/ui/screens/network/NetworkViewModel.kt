package com.example.securityapp.ui.screens.network

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securityapp.data.repository.AppRepository
import com.example.securityapp.data.repository.NetworkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import com.example.securityapp.data.repository.DnsLogRepository
import com.example.securityapp.ui.screens.connections.ConnectionLog
import com.example.securityapp.data.vpn.LocalVpnService
import android.content.Intent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

data class NetworkAppModel(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val wifiUsage: Long,
    val mobileUsage: Long,
    val totalUsage: Long
)

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val appRepository: AppRepository,
    private val dnsLogRepository: DnsLogRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    val dnsLogs: StateFlow<List<ConnectionLog>> = dnsLogRepository.logs
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _uiState = MutableStateFlow<NetworkUiState>(NetworkUiState.Loading)
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    private var allAppsUsage = listOf<NetworkAppModel>()
    private var showSystemApps = false

    init {
        loadNetworkData()
    }

    private fun loadNetworkData() {
        viewModelScope.launch {
            _uiState.value = NetworkUiState.Loading
            
            // Get last 30 days
            val cal = Calendar.getInstance()
            val endTime = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -30)
            val startTime = cal.timeInMillis

            val usageMap = networkRepository.getNetworkUsage(startTime, endTime)
            val installedApps = appRepository.getInstalledApps()
            val pm = context.packageManager

            // Map UIDs to app models
            val mappedApps = installedApps.mapNotNull { app ->
                try {
                    val appInfo = pm.getApplicationInfo(app.packageName, 0)
                    val uid = appInfo.uid
                    val usage = usageMap[uid]
                    if (usage != null && usage.totalUsage > 0) {
                        NetworkAppModel(
                            packageName = app.packageName,
                            appName = app.appName,
                            icon = app.icon,
                            isSystemApp = app.isSystemApp,
                            wifiUsage = usage.totalWifi,
                            mobileUsage = usage.totalMobile,
                            totalUsage = usage.totalUsage
                        )
                    } else null
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }

            allAppsUsage = mappedApps.sortedByDescending { it.totalUsage }
            updateViewState()
        }
    }

    fun toggleSystemApps(show: Boolean) {
        showSystemApps = show
        updateViewState()
    }

    private fun updateViewState() {
        val filtered = if (showSystemApps) allAppsUsage else allAppsUsage.filter { !it.isSystemApp }
        val maxUsage = filtered.maxOfOrNull { it.totalUsage } ?: 1L
        _uiState.value = NetworkUiState.Success(filtered, showSystemApps, maxUsage)
    }

    fun toggleVpn(enabled: Boolean) {
        _isVpnActive.value = enabled
        val intent = Intent(context, LocalVpnService::class.java).apply {
            action = if (enabled) LocalVpnService.ACTION_START_VPN else LocalVpnService.ACTION_STOP_VPN
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun prepareVpnIntent(): Intent? {
        return android.net.VpnService.prepare(context)
    }
}

sealed class NetworkUiState {
    object Loading : NetworkUiState()
    data class Success(
        val apps: List<NetworkAppModel>,
        val showSystemApps: Boolean,
        val maxUsage: Long
    ) : NetworkUiState()
    data class Error(val message: String) : NetworkUiState()
}
