package com.example.securityapp.ui.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed class DashboardUiState {
    object Idle : DashboardUiState()
    object Loading : DashboardUiState()
    data class Success(val score: Int, val status: String, val highRiskCount: Int, val dataTodayGb: Float) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Idle)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        _uiState.value = DashboardUiState.Loading
        // Mock loading data
        _uiState.value = DashboardUiState.Success(
            score = 85,
            status = "Needs Attention",
            highRiskCount = 2,
            dataTodayGb = 1.2f
        )
    }

    fun onQuickScanClicked() {
        Log.d("DashboardViewModel", "QuickScanClicked")
        // The UI will observe this or handle navigation via callback, but we log here as required.
    }

    fun onViewPermissionsClicked() {
        Log.d("DashboardViewModel", "ViewPermissionsClicked")
    }

    fun onViewDataUsageClicked() {
        Log.d("DashboardViewModel", "ViewDataUsageClicked")
    }
}
