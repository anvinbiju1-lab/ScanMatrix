package com.example.securityapp.ui.screens.scan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securityapp.data.db.ScanHistoryDao
import com.example.securityapp.data.db.ScanHistoryEntity
import com.example.securityapp.data.repository.FullScanSummary
import com.example.securityapp.data.repository.RiskLevel
import com.example.securityapp.data.repository.ScanResult
import com.example.securityapp.data.repository.ThreatScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ScanUiState {
    object Idle : ScanUiState()
    data class Scanning(
        val phase: String, 
        val progress: Float,
        val currentFolder: String = "",
        val fileCount: Int = 0
    ) : ScanUiState()
    data class Finished(
        val results: List<ScanResult>,
        val summary: FullScanSummary?,
        val isFullScan: Boolean
    ) : ScanUiState()
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val threatScanner: ThreatScanner,
    private val scanHistoryDao: ScanHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _autoSanitizeEnabled = MutableStateFlow(true)
    val autoSanitizeEnabled: StateFlow<Boolean> = _autoSanitizeEnabled.asStateFlow()

    fun toggleAutoSanitize(enabled: Boolean) {
        _autoSanitizeEnabled.value = enabled
    }

    fun startQuickScan() {
        Log.d("ScanViewModel", "QuickScanClicked")
        performScan(isDeepScan = false)
    }

    fun startFullScan() {
        Log.d("ScanViewModel", "FullScanStarted")
        performScan(isDeepScan = true)
    }

    private fun performScan(isDeepScan: Boolean) {
        viewModelScope.launch {
            _uiState.value = ScanUiState.Scanning("Starting", 0f)
            
            val (results, summary) = threatScanner.performScan(
                isDeepScan = isDeepScan,
                onPhaseChange = { phase ->
                    val curr = _uiState.value as? ScanUiState.Scanning
                    _uiState.value = ScanUiState.Scanning(phase, curr?.progress ?: 0f, curr?.currentFolder ?: "", curr?.fileCount ?: 0)
                },
                onProgress = { progress ->
                    val curr = _uiState.value as? ScanUiState.Scanning
                    _uiState.value = ScanUiState.Scanning(curr?.phase ?: "Scanning", progress, curr?.currentFolder ?: "", curr?.fileCount ?: 0)
                },
                onCurrentFolder = { folder ->
                    val curr = _uiState.value as? ScanUiState.Scanning
                    _uiState.value = ScanUiState.Scanning(curr?.phase ?: "Scanning", curr?.progress ?: 0f, folder, curr?.fileCount ?: 0)
                },
                onFileCount = { count ->
                    val curr = _uiState.value as? ScanUiState.Scanning
                    _uiState.value = ScanUiState.Scanning(curr?.phase ?: "Scanning", curr?.progress ?: 0f, curr?.currentFolder ?: "", count)
                }
            )

            // Save to Room DB
            val entity = ScanHistoryEntity(
                scanType = if (isDeepScan) "FULL" else "QUICK",
                totalAppsScanned = results.count { it.app != null },
                totalFilesScanned = summary?.totalScanned ?: results.count { it.app == null },
                highRiskFound = results.count { it.riskLevel == RiskLevel.HIGH },
                mediumRiskFound = results.count { it.riskLevel == RiskLevel.MEDIUM },
                lowRiskFound = results.count { it.riskLevel == RiskLevel.LOW }
            )
            scanHistoryDao.insertScanHistory(entity)

            _uiState.value = ScanUiState.Finished(results, summary, isDeepScan)
        }
    }
}
