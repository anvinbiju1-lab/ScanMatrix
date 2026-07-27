package com.example.securityapp.data.repository

import com.example.securityapp.ui.screens.connections.ConnectionLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Singleton
class DnsLogRepository @Inject constructor() {
    private val _logs = MutableStateFlow<List<ConnectionLog>>(emptyList())
    val logs: StateFlow<List<ConnectionLog>> = _logs.asStateFlow()

    private var nextId = 1
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun addLog(domain: String, isSuspicious: Boolean) {
        val newLog = ConnectionLog(
            id = nextId++,
            appName = "Unknown App", // Identifying app requires complex UID lookup via /proc/net, simplified for demo
            domain = domain,
            isSuspicious = isSuspicious,
            timestamp = timeFormat.format(Date())
        )
        // Keep last 100 logs
        val updatedList = listOf(newLog) + _logs.value.take(99)
        _logs.value = updatedList
    }
    
    fun clearLogs() {
        _logs.value = emptyList()
    }
}
