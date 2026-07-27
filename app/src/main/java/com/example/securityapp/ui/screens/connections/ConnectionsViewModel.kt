package com.example.securityapp.ui.screens.connections

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ConnectionsUiState {
    object Loading : ConnectionsUiState()
    data class Success(
        val isVpnActive: Boolean,
        val logs: List<ConnectionLog>
    ) : ConnectionsUiState()
    data class Error(val message: String) : ConnectionsUiState()
}

data class ConnectionLog(
    val id: Int,
    val appName: String,
    val domain: String,
    val isSuspicious: Boolean,
    val timestamp: String
)

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dnsLogRepository: com.example.securityapp.data.repository.DnsLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConnectionsUiState>(ConnectionsUiState.Loading)
    val uiState: StateFlow<ConnectionsUiState> = _uiState.asStateFlow()

    private var currentVpnState: Boolean = false
    private var currentLogs: List<ConnectionLog> = emptyList()

    init {
        viewModelScope.launch {
            dnsLogRepository.logs.collect { logs ->
                updateState(logs)
            }
        }
    }

    private fun updateState(logs: List<ConnectionLog> = dnsLogRepository.logs.value) {
        _uiState.value = ConnectionsUiState.Success(
            isVpnActive = currentVpnState,
            logs = logs
        )
    }

    fun prepareVpnIntent(): Intent? {
        Log.d("ConnectionsViewModel", "VpnToggleOnRequested")
        return VpnService.prepare(context)
    }

    fun startVpn() {
        Log.d("ConnectionsViewModel", "VpnStarted")
        val intent = Intent(context, com.example.securityapp.data.vpn.LocalVpnService::class.java).apply {
            action = com.example.securityapp.data.vpn.LocalVpnService.ACTION_START_VPN
        }
        context.startService(intent)
        currentVpnState = true
        updateState()
    }

    fun stopVpn() {
        Log.d("ConnectionsViewModel", "VpnStopped")
        val intent = Intent(context, com.example.securityapp.data.vpn.LocalVpnService::class.java).apply {
            action = com.example.securityapp.data.vpn.LocalVpnService.ACTION_STOP_VPN
        }
        context.startService(intent)
        currentVpnState = false
        updateState()
    }
}
