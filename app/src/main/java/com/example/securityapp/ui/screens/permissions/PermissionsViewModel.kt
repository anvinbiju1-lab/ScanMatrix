package com.example.securityapp.ui.screens.permissions

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securityapp.data.repository.AppDomainModel
import com.example.securityapp.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("security_app_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<PermissionsUiState>(PermissionsUiState.Loading)
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    private val _trustedApps = MutableStateFlow<Set<String>>(emptySet())
    val trustedApps: StateFlow<Set<String>> = _trustedApps.asStateFlow()

    private var allApps: List<AppDomainModel> = emptyList()

    init {
        loadTrustedApps()
        loadApps()
    }

    private fun loadTrustedApps() {
        val trusted = prefs.getStringSet("trusted_apps", emptySet()) ?: emptySet()
        _trustedApps.value = trusted
    }

    fun toggleTrustApp(packageName: String) {
        val current = _trustedApps.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _trustedApps.value = current
        prefs.edit().putStringSet("trusted_apps", current).apply()
        
        // Re-filter to trigger UI update
        val currentState = _uiState.value
        if (currentState is PermissionsUiState.Success) {
            filterApps(currentState.currentFilter)
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.value = PermissionsUiState.Loading
            allApps = appRepository.getInstalledApps().filter { !it.isSystemApp }
            filterApps(PermissionFilter.ALL_SENSITIVE)
        }
    }

    fun filterApps(filter: PermissionFilter) {
        android.util.Log.d("PermissionsViewModel", "PermissionFilterSelected: $filter")
        val filtered = when (filter) {
            PermissionFilter.ALL_SENSITIVE -> allApps.filter { it.grantedPermissions.any { p -> isSensitive(p) } }
            PermissionFilter.CAMERA -> allApps.filter { it.grantedPermissions.contains("android.permission.CAMERA") }
            PermissionFilter.MICROPHONE -> allApps.filter { it.grantedPermissions.contains("android.permission.RECORD_AUDIO") }
            PermissionFilter.LOCATION -> allApps.filter {
                it.grantedPermissions.contains("android.permission.ACCESS_FINE_LOCATION") ||
                it.grantedPermissions.contains("android.permission.ACCESS_COARSE_LOCATION")
            }
        }.sortedByDescending { it.grantedPermissions.size }
        _uiState.value = PermissionsUiState.Success(filtered, filter)
    }

    private fun isSensitive(permission: String): Boolean {
        val sensitive = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE"
        )
        return sensitive.contains(permission)
    }
}

sealed class PermissionsUiState {
    object Loading : PermissionsUiState()
    data class Success(val apps: List<AppDomainModel>, val currentFilter: PermissionFilter) : PermissionsUiState()
    data class Error(val message: String) : PermissionsUiState()
}

enum class PermissionFilter {
    ALL_SENSITIVE, CAMERA, MICROPHONE, LOCATION
}
